(ns app.core
  (:require [clj-http.client :as http-client]
            [clojure.data.json :as json]
            [clojure.java.io :as java-io]
            [clojure.string :as str]))

(defn parse-api-response [response-body]
  "Parse the API's response body from JSON into a Clojure map."
  (json/read-str (:body response-body) :key-fn keyword))

(defn fetch-api-response [user-request]
  "Send a POST request to the API and get the response."
  (let [api-endpoint "https://api.openai.com/v1/chat/completions"
        request-config {:model "gpt-4"
                        :messages [{:role "user" :content user-request}]
                        :temperature 1.01}
        header-content {"Content-Type" "application/json"
                        "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        formatted-request (json/write-str request-config)]
    (try
      (http-client/post api-endpoint {:body (.getBytes formatted-request "UTF-8")
                                      :headers header-content})
      (catch Exception e
        {:status 500, :body (str "Failed to fetch API's response: " (.getMessage e))}))))

(defn extract-output [user-request]
  "Extract the output from the API's response."
  (-> user-request
      fetch-api-response
      parse-api-response
      :choices
      first
      :message
      :content))

(defn apply-enhancements [file-content]
  "Apply enhancements to the file_content using the API's response."
  (let [enhancement-desc (str "Enhancements:"
                              "1. Remove unfitting blocks of code."
                              "2. Align with naming standards."
                              "3. Review possible delays."
                              "4. Improve documentation."
                              "5. Create functional framework for enhancements."
                              "-- File: --" file-content)
        enhanced-content (extract-output enhancement-desc)]
    enhanced-content))

(defn refine-code [source-file]
  "Refine the file source code with the enhancements from the API."
  (let [file-source (slurp source-file)
        optimized-content (apply-enhancements file-source)]
    (spit source-file optimized-content)
    (println (str "File enhancement successful: " (.getName source-file)))))

(defn initiate-enhancement-process [file-path]
  "Initiate enhancing the file ..."
  (refine-code file-path))

(initiate-enhancement-process "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")