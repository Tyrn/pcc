(ns pcc.core
  "Player album loader"
  (:require [me.raynes.fs :as fs])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class)
  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (printf "Hello, World: %s %s\n" (first args) (nth args 1))
  )

(-main
 "-a"
 "-b"
 "c"
 )
