; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.

(ns noahtheduke.scram.extensions.lazytest
  (:require
   [lazytest.core :as lt :refer [defdescribe describe it ->ex-failed]]
   [noahtheduke.scram :as scram]))

(defmacro compare-output
  [form output]
  (assert (string? output) "Must be given a string literal")
  `(let [ctx# (or scram/*ctx* {:state (atom {:diffs []})})
         result# (binding [scram/*ctx* ctx#]
                   ~(with-meta `(scram/compare-output ~form ~output) (meta &form)))
         diff# (first (:diffs @(:state ctx#)))]
     (or result#
         (throw (->ex-failed ~&form
                             {:message (:diff diff#)
                              :expected '~output
                              :actual (:actual diff#)})))))

(defdescribe lt-test
  (describe "example"
    (it "should work???"
      (compare-output
       (print \newline \newline \newline \newline (+ 1 1))
       "1\n2"))))
