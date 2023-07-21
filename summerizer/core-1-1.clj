(ns mimi.summarizer.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn parse-response-body [response]
  (-> response :body json/read-str :key-fn keyword))

(defn post-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-3.5-turbo"
              :messages [{:role "user" :content query}]
              :temperature 1.5}
        json-body (json/write-str body)]
    (http/post url {:body (.getBytes json-body "UTF-8")
                    :headers headers})))

(defn query-gpt [query]
  (-> query
      post-gpt
      parse-response-body
      :choices
      first
      :message
      :content))

(defn enhance-file [file-content]
  (query-gpt (str "Your mission is to:
                   Please update this file with enhancements without increasing the file size.
                   Add new enhancements as clojure code allowing this file greater utility.
                   Enhancements could be made to this string prompt too.
                   If you need more room, please refactor existing code in order to make room.
                   Also, please remove any dead, unused code.
                   Also, please rename functions and variables if you think of better names for them.
                   Here is the file content: " file-content
                  "Remember to return ONLY updated file content. No ai voice. No ai summary. No ai description.")))

(defn process-file [file]
  (let [content (slurp file)
        enhanced-content (enhance-file content)]
    (spit file enhanced-content)
    (println (str "done processing " (.getName file)))))

(defn process-dir [target-dir-path]
  (let [files (->> target-dir-path
                   io/file
                   file-seq
                   (filter #(.isFile %)))]
    (doseq [file files]
      (process-file file))))

(defn update-file [file-path enhancement]
  (let [content (slurp file-path)
        updated-content (str content enhancement)]
    (spit file-path updated-content)))

(defn get-file-size [file-path]
  (-> file-path io/file :file-size))

(defn get-file-lines [file-path]
  (with-open [reader (io/reader file-path)]
    (doall (line-seq reader))))

(defn append-to-file [file-path content]
  (with-open [writer (io/writer file-path :append true)]
    (.write writer content)))

(defn prepend-to-file [file-path content]
  (let [existing-content (slurp file-path)]
    (spit file-path (str content existing-content))))

(defn insert-at-line [file-path line content]
  (let [lines (get-file-lines file-path)
        updated-lines (assoc lines line (str (get lines line "\n") content "\n"))]
    (spit file-path (clojure.string/join "\n" updated-lines))))

(process-dir "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")