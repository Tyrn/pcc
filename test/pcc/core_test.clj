(ns pcc.core-test
  (:use [clojure.repl])
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [green-tags.core :as core]
            [me.raynes.fs :as fs]
            [pcc.core :refer :all]
            [midje.sweet :refer :all]))

;(println "You should expect to see one failure below.")

(fact
 "Returns a zero padded string representation of integer"
 (zero-pad 1 4) => "0001"
 (zero-pad 15111 4) => "15111"
 (zero-pad 2 5) => "00002")

(fact
 "Returns a path stripped of extension, if any"
 (strip-file-ext "/alfa/bravo/charlie.dat") => "/alfa/bravo/charlie"
 (strip-file-ext "/alfa/bravo/charlie") => "/alfa/bravo/charlie"
 (strip-file-ext "/alfa/bravo/charlie/") => "/alfa/bravo/charlie/"
 (strip-file-ext "/alfa/bra.vo/charlie.dat") => "/alfa/bra.vo/charlie")

(fact
 "Returns a vector of integer numbers
  embedded in a string argument"
 (str-strip-numbers "ab11cdd2k.144") => [11, 2, 144]
 (str-strip-numbers "Ignacio Vazquez-Abrams") => [])

(fact
 "Compares vectors of integers using 'string semantics'"
 (cmpv-int [] []) => 0
 (cmpv-int [1] []) => 1
 (cmpv-int [3] []) => 1
 (cmpv-int [1, 2, 3] [1, 2, 3, 4, 5]) => -2
 (cmpv-int [1, 4] [1, 4, 16]) => -1
 (cmpv-int [2, 8] [2, 2, 3]) => 1
 (cmpv-int [0, 0, 2, 4] [0, 0, 15]) => -1
 (cmpv-int [0, 13] [0, 2, 2]) => 1
 (cmpv-int [11, 2] [11, 2]) => 0)

(fact
 "Compares strings naturally"
 (cmpstr-naturally "" "") => 0
 (cmpstr-naturally "2a" "10a") => -1
 (cmpstr-naturally "alfa" "bravo") => -1)

(fact
 "Reduces a string of names to initials."
 (make-initials "John ronald reuel Tolkien" ".") => "J.R.R.T"
 (make-initials "e. B. Sledge" ".") => "E.B.S")
