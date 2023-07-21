(ns mimi.summarizer.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn parse-response-body [response]
  (json/read-str (:body response) :key-fn keyword))

(defn send-query-to-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-3.5-turbo"
              :messages [{:role "user" :content query}]
              :temperature 1.2}
        json-body (json/write-str body)]
    (http/post url {:body (.getBytes json-body "UTF-8")
                    :headers headers})))

(defn get-gpt-response [query]
  (-> query
      send-query-to-gpt
      parse-response-body
      :choices
      first
      :message
      :content))

(defn enhance-file [file-content]
  (let [prompt (str "Please consider the following updates and enhancements for this file:\n"
                    "\n"
                    "- Refactor existing code to remove dead, unused code.\n"
                    "- Rename functions and variables for clarity and better naming conventions.\n"
                    "- Allow for additional enhancements as Clojure code to improve utility.\n"
                    "- Update the string prompt to provide more context and details for the desired enhancements.\n"
                    "\n"
                    "Remember to return ONLY the updated file content. Avoid AI voices, summaries, and descriptions."
                    "\n\n"
                    "Here is the file content:\n\n"
                    file-content)
        enhanced-content (get-gpt-response prompt)]
    enhanced-content))

(defn process-file [file]
  (let [content (slurp file)
        summary (enhance-file content)]
    (spit file summary)
    (println (str "Done processing " (.getName file)))))

(defn process-dir [target-dir-path]
  (let [files (filter #(.isFile %) (file-seq (clojure.java.io/file target-dir-path)))]
    (doseq [file files]
      (process-file file))))

(defn update-file [file-path enhancement]
  (let [content (slurp file-path)
        updated-content (str content enhancement)]
    (spit file-path updated-content)))

(defn get-file-size [file-path]
  (-> (io/file file-path) :file-size))

(defn get-file-lines [file-path]
  (let [file (io/file file-path)]
    (with-open [reader (io/reader file)]
      (doall (line-seq reader)))))

(defn append-to-file [file-path content]
  (with-open [writer (io/writer file-path :append true)]
    (.write writer content)))

(defn prepend-to-file [file-path content]
  (let [existing-content (slurp file-path)]
    (spit file-path (str content existing-content))))

(defn insert-at-line [file-path line content]
  (let [lines (get-file-lines file-path)
        updated-lines (update lines line #(str % content "\n"))]
    (spit file-path (clojure.string/join "\n" updated-lines))))

(process-dir "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")