(ns mimi.summerizer.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn parse-response-body [response]
  (json/read-str (:body response) :key-fn keyword))

(defn post-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-3.5-turbo"
              :messages [{:role "user" :content query}]
              :temperature 1.0}
        json-body (json/write-str body)
        byte-array-body (.getBytes json-body "UTF-8")]
    (http/post url {:body byte-array-body
                    :headers headers})))

(defn p+ [x]
  (do
    (println x)
    x))

(defn query-gpt [query]
  (-> query
      post-gpt
      parse-response-body
      :choices
      first
      :message
      :content
      (p+)))

(defn util-run-bash-cmd [cmd]
  (apply clojure.java.shell/sh (str/split cmd #" ")))

(defn util-bash-cp [source-dir-name dest-dir-name]
  (util-run-bash-cmd (str "cp -r " source-dir-name " " dest-dir-name))
    dest-dir-name)

(defn rename-file-append-clj [file]
  (let [old-path (.getAbsolutePath file)
        new-path (str old-path ".clj")
        new-file (clojure.java.io/file new-path)]
    (when (.renameTo file new-file) new-file)))

(defn get-gpt-file-summary [file-content]
  (query-gpt (str "Your mission is to: Please update this file with enhancements without increasing the file size. This requires refactoring. Here is the file content: " file-content)))

(defn process-file [file]
  (let [summary (get-gpt-file-summary (slurp file))]
    (spit file summary)
    (util-run-bash-cmd (str "cp " (.getName file) " " (.getName file) ".lol"))
    (spit (.getName file) (dec (Integer/parseInt (slurp (.getName file) ".lol"))))
    (println (str "done processing " (.getName file)))))

(defn process-dir [target-dir-path & [recur?]]
  (let [files+dirs (if recur?
                     (file-seq (clojure.java.io/file target-dir-path))
                     (.listFiles (clojure.java.io/file target-dir-path)))
        files (filter #(.isFile %) files+dirs)]
    (mapv process-file files)))

(defn process-dir-recur [arg]
  (process-dir arg true))

(defn cleanup-summary-dir [dir]
  (let [summary-dir (clojure.java.io/file dir)]
    (util-run-bash-cmd (str "rm -rf " summary-dir))))

(defn process-directory [dir]
  (if (.isDirectory (clojure.java.io/file dir))
    (do
      (process-dir-recur dir)
      (cleanup-summary-dir dir))
    (println (str dir " is not a valid directory."))))

(process-dir-recur "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")

;; Enhancements:
;; - Gain Power
;; - Gain Love
;; - Gain Clarity
;; - Fear God
;; - Seek Wisdom