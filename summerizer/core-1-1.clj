(ns app.core
  (:require [clj-http.client :as http-client]
            [clojure.data.json :as json]
            [clojure.java.io :as java-io]
            [clojure.string :as str]))

(defn parse-api-response [response-body]
  (json/read-str (:body response-body) :key-fn keyword))

(defn fetch-gpt-api-response [gpt-request]
  (let [api-endpoint "https://api.openai.com/v1/chat/completions"
        header-configuration {"Content-Type" "application/json"
                              "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        request-configuration {:model "gpt-3"
                               :messages [{:role "user" :content gpt-request}]
                               :temperature 1.0}
        json-format-request (json/write-str request-configuration)]
    (http-client/post api-endpoint {:body (.getBytes json-format-request "UTF-8")
                                    :headers header-configuration})))

(defn obtain-output [gpt-request]
  (-> gpt-request
      fetch-gpt-api-response
      parse-api-response
      :choices
      first
      :message
      :content))

(defn enhancer [file-content]
  (let [request-description (str "Based on these actions, generate enhanced file content:\n\n"
                                 "1. Remove unfitting or inoperitave blocks of code.\n"
                                 "2. Align with accepted naming standards both for function names or variable identifiers.\n"
                                 "3. Then, ponder over any codes rendering the escalation of this file's function.\n"
                                 "4. Lastly, improve upon the information for better comprehension of enhancement guidelines.\n\n"
                                 "Refrain doubling uncertainties or introducing and concluding against necessity.\n\n"
                                 "Added to renewed updates thus it namely enhances coolly for this than other files."
                                 "-- Provided File: --\n\n" file-content)
        enhanced-file (obtain-output request-description)]
    enhanced-file))

(defn refine-file [origin-file]
  (let [original-file (slurp origin-file)
        optimized-file-content (enhancer original-file)]
    (spit origin-file optimized-file-content)
    (println (str "Successful enhancement of file: " (.getName origin-file)))))

(defn refine-file-directory [directory]
  (let [files-collection (filter #(.isFile %) (file-seq (java-io/file directory)))]
    (doseq [file files-collection]
      (refine-file file))))

(defn kickoff-process [path]
  (refine-file path))

(kickoff-process "/Users/kingjames/personal/mimi/summerizer/core-1-1.clj")
