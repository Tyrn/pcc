(ns pcc.core-test
  (:use [clojure.repl])
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [me.raynes.fs :as fs]
            [pcc.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(-main
 "-h"
 )

(-main
 "-r"
 "-u AlfaName"
 "-g AlfaTag"
 "-b" "42"
 "/home/alexey/dir-src"
 "/home/alexey/dir-dst"
 )
*parsed-args*
