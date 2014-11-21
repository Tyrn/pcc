(ns pcc.core-test
  (:use [clojure.repl])
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [green-tags.core :as core]
            [me.raynes.fs :as fs]
            [pcc.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(-main
 "-h"
 )

(-main
;  "-t"
 "-r"
 "-p"
; "-u" "AlfaName"
 "-g" "AlfaTag"
; "-b" "42"
 "/home/alexey/dir-src1/"
 "/home/alexey/dir-dst/"
 )
*parsed-args*
(delete-offspring "/home/alexey/dir-dst/")
(fs/file ".")
(core/get-all-info "/home/alexey/dir-src1/12 Byzantine Rulers_ Reading Suggestions.mp3")
(core/get-fields "/home/alexey/dir-src1/12 Byzantine Rulers_ Reading Suggestions.mp3")
(core/get-fields "/home/alexey/common/Downloads/UpDown/Books/Audio/48 Laws Of Power - Robert Greene/48 Laws Of Power CD 6.mp3")
core/mp3-fields
