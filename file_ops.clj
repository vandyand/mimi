(ns mimi.file_ops
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn get-line-config [start-line end-line num-lines]
  {:start-line start-line :end-line end-line :num-lines num-lines})

(defn create-file [file-path content]
  (spit file-path content))

(defn read-file [file-path]
  (slurp file-path))

(defn get-file-lines [file-path]
  (str/split-lines (slurp file-path)))

(defn post-file-lines [file-path lines]
  (spit file-path (str/join "\n" lines)))

(defn append-content-to-file [file-path content]
  (let [existing-content (read-file file-path)]
    (spit file-path (str existing-content content))))

(defn delete-file [file-path]
  (io/delete-file file-path))

(defn delete-lines ([file-path start-line-number finish-line-number]
                    """Delete lines start (inclusive) to finish (inclusive)"""
  (let [lines (get-file-lines file-path)
        new-lines (concat (take (dec start-line-number) lines)
                          (drop finish-line-number lines))]
    (post-file-lines file-path new-lines)))
  ([file-path start-line-number] (delete-lines file-path start-line-number (inc start-line-number))))

(defn add-content-to-file [file-path content & [start-line-number]]
  (let [lines (get-file-lines file-path)
        new-lines (if start-line-number
                    (let [start-line (dec start-line-number)
                          end-line (min (count lines) start-line-number)]
                      (concat (take start-line lines)
                              [content]
                              (drop (dec end-line) lines)))
                    (conj lines content))]
    (post-file-lines file-path new-lines)))

(defn mutate-lines ;; should this just delete-lines and add-content-to-file?
  ([file-path new-content start-line-number] (mutate-lines file-path new-content start-line-number (inc start-line-number)))
  ([file-path new-content start-line-number finish-line-number]
  ;;  (let [lines (get-file-lines file-path)
  ;;        new-lines (concat (take (dec start-line-number) lines)
  ;;                  [new-content]
  ;;                  (drop finish-line-number lines))]
  ;;    (post-file-lines file-path new-lines))
   
   (delete-lines file-path start-line-number finish-line-number)
   (add-content-to-file file-path new-content start-line-number)
   ))

(defn search-file [file-path search-str]
  (let [lines (str/split-lines (slurp file-path))
        line-nums (map-indexed 
                   (fn [idx line] 
                     (when 
                      (str/includes? line search-str) 
                       (inc idx))) 
                   lines)]
    (vec (remove nil? line-nums))))


;; (add-content-to-file "file_ops.clj" ";; New content!!!!" 80) ;;GOOD

;; (delete-lines "file_ops.clj" 87 93) ;;GOOD
(let [x 91 
      y (inc x)]
  (mutate-lines "file_ops.clj" (str ";; This should be line " x "\n;; And this should be line " y) x y))

;; TESTING BELOW THIS LINE!______________________________________________________________________________________________________________
(defn newFuncc [x] (+ x 3))
(def x 23)
(defn newFuncc [x] (+ x 3))

;; 1
(defn newFuncc [x] (+ x 3))
(defn newFuncc [x] (+ x 3))
;; 3
(def x 23)


;; This should be line 88
;; This should be line 90
;; This should be line 91
;; And this should be line 92

;;eddd
;;rihklh;;



;; 177