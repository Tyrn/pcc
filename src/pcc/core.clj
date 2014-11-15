(ns pcc.core
  "Player album loader"
  (:require [me.raynes.fs :as fs])
  (:require [clojure.string :as string])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class)
  )

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
    "root substring for destination directory and file names"
    :default "default-name"]
   ["-g" "--album-tag ALBUM_TAG"
    "album tag name"
    :default "default-tag"]
   ["-b" "--album-num ALBUM_NUM"
    "album (book) start number; 0...99"
    :default 0
    :parse-fn #(Integer/parseInt %)]
   ]
  )

(defn -main
  "Parsing the Command Line and Giving Orders"
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help
    (cond
     (:help options) (println (usage summary))
     )
    )
  )

;(-main
; "-h"
; )
