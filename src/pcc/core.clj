(ns pcc.core
  "Player album loader"
  (:require [me.raynes.fs :as fs])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as string])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class)
  )

(declare ^:dynamic *parsed-args*)

(defn usage [options-summary]
  (->> ["usage: pcc [-h] [-t] [-p] [-u UNIFIED_NAME] [-r] [-g ALBUM_TAG]"
        "    [-b ALBUM_NUM]"
        "    src_dir dst_dir"
        ""
        "pcc \"Procrustes\" MoBiL is a CLI utility for copying subtrees containing audio (mp3) files in sequence"
        "(preorder of the source subtree, alphabetically sorted by default)."
        "The end result is a \"flattened\" copy of the source subtree. \"Flattened\" means"
        "that only a namesake of the root source directory is created, where all the files get copied,"
        "names prefixed with a serial number. Mp3 tags 'Title' and 'Track Number' get removed."
        "Tag 'Album' can be replaced (or removed)."
        "The writing process is strictly sequential: either starting with the number one file,"
        "or in the reversed order. This can be important for some mobile devices."
        ""
        "positional arguments:"
        "  src_dir    source directory to be copied itself as root directory"
        "  dst_dir    destination directory"
        ""
        "optional arguments:"
        options-summary
        ""]
       (string/join \newline)
   )
  )

(def cli-options
  [["-h" "--help" "brief usage info"]
   ["-t" "--tree-dst" "copy as tree: keep source tree structure at destination"]
   ["-p" "--drop-dst" "do not create destination directory"]
   ["-r" "--reverse" "write files in reverse order (time sequence)"]
   ["-u" "--unified-name UNIFIED_NAME"
    "naming suggestion for destination directory and files"
    :default "default-name"]
   ["-g" "--album-tag ALBUM_TAG"
    "album tag name"
    :default "default-tag"]
   ["-b" "--album-num ALBUM_NUM"
    "album (book) start number; 0...99"
    :default 0
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 0 % 99) "must be a number, 0...99"]]
   ]
  )

(defn str-strip-numbers
  "Returns a vector of integer numbers,
  embedded in a string"
  [s]
  (let [matcher (re-matcher #"\d+" s)]
    (loop [match (re-find matcher) result []]
      (if-not match
        result
        (recur (re-find matcher) (conj result (Integer/parseInt match)))
        )
      )
    )
  )

(defn cmpv-int
  "Compares vectors of integers using 'string semantics'"
  [vx vy]
  (let [res (first (drop-while zero? (map compare vx vy)))
        diffenence (- (count vx) (count vy))]
     (if res res diffenence)
    )
  )

(defn cmpstr-naturally
  "If both strings contain digits, returns
  numerical comparison based on the numeric
  values embedded in the strings,
  otherwise returns standard string comparison.
  The idea of natural sort as opposed to standard
  lexicographic sort is one of coping
  with the possible absence of the leading zeros
  in \"numbers\" of files or directories"
  [str-x str-y]
  (let [num-x (str-strip-numbers str-x)  ;; building vectors of integers,
        num-y (str-strip-numbers str-y)] ;; possibly empty
     (if (and (not-empty num-x) (not-empty num-y))
       (cmpv-int num-x num-y)
       (compare str-x str-y)
      )
    )
  )

(defn compare-root
  "Compares two paths extracted from file objects
  stored as first elements of argument vectors"
  [root-x root-y]
  (let [x0 (root-x 0) y0 (root-y 0) ;; Java file objects extracted
        xp (.getPath x0) yp (.getPath y0)] ;; paths extracted
    (cmpstr-naturally xp yp)
   )
  )

(defn build-album
  "Copy source files to destination according
  to command line options"
  []
  (require 'pcc.core)
  (let [{:keys [options arguments]} *parsed-args*
        roots (fs/iterate-dir (arguments 0))
        rsorted (sort compare-root roots)
        ]
     rsorted
    )
  )

(defn -main
  "Parsing the Command Line and Giving Orders"
  [& args]
  ;(use 'pcc.core)
  (def ^:dynamic *parsed-args* (parse-opts args cli-options))
  (let [{:keys [options arguments errors summary]} *parsed-args*]
    (cond
     (not (nil? errors)) (println errors)
     (not= (count arguments) 2) (println (usage summary))
     (:help options) (println (usage summary))
     :else (build-album)
     )
    )
  )
