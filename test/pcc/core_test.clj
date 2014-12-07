(ns pcc.core-test
  (:use [clojure.repl])
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [green-tags.core :as core]
            [me.raynes.fs :as fs]
            [pcc.core :refer :all]
            [midje.sweet :refer :all]))

(println "You should expect to see one failure below.")

(fact
 "Returns a zero padded string representation of integer"
 (zero-pad 1 4) => "0001"
 (zero-pad 15111 4) => "15111"
 (zero-pad 2 5) => "00002")

(fact
 "Returns a path stripped of extension, if any"
 (strip-file-ext "/alfa/bravo/charlie.dat") => "/alfa/bravo/charlie"
 (strip-file-ext "/alfa/bravo/charlie") => "/alfa/bravo/charlie"
 (strip-file-ext "/alfa/bravo/charlie/") => "/alfa/bravo/charlie"
 (strip-file-ext "/alfa/bra.vo/charlie.dat") => "/alfa/bra.vo/charlie")
