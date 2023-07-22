(ns mimi.summerizer.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.core :as core]
            [clojure.data.json :as json]))

(defn parse-response-body [response]
  (json/read-str (response :body) :key-fn keyword))

(def functions 
  [{:name "gen-ate"
    :description "Generate an operable."}])

(defn post-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-3.5-turbo"
              :messages [{:role "user" :content query}
                         {:role "function" }]
              :functions [{:name "get_current"}]
              :temperature 1.0
              :max_tokens 1056}
        json-body (json/write-str body)
        byte-array-body (.getBytes json-body "UTF-8")]
    (http/post url {:body byte-array-body
                    :headers headers
                    :debug true
                    :chunked true})))

(defn p+ [x]
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
      (p+)))

(defn get-gpt-file-enhancements [file-content]
  (query-gpt (str "Your mission is to: Please update this file with enhancements. Here is the file content: " file-content)))

(defn util-run-bash-cmd [cmd]
  (apply clojure.java.shell/sh (clojure.string/split cmd #" ")))

(defn bash-copy [target-name]
  (let [summary-dir-name (str target-name "-1")]
    (util-run-bash-cmd (str "cp -r " target-name " " summary-dir-name))
    summary-dir-name))

(defn rename-file-append-clj [file]
  (let [old-path (.getAbsolutePath file)
        new-path (str old-path ".clj")
        new-file (clojure.java.io/file new-path)]
    (when (.renameTo file new-file) new-file)))

(defn process-file [file]
  (let [_file (rename-file-append-clj file)
        summary (get-gpt-file-enhancements (slurp _file))]
    (spit _file summary)
    (println (str "done processing " (.getName _file)))))

(defn update-file-name [file-name]
 (let [y (clojure.string/split file-name #"\.")
      z (str (first y) (rand-int 10) (when (second y) (str "." (second y))))]
  z))

;; (process-dir-recur "/Users/kingjames/personal/mimi/summerizer/core-1.clj")

(def db (atom []))
(def root-dir (atom {}))
(def )

(defn append-to-db [data]
  (swap! db conj data))

(defn fetch-op []
  (append-to-db
  (query-gpt 
   (str "Please return an operable object based on the following information")))
  (println "doing thing 1"))

(defn spectate [])
(defn mutate [])
(defn dictate [])

(defn exec-op [] ;; we need to execute the operable
  (let [op (last @db)]
    (cond
      (clojure.string/includes? op "SPECTATE") (spectate)
      (clojure.string/includes? op "MUTATE") (mutate)
      (clojure.string/includes? op "DICTATE") (dictate))
    )
  (append-to-db "doing thing 2")
  (println "doing thing 2"))



(loop [i 0]
  (fetch-op) ;; send gpt req -> get operable (use function!)
  (exec-op) ;; edit internal representation via an action
        
  (if (> i 10) nil (recur (inc i))))

