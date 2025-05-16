; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.

(ns noahtheduke.scram.clojure-test 
  (:require
   [clojure.test :refer [assert-expr do-report]]
   [noahtheduke.scram :refer [*ctx* compare-output]]))

(defmethod assert-expr 'compare-output
  [msg assert-form]
  (let [[_compare-output form output & others] assert-form]
    (assert (empty? others) "Don't give too many to compare-output")
    `(let [ctx# (or *ctx* {:state (atom {:diffs []})})
           result# (binding [*ctx* ctx#]
                     ~(with-meta `(compare-output ~form ~output) (meta assert-form)))
           diff# (first (:diffs @(:state ctx#)))
           msg# ~msg]
       (if result#
         (do-report {:type :pass
                     :message msg#
                     :expected '~output
                     :actual '~form})
         (do-report {:type :fail
                     :message (str msg# (:diff diff#))
                     :expected '~output
                     :actual (:actual diff#)}))
       result#)))

