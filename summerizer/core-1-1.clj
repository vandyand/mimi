(ns mimi.summarizer.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]))

(defn parse-response-body [response]
  (json/read-str (:body response) :key-fn keyword))

(defn post-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-3.5-turbo"
              :messages [{:role "user" :content query}]
              :temperature 1.0}
        json-body (json/write-str body)]
    (http/post url {:body (.getBytes json-body "UTF-8")
                    :headers headers})))

(defn p+ [x]
  (println x)
  x)

(defn query-gpt [query]
  (-> query
      post-gpt
      parse-response-body
      :choices
      first
      :message
      :content
      p+))

(defn util-run-bash-cmd [cmd]
  (apply clojure.java.shell/sh (str/split cmd #" ")))

(defn get-gpt-file-summary [file-content]
  (query-gpt file-content))

(defn process-file [file]
  (let [content (slurp file)
        summary (get-gpt-file-summary content)]
    (spit file summary)
    (println (str "done processing " (.getName file)))))

(defn process-dir [target-dir-path]
  (let [files+dirs (file-seq (clojure.java.io/file target-dir-path))
        files (filter #(.isFile %) files+dirs)]
    (mapv process-file files)))

(defn update-file [file-path enhancement]
  (let [content (slurp file-path)
        updated-content (str content enhancement)]
    (spit file-path updated-content)))

(defn get-file-size [file-path]
  (let [file (io/file file-path)]
    (:file-size (io/file file))))

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