; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.

(ns noahtheduke.scram-test 
  (:require
   [clojure.test :refer [is]]
   [noahtheduke.scram :refer [compare-output]]
   [noahtheduke.scram.clojure-test :refer [defcram]]))

(def example
  "hello
")

(defcram hello
  (is (compare-output
       (print example)
       "hello\n")))
