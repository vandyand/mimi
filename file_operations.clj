(ns mimi.file_operations
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn create-file [file-path content]
  (spit file-path content))

(defn read-file [file-path]
  (slurp file-path))

(defn append-to-file [file-path content]
  (let [existing-content (read-file file-path)]
    (spit file-path (str existing-content content))))

(defn delete-file [file-path]
  (io/delete-file file-path))

(defn delete-lines [file-path start nskip]
  (let [lines (str/split-lines (slurp file-path))
        new-lines (concat (take (dec start) lines)
                          (drop (+ start nskip -1) lines))]
    (spit file-path (str/join "\n" new-lines))))

(defn update-file [file-path content & [line-number]]
  (let [lines (str/split-lines (slurp file-path))
        new-lines (if line-number
                    (concat (take line-number lines)
                            [content]
                            (drop line-number lines))
                    (conj lines content))]
    (spit file-path (str/join "\n" new-lines))))

(defn update-lines [file-path new-content start nskip ]
  (delete-lines file-path start nskip)
  (update-file file-path new-content (if (= start 0) 0 (dec start))))

(defn search-file [file-path search-str]
  (let [lines (str/split-lines (slurp file-path))
        line-nums (map-indexed 
                   (fn [idx line] 
                     (when 
                      (str/includes? line search-str) 
                       (inc idx))) 
                   lines)]
    (vec (remove nil? line-nums))))

(let [file-path "target.clj"]
  (create-file file-path "Initial content\nLine 2\nLine 3\nAnother Line 2")
  (println (read-file file-path))
  (println (search-file file-path "Line 2")))



;; (let [file-path "target.clj"]
;;   (println "file content:\n" (read-file file-path))
;;   (update-lines file-path "update", 4 2)
;;   (println "file content:\n" (read-file file-path)))

;; (let [file-path "target.clj"]
;;   (create-file file-path "Initial content")
;;   (println (read-file file-path))
;;   (update-file file-path " Updated content")
;;   (println (read-file file-path))
;;   (delete-lines file-path 2 1)
;;   (println (read-file file-path))
;;   (append-to-file file-path " Appended content" 2)
;;   (println (read-file file-path)))