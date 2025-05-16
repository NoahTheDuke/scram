; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.

(ns noahtheduke.scram-test
  (:require
   [noahtheduke.scram :refer [compare-output defcram]]))

(def example
  "hello
")

(defcram hello
  (compare-output
       (print example)
       "hello\n"))

(defcram nil-test
  (compare-output
   (+ 1 1)
   ""))
