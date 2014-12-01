(ns pcc.core
  "Player album loader"
  (:import java.io.File)
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [green-tags.core :as core]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(declare ^:dynamic *parsed-args*)
(def ^:dynamic *sys-sep* java.io.File/separator)
(def ^:dynamic *nix-sep* "/")

(defn usage [options-summary]
  (->> ["usage: pcc [-h] [-t] [-p] [-r] [-u UNIFIED_NAME] [-g ALBUM_TAG]"
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
    :default nil]
   ["-g" "--album-tag ALBUM_TAG"
    "album tag name"
    :default nil]
   ["-b" "--album-num ALBUM_NUM"
    "album (book) start number; 0...99"
    :default nil
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 0 % 99) "must be a number, 0...99"]]])

(defn delete-recursively [fname]
  (letfn [(func [f]
                (when (.isDirectory f)
                  (doseq [f2 (.listFiles f)]
                    (func f2)))
                (clojure.java.io/delete-file f))]
    (func (clojure.java.io/file fname))))

(defn delete-offspring
  "Deletes offspring of the directory
  NB Not quite charming, but first
  I have to grok delete-recursively (the scavenged one)"
  [dir-name]
  (let [offspring (fs/list-dir dir-name)]
    (map #(delete-recursively (.getPath %)) offspring)))

(defn zero-pad
  "Returns i zero-padded to n"
  [i n]
  (format (str "%0" n "d") i))

(defn counter
  "Provides a function returning next
  consecutive integer, starting from seed"
  [seed]
  (let [x (atom (dec seed))]
    #(swap! x inc)))

(defn strip-file-ext
  "Discard file extension"
  [s]
  (let [[_ ext] (fs/split-ext (fs/file s))]
    (apply str (drop-last (count ext) s))))

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
  "If both strings contain digits, returns numerical comparison based on the numeric
  values embedded in the strings, otherwise returns the standard string comparison.
  The idea of the natural sort as opposed to the standard lexicographic sort is one of coping
  with the possible absence of the leading zeros in 'numbers' of files or directories"
  [str-x str-y]
  (let [num-x (str-strip-numbers str-x)  ;; building vectors of integers,
        num-y (str-strip-numbers str-y)] ;; possibly empty
     (if (and (not-empty num-x) (not-empty num-y))
       (cmpv-int num-x num-y)
       (compare str-x str-y))))

(defn compare-fobj-path
  "Extracts and compares two paths from file obects"
  [fobj-x fobj-y]
  (let [xp (.getPath fobj-x) yp (.getPath fobj-y)]          ;; paths extracted
    (cmpstr-naturally (strip-file-ext xp) (strip-file-ext yp))))

(defn compare-fobj-file
  "Extracts and compares two paths from file obects, filenames only"
  [fobj-x fobj-y]
  (let [xn (fs/base-name fobj-x) yn (fs/base-name fobj-y)] ;; names extracted
    (cmpstr-naturally (strip-file-ext xn) (strip-file-ext yn))))

(defn list-dir-groomed
  "Returns a vector of: (0) naturally sorted list of
  directory objects (1) naturally sorted list
  of file objects. Function takes an unsotred list
  of objects"
  [dir-obj-list]
  ;; Different approach to sorting dirs and files is a necessity.
  ;; Otherwise it won't work, thanks to the idea of the natural sort.
  (let [dirs (sort compare-fobj-path (filter #(not (fs/file? %)) dir-obj-list))

        audio-file?   (fn [file-obj]
                        "Audio file to be copied
                        NB Not quite correct detection by extension"
                        (and (fs/file? file-obj) (= (string/upper-case (fs/extension file-obj)) ".MP3")))

        files (sort compare-fobj-file (filter audio-file? dir-obj-list))]

    (vector dirs files)))

(defn- traverse-dir
  "Traverses the (source) directory, preorder"
  [src-dir dst-root dst-step ffc!]
  (let [{:keys [options]} *parsed-args*
        uname (:unified-name options)
        [dirs files] (list-dir-groomed (fs/list-dir src-dir))

        dir-name-decorator   (fn [i name]
                               (let []
                                 (str (zero-pad (inc i) 3) "-" name)))

        file-name-decorator  (fn [i name]
                               (let []
                                 (str (zero-pad (inc i) 4) "-" (if uname (str uname ".mp3") name))))

        dir-tree-hnd  (fn [i dir-obj]
                        "Processes the current directory, source side;
                        creates properly named destination directory"
                        (let [dir (.getPath dir-obj)
                              dir-name (fs/base-name dir-obj)
                              step (str dst-step *nix-sep* (dir-name-decorator i dir-name))]
                          (fs/mkdir (str dst-root step))
                          (traverse-dir dir dst-root step ffc!)))

        dir-flat-hnd  (fn [i dir-obj]
                        "Processes the current directory, source side;
                        never creates any destination directories"
                        (let [dir (.getPath dir-obj)
                              dir-name (fs/base-name dir-obj)]
                          (traverse-dir dir dst-root "" ffc!)))

        file-tree-hnd (fn [i file-obj]
                        "Copies the current file, properly named and tagged"
                        (let [file-name (.getName file-obj)
                              rel-dst (str dst-step *nix-sep* (file-name-decorator i file-name))
                              dst-path (str dst-root rel-dst)]
                          ;(fs/copy file-obj (fs/file dst-path))
                          (ffc!)
                          (vector (.getPath file-obj) dst-path)))

        file-flat-hnd (fn [i file-obj]
                        "Copies the current file, properly named and tagged"
                        (let [file-name (.getName file-obj)
                              dst-path (str dst-root dst-step *nix-sep* (file-name-decorator (ffc!) file-name))]
                          ;(fs/copy file-obj (fs/file dst-path))
                          (vector (.getPath file-obj) dst-path)))

        file-handler  (fn []
                        "Returns proper file handler according to options"
                        (let []
                          (if (:tree-dst options) file-tree-hnd file-flat-hnd)))

        dir-handler   (fn []
                        "Returns proper directory handler according to options"
                        (let []
                          (if (:tree-dst options) dir-tree-hnd dir-flat-hnd)))]

    (doall (concat (map-indexed (dir-handler) dirs) (map-indexed (file-handler) files))))) ;; traverse-dir

(defn- build-album
  "Copy source files to destination according
  to command line options"
  []
  (let [{:keys [options arguments]} *parsed-args*
        path-trimmer (fn [str] (.getPath (fs/file str)))
        uname (:unified-name options)
        anum (:album-num options)
        arg-src (path-trimmer (arguments 0))
        src-name (fs/base-name (fs/file arg-src))
        arg-dst (path-trimmer (arguments 1))
        base-dst (if anum
                   (if uname
                     (str (zero-pad anum 2) "-" uname)
                     (str (zero-pad anum 2) "-" src-name))
                   src-name)
        tail (if (:drop-dst options) "" (str *nix-sep* base-dst))
        executive-dst (str arg-dst tail)
        file-counter (counter 0)]

    (or (:drop-dst options) (fs/mkdir executive-dst))
    (vector arg-src executive-dst file-counter (traverse-dir arg-src executive-dst  "" file-counter))))

(defn- copy-album
  "Copy actual files in the reversed order
  if requested, according to the file list
  'ammo belt', set tags"
  []
  (let [[src dst fcnt! & belt] (build-album)
        {:keys [options]} *parsed-args*

        file-count (fcnt!)
        ready-belt (->> (flatten belt) (partition 2) (map vec))

        tag-map (fn [i]
                  (if (:album-tag options)
                    {:track (str (inc i)) :track-total (str file-count) :album (:album-tag options)}
                    {:track (str (inc i)) :track-total (str file-count)}))

        file-copy-n-print   (fn [entry tags]
                              (let [[src dst] entry]
                                (fs/copy (fs/file src) (fs/file dst))
                                (core/update-tag! dst tags)
                                (println (format "%4d/%-4d %s"
                                        (Integer/parseInt (:track tags))
                                        (Integer/parseInt (:track-total tags)) dst))
                                entry))

        file-copier         (fn [i entry] (file-copy-n-print entry (tag-map i)))

        reverse-file-copier (fn [i entry] (file-copy-n-print entry (tag-map (- file-count i 1))))]

    (if (:reverse options)
      (doall (map-indexed reverse-file-copier (reverse ready-belt)))
      (doall (map-indexed file-copier ready-belt)))))

(defn -main
  "Parsing the Command Line and Giving Orders"
  [& args]
  (def ^:dynamic *parsed-args* (parse-opts args cli-options))
  (let [{:keys [options arguments errors summary]} *parsed-args*]
    (cond
     (not (nil? errors)) (println errors)
     (:help options) (println (usage summary))
     (not= (count arguments) 2) (println (usage summary))
     :else (copy-album))))
;;
;;
;;
;; Editor breathing space: might be necessary :)
;;
;;
;;
