(ns app.core
  (:require [clj-http.client :as http-client]
            [clojure.data.json :as json]
            [clojure.java.io :as java-io]
            [clojure.string :as str]))

(defn parse-api-response [response-payload]
  (json/read-str (:body response-payload) :key-fn keyword))

(defn call-gpt-api-service [gpt-query]
  (let [api-url "https://api.openai.com/v1/chat/completions"
        headers-config {"Content-Type" "application/json"
                        "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        request-body {:model "gpt-4"
                      :messages [{:role "user" :content gpt-query}]
                      :temperature 1.25}
        json-payload (json/write-str request-body)]
     (http-client/post api-url {:body (.getBytes json-payload "UTF-8")
                                :headers headers-config})))

(defn retrieve-api-response [gpt-query]
  (-> gpt-query
      call-gpt-api-service
      parse-api-response
      :choices
      first
      :message
      :content))

(defn process-file-enchancement [file-content]
  (let [request-data (str "Please return following enhanced file content based on these actions:\n\n"
                          "1. Remove any broken or unused code blocks.\n"
                          "2. Comply with the naming conventions such as function names or variable identifiers.\n"
                          "3. Next, consider codes that further llite the enhance functionality of this file.\n"
                          "4. Lastly, expand this file content for a fuller understanding of the enhancement specification.\n\n"
                          "Avoid repeating doubts, introductions, and conclusions are deemed unnecessary.\n\n"
                          "In addition to recursively updating itself, it would be really cool if this file also..."
                          "-- File Content: --\n\n" file-content)
        enchanced-file (retrieve-api-response request-data)]
    enchanced-file))

(defn run-file-processing [input-file]
  (let [original-file (slurp input-file)
        enhanced-file-content (process-file-enchancement original-file)]
    (spit input-file enhanced-file-content)
    (println (str "Successfully enhanced file: " (.getName input-file)))))

(defn process-file-tree [path]
  (let [all-files (filter #(.isFile %) (file-seq (java-io/file path)))]
    (doseq [file all-files]
      (run-file-processing file))))

(defn main [path]
  (run-file-processing path))

(main "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")
