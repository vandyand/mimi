(ns mimi.summerizer.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.core :as core]
            [clojure.data.json :as json]))


(defn parse-response-body [response]
  (json/read-str (response :body) :key-fn keyword))

(defn post-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-3.5-turbo"
              :messages [{:role "user" :content query}]
              :temperature 1.0
              :max_tokens 1056}
        json-body (json/write-str body)
        byte-array-body (.getBytes json-body "UTF-8")]
    (http/post url {:body byte-array-body
                    :headers headers
                    :debug true
                    :chunked true
                    })))

(defn p+ [x]
  x
  (do
    (println x)
    x))

(defn query-gpt [query]
  (-> query
      post-gpt
      parse-response-body
      :choices
      first
      :message
      :content
    ;;   p+
      ))

(defn get-gpt-file-summary [file-content]
  (query-gpt (str "Your mission is to: Please summarize this file in MARKDOWN format. Here is the file content: " file-content)))

(defn util-run-bash-cmd [cmd]
  (apply clojure.java.shell/sh (clojure.string/split cmd #" ")))

(defn copy-directory [target-dir-name]
  (let [summary-dir-name (str target-dir-name "-summary")]
    (util-run-bash-cmd (str "cp -r " target-dir-name " " summary-dir-name))
    summary-dir-name))

(defn rename-file-append-md [file]
  (let [old-path (.getAbsolutePath file)
        new-path (str old-path ".md")
        new-file (clojure.java.io/file new-path)]
    (when (.renameTo file new-file) new-file)))

(defn process-file [file]
  (let [foo (println (str "processing " (.getName file)))
        _file (rename-file-append-md file)
        foo (println (str "renamed " (.getName file) " to " (.getName _file)))
        summary (get-gpt-file-summary (slurp _file))]
    (spit _file summary)
    (println (str "done processing " (.getName _file)))))

(defn process-dir [target-dir-path & recur?]
  (let [foo (println {:target-dir-path target-dir-path})
        summary-dir-name (copy-directory target-dir-path)
        foo (println {:summary-dir-name summary-dir-name})
        files+dirs-recur (file-seq (clojure.java.io/file summary-dir-name))
        files+dirs (.listFiles (clojure.java.io/file summary-dir-name))
        files (filter #(.isFile %) (if recur? files+dirs-recur files+dirs))
        foo (println {:files files})]
    (mapv process-file files)))

(defn process-dir-recur [arg]
  (process-dir arg true))

(process-dir-recur "/Users/kingjames/work/old-app/gantt/schedule/iframe-overlay.js")