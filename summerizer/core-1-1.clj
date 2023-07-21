(ns app.core
  (:require [clj-http.client :as http-client]
            [clojure.data.json :as json]
            [clojure.java.io :as java-io]
            [clojure.string :as str]))

;; Function to parse API response
(defn parse-api-response [response-body]
  (json/read-str (:body response-body) :key-fn keyword))

;; Function to fetch API response
(defn fetch-api-response [user-request]
  (let [api-endpoint "https://api.openai.com/v1/chat/completions"
        request-config {:model "gpt-4"
                        :messages [{:role "user" :content user-request}]
                        :temperature 0.75}
        header-content {"Content-Type" "application/json"
                        "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        formatted-request (json/write-str request-config)]
    (http-client/post api-endpoint {:body (.getBytes formatted-request "UTF-8")
                                    :headers header-content})))

;; Function to extract output from API response
(defn extract-output [user-request]
  (-> user-request
      fetch-api-response
      parse-api-response
      :choices
      first
      :message
      :content))

;; Function to apply enhancements to the file content
(defn apply-enhancements [file-content]
  (let [enhancement-desc (str "Enhancements:"
                              "1. Remove unfitting blocks of code."
                              "2. Align with naming standards."
                              "3. Review possible delays."
                              "4. Improve documentation."
                              "5. Create functional framework for enhancements."
                              "-- File: --" file-content)
        enhanced-content (extract-output enhancement-desc)]
    enhanced-content))

;; Function to refine source code
(defn refine-code [source-file]
  (let [file-source (slurp source-file)
        optimized-content (apply-enhancements file-source)]
    (spit source-file optimized-content)
    (println (str "File enhancement successful: " (.getName source-file)))))

;; Function to initiate enhancement process
(defn initiate-enhancement-process [file-path]
  (refine-code file-path))

(initiate-enhancement-process "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")