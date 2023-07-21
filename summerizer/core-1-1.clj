(ns mimi.summarizer.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; Parsing HTTP response
(defn parse-http-response [response]
  (json/read-str (:body response) :key-fn keyword))

;; Sending a query to the GPT service
(defn send-query-to-service [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-4"
              :messages [{:role "user" :content query}]
              :temperature 1.25}
        json-body (json/write-str body)]
     (http/post url {:body (.getBytes json-body "UTF-8")
                     :headers headers})))

;; Retrieving service response
(defn retrieve-service-response [query]
  (-> query
      send-query-to-service
      parse-http-response
      :choices
      first
      :message
      :content))

;; Enhancing a given file content
(defn enhance-file-content [content]
  (let [request (str "Please return following enhanced file content based on these actions:\n\n"
                      "1. Remove any broken or unused code blocks.\n"
                      "2. Comply the naming conventions such as function names or variable identifiers.\n"
                      "3. Next, consider codes that further boost the functionality of this file.\n"
                      "4. Lastly, expand this given transcript for a fuller understanding of the requested enhancement.\n\n"
                      "No repeating writings, introductions, and conclusions are necessary.\n\n"
                      "-- File Content: --\n\n" content)
        enhanced-content (retrieve-service-response request)]
    enhanced-content))

;; Process file with the above enhancements
(defn process-and-enhance-file [file]
  (let [raw-content (slurp file)
        enhanced-content (enhance-file-content raw-content)]
    (spit file enhanced-content)
    (println (str "Finished enhancing file: " (.getName file)))))

;; Seamlessly processing all files within a specified directory
(defn process-all-files-in-dir [directory-path]
  (let [files (filter #(.isFile %) (file-seq (io/file directory-path)))]
    (doseq [file files]
        (process-and-enhance-file file))))

(process-all-files-in-dir "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")