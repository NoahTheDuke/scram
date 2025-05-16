; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.

(ns noahtheduke.scram.extensions.lazytest
  (:require
   [lazytest.core :refer [->ex-failed]]
   [noahtheduke.scram :as scram]))

(defmacro defcram
  [cname & body]
  `(do (scram/defcram ~cname ~@body)
       (alter-meta! #'~cname assoc :lazytest/test (fn [] (~cname nil)))
       #'~cname))

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
