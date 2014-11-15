(ns pcc.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
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
(string/blank? "")
(compare "4" "1")
(cmpstr-naturally "alfa" "cravo")
(compare 11 23)
(def res ([#<File /home/alexey/dir-src> #{"3" "4" "1" "2" "10"} #{"Фото-0015.jpg"}]
          [#<File /home/alexey/dir-src/1> #{} #{"vB8vqyc4XBk.jpg" "valet.jpg"}]
          [#<File /home/alexey/dir-src/10> #{} #{"jca3.jpg" "jca10.jpg" "jca1.jpg" "jca4.jpg" "jca2.jpg"}]
          [#<File /home/alexey/dir-src/2> #{"002"} #{"warrior-babe-305079.jpg" "wallp_fant_0017.jpg"}]
          [#<File /home/alexey/dir-src/2/002> #{} #{"tumblr_mt7rckyTbi1qd5ic3o1_500.jpg"}]
          [#<File /home/alexey/dir-src/3> #{} #{"Сияние-cosplay-931717.jpeg"}]
          [#<File /home/alexey/dir-src/4> #{} #{}]))
