(ns app.core
  (:require [clj-http.client :as httpClient]
            [clojure.data.json :as json]
            [clojure.java.io :as javaIO]
            [clojure.string :as str]))

(defn parseAPIResponse [responsePayload]
  (json/read-str (:body responsePayload) :key-fn keyword))

(defn callAPIGPTService [gptQuery]
  (let [apiUrl "https://api.openai.com/v1/chat/completions"
        headersConfiguration {"Content-Type" "application/json"
                              "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        requestBody {:model "gpt-4"
                     :messages [{:role "user" :content gptQuery}]
                     :temperature 1.25}
        jsonPayload (json/write-str requestBody)]
     (httpClient/post apiUrl {:body (.getBytes jsonPayload "UTF-8")
                              :headers headersConfiguration})))

(defn retrieveAPIResponse [gptQuery]
  (-> gptQuery
      callAPIGPTService
      parseAPIResponse
      :choices
      first
      :message
      :content))

(defn processFileEnchancement [fileContent]
  (let [requestData (str "Please return following enhanced file content based on these actions:\n\n"
                         "1. Remove any broken or unused code blocks.\n"
                         "2. Comply the naming conventions such as function names or variable identifiers.\n"
                         "3. Next, consider codes that further boost the functionality of this file.\n"
                         "4. Lastly, expand this file content for a fuller understanding of the enhancement specification.\n\n"
                         "Avoid repeating doubts, introductions, and conclusions are deemed unnecessary.\n\n"
                         "-- File Content: --\n\n" fileContent)
        enchancedFile (retrieveAPIResponse requestData)]
    enchancedFile))

(defn automateFileProcess [inputFile]
  (let [currentFile (slurp inputFile)
        enhancedFileContent (processFileEnchancement currentFile)]
    (spit inputFile enhancedFileContent)
    (println (str "Completed file enhancement: " (.getName inputFile)))))

(defn processAllFiles [path]
  (let [allFiles (filter #(.isFile %) (file-seq (javaIO/file path)))]
    (doseq [f allFiles]
      (automateFileProcess f))))

(processAllFiles "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")