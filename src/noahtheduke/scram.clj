; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.

(ns noahtheduke.scram
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.zip :as z]
   [clojure.tools.cli :as cli]) 
  (:import
   [com.github.difflib DiffUtils UnifiedDiffUtils]
   [java.io FileNotFoundException]))

(set! *warn-on-reflection* true)

(def ^:dynamic *ctx* nil)

(defn root-zipper
  {:no-doc true}
  [zloc]
  (loop [zloc zloc]
    (if-let [u (z/up zloc)]
      (recur u)
      zloc)))

(defn find-by-pos
  {:no-doc true}
  [zloc [frow fcol]]
  (z/find zloc z/next (fn [zloc]
                        (let [{:keys [row col]} (meta (z/node zloc))]
                          (and (= frow row)
                               (= fcol col))))))

(defn replace-node-at-position
  {:no-doc true}
  [zloc pos new-node]
  (when zloc
    (-> zloc
        (find-by-pos pos)
        (z/down)
        (z/right)
        (z/right)
        (z/replace new-node)
        (root-zipper))))

(defn get-diff
  [path original updated]
  (let [o-lines (str/split-lines original)
        u-lines (str/split-lines updated)
        patch (DiffUtils/diff o-lines u-lines)
        diff (UnifiedDiffUtils/generateUnifiedDiff path path o-lines patch 2)]
    (str/join \newline diff)))

(defn update-state
  "Only useful in `compare-output`."
  {:no-doc true}
  [ctx pos cur-str new-str]
  (let [state (:state ctx)
        diff (get-diff (:path ctx) cur-str new-str)]
    (swap! state
           (fn [state#]
             (-> state#
                 (update :diffs conj {:pos pos
                                      :expected cur-str
                                      :actual new-str
                                      :diff diff
                                      :path (:path ctx)
                                      :ns (:ns ctx)})
                 (update :zloc replace-node-at-position pos (node/string-node new-str)))))))

(defmacro compare-output
  "Must be used in a situation where *ctx* is bound to a context map."
  [form output]
  (assert (string? output) "Must be given a string literal")
  (let [m (meta &form)]
    `(let [pos# ~[(:line m) (:column m)]
           cur-str# ~output
           new-str# (with-out-str (do ~form))
           ret# (= cur-str# new-str#)]
       (when-not ret#
         (update-state *ctx* pos# cur-str# new-str#))
       ret#)))

(defn get-path-from-var [var']
  (let [f (:file (meta var'))
        path (or (some-> f
                         (#(.. (Thread/currentThread)
                               (getContextClassLoader)
                               (getResource %)))
                         (io/file)
                         (str))
                 f)]
    {:path path
     :file (when (and path (.exists (io/file path)))
             (slurp path))}))

(defmacro defcram
  [cname & body]
  (assert (simple-symbol? cname) "defcram must be given a symbol")
  (with-meta
    `(defn ~cname
       {::cram true
        :test (fn [] (~cname nil))}
       ([]
        (let [cv# (var ~cname)
              {file# :file path# :path} (get-path-from-var cv#)]
          (when file#
            (let [ctx# {:path path#
                        :ns (the-ns (symbol (namespace (symbol cv#))))
                        :state (atom {:diffs []
                                      :zloc (z/of-string* file#)})}
                  _# (~cname ctx#)
                  updated-file# (z/root-string (:zloc @(:state ctx#)))]
              (when (not= file# updated-file#)
                (println (get-diff path# file# updated-file#)))))))
       ([ctx#]
        (binding [*ctx* ctx#]
          ~@body)))
    (meta &form)))

(defn find-crams-in-ns [ns*]
  (->> (vals (ns-interns ns*))
       (filter #(::cram (meta %)))))

(defn run-on-ns
  [ctx]
  (let [crams (find-crams-in-ns (:ns ctx))
        {:keys [file path]} (get-path-from-var (first crams))]
    (if-not file
      ctx
      (let [ctx (assoc ctx :path path)
            state (:state ctx)]
        (swap! state assoc :zloc (z/of-string* file))
        (doseq [cram-fn crams]
          (cram-fn ctx))
        (let [updated-file (z/root-string (:zloc @state))
              diff (get-diff path file updated-file)
              updated? (not= file updated-file)]
          (when (and updated? (-> ctx :options :print-diffs))
            (println diff))
          (assoc ctx :updated? updated?))))))

(defn update-file
  [ctx]
  (when (and (-> ctx :options :write)
             (:path ctx)
             (:state ctx)
             (:updated? ctx))
    (spit (:path ctx) (z/root-string (:zloc @(:state ctx)))))
  ctx)

(defn run-impl
  [ctx]
  (doseq [ns (:nses ctx)]
    (try (require (symbol ns))
         (-> ctx
             (assoc :ns (the-ns ns))
             (dissoc :path :updated?)
             (run-on-ns)
             (update-file))
         (catch FileNotFoundException _
           (println "Namespace" ns "does not exist"))))
  ctx)

(defn init-context
  [options]
  {:nses (:nses options)
   :state (atom {:diffs []
                 :zloc nil})
   :options (merge {:print-diffs true :write false}
                   (dissoc options :nses))})

(defn run
  #_{:clj-kondo/ignore [:unused-binding]}
  [{:keys [nses print-diffs write] :as options}]
  (let [ctx (run-impl (init-context options))]
    {:diffs (sort-by :pos (:diffs @(:state ctx)))}))

(def cli-options
  [[nil "--print-diffs" "Print differences as git-style diffs."]
   ["-w" "--write" "Write each updated file."]
   ["-h" "--help" "Print help docs"]])

(defn format-help-message
  [specs]
  (let [lines ["Scram version 0.0"
               ""
               "Usage:"
               "  clojure -M:scram [options] [namespace...]"
               "  clojure -M:scram [options] -- [namespace...]"
               ""
               "Options:"
               (cli/summarize specs)
               ""]]
    {:exit-message (str/join \newline lines)
     :ok true}))

(defn format-cli-errors
  [errors]
  {:exit-message (str/join \newline (cons "scram errors:" errors))
   :errors errors
   :ok false})

(defn -main [& args]
  (let [{:keys [arguments options errors summary]}
        (cli/parse-opts args cli-options :strict true :summary-fn identity)
        arguments (map symbol arguments)]
    (cond
      (:help options) (println (:exit-message (format-help-message summary)))
      (seq errors) (println (:exit-message (format-cli-errors errors)))
      (not (seq arguments)) (println "scram errors:\nNeed at least 1 namespace argument")
      :else (run (merge {:nses arguments} options))))
  nil)

(comment
  (-main)
  (run {:nses [(symbol (namespace ::a))]
        :print-diffs false}))
