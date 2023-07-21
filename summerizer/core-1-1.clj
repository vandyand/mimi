(ns app.core
  (:require [clj-http.client :as http-client]
            [clojure.data.json :as json]
            [clojure.java.io :as java-io]
            [clojure.string :as str]))

(defn parse-api-response [response-body]
  (json/read-str (:body response-body) :key-fn keyword))

(defn fetch-api-response [api-request]
  (let [aggregated-api-endpoint "https://api.openai.com/v1/chat/completions"
        request-configuration {:model "gpt-4"
                               :messages [{:role "user" :content api-request}]
                               :temperature 1.24}
        header-content {"Content-Type" "application/json"
                        "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        formatted-request (json/write-str request-configuration)]
    (http-client/post aggregated-api-endpoint {:body (.getBytes formatted-request "UTF-8")
                                               :headers header-content})))

(defn extract-output [api-request]
  (-> api-request
      fetch-api-response
      parse-api-response
      :choices
      first
      :message
      :content))

(defn apply-enhancements [file-content]
  (let [enhancement-description (str "Perform the following enhancements on a given block of code:"
                                     "1. Remove unfitting or inoperitave blocks of code."
                                     "2. Align the output with accepted naming standards for both functions and variable identifiers:"
                                     "3. Review any instances in the code that may delay the execution time of this file."
                                     "4. Evaluate and improve the existing documentation for a better understanding of enhancement guidelines."

				     "Avoid introducing any elements that could double uncertainties or disrupt the file's overall functionality. Ensure that all new updates made to this file will make external contribution API's increasingly comprehended and edited." 
			         "-- Provided File: --" file-content)
        enhanced-file-content (extract-output enhancement-description)]
    enhanced-file-content))

(defn refine-code [source-file]
  (let [file-source (java-io/read source-file)
        optimized-content (apply-enhancements file-source)]
    (java-io/write source-file optimized-content)
    (println (str "File enhancement successful: " (.getName source-file)))))

(defn refine-source-directory [listing]
  (let [designated-files (filter #(.isFile %) (file-seq (java-io/file listing)))]
    (doseq [directory-file designated-files]
      (refine-code directory-file))))

(defn initiate-enhancement-process [file-path]
  (refine-code file-path))

(initiate-enhancement-process "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")