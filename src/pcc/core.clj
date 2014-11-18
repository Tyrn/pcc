(ns pcc.core
  "Player album loader"
  (:require [me.raynes.fs :as fs])
  (:import java.io.File)
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as string])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(declare ^:dynamic *parsed-args*)
(def ^:dynamic *sys-sep* java.io.File/separator)
(def ^:dynamic *nix-sep* "/")

(defn usage [options-summary]
  (->> ["usage: pcc [-h] [-t] [-p] [-u UNIFIED_NAME] [-r] [-g ALBUM_TAG]"
        "    [-b ALBUM_NUM]"
        "    src_dir dst_dir"
        ""
        "pcc \"Procrustes\" SmArT is a CLI utility for copying subtrees containing audio (mp3)"
        "files in sequence (preorder of the source subtree, alphabetically sorted by default)."
        "The end result is a \"flattened\" copy of the source subtree. \"Flattened\" means"
        "that only a namesake of the root source directory is created, where all the files get"
        "copied to, names prefixed with a serial number. Mp3 tags 'Title' and 'Track Number'"
        "get removed. Tag 'Album' can be replaced (or removed)."
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
       (string/join \newline)))

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
    :validate [#(<= 0 % 99) "must be a number, 0...99"]]])

(defn str-strip-numbers
  "Returns a vector of integer numbers
  embedded in a string argument"
  [s]
  (let [matcher (re-matcher #"\d+" s)]
    (loop [match (re-find matcher) result []]
      (if-not match
        result
        (recur (re-find matcher) (conj result (Integer/parseInt match)))))))

(defn cmpv-int
  "Compares vectors of integers using 'string semantics'"
  [vx vy]
  (or (first (drop-while zero? (map compare vx vy))) (- (count vx) (count vy))))

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
       (compare str-x str-y))))

(defn compare-fobj-path
  "Extracts and compares two paths from file obects"
  [fobj-x fobj-y]
  (let [xp (.getPath fobj-x) yp (.getPath fobj-y)] ;; paths extracted
    (cmpstr-naturally xp yp)))

(defn compare-root
  "Compares two paths extracted from file objects
  stored as first elements of argument vectors"
  [root-x root-y]
  (compare-fobj-path (root-x 0) (root-y 0)))

(defn counter
  "Provides a function returning next
  consecutive integer, starting from seed"
  [seed]
  (let [x (atom (dec seed))]
    #(do (reset! x (inc @x)) @x)))

(defn drop-common-root
  "Deprecated"
  [path-x path-y]
  (let [sx (fs/split path-x)
        sy (fs/split path-y)
        trail (drop-while integer? (map #(if (zero? (compare % %2)) 0 %) sx sy))]
    trail))

(defn list-dir-groomed
  "Returns a vector of: (0) naturally sorted list of
  directory objects (1) naturally sorted list
  of file objects. Function takes an unsotred list
  of objects"
  [dir-obj-list]
  (let [dirs (sort compare-fobj-path (filter #(not (fs/file? %)) dir-obj-list))
        files (sort compare-fobj-path (filter fs/file? dir-obj-list))]
    (vector dirs files)))

(defn drop-trail
  "Drop the given character from the
  argument string, if any"
  [s, trailer]
  (if (= (nth s (dec (count s))) (first trailer)) (apply str (take (dec (count s)) s)) s))

(defn traverse-dir
  "Traverses the (source) directory, preorder"
  [src-dir dst-step]
  (let [{:keys [options arguments]} *parsed-args*
        dst-root (arguments 1)
        [dirs files] (list-dir-groomed (fs/list-dir src-dir))

        dir-handler  (fn [dir-obj]
                       "Processes the current directory, source side;
                       creates properly named destination directory, if necessary"
                       (let [dir (.getPath dir-obj)
                             step (str dst-step *nix-sep* (fs/base-name dir-obj))]
                         (fs/mkdir (str dst-root step))
                         (traverse-dir dir step)))

        file-handler (fn [file-obj]
                       "Copies the current file, properly named and tagged"
                       (let [dst-path (str dst-root dst-step *nix-sep* (.getName file-obj))]
                         (fs/copy file-obj (fs/file dst-path))
                         dst-path))]

    (concat (map dir-handler dirs) (map file-handler files))))

(defn build-album
  "Copy source files to destination according
  to command line options"
  []
  (let [{:keys [options arguments]} *parsed-args*
        output (traverse-dir (arguments 0) "")]
    output))

(defn -main
  "Parsing the Command Line and Giving Orders"
  [& args]
  (def ^:dynamic *parsed-args* (parse-opts args cli-options))
  (let [{:keys [options arguments errors summary]} *parsed-args*]
    (cond
     (not (nil? errors)) (println errors)
     (not= (count arguments) 2) (println (usage summary))
     (:help options) (println (usage summary))
     :else (build-album))))
