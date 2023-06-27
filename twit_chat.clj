(ns mimi.twit_chat
  [:require [clj-http.client :as client]
   [clojure.data.json :as json]])

(defn parse-response-body [response]
  (json/read-str (response :body) :key-fn keyword))

(defn post-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-4"
              :messages [{:role "user" :content query}]
              :temperature 0.7}
        json-body (json/write-str body)
        byte-array-body (.getBytes json-body "UTF-8")]
    (client/post url {:body byte-array-body
                      :headers headers})))

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
      p+))

;; (defn tweet [tweet-text]
;;   (let [url "https://api.twitter.com/1.1/statuses/update.json"
;;         headers {:Authorization (str "Bearer " (System/getenv "TWITTER_ACCESS_TOKEN"))}
;;         body {:status tweet-text}
;;         json-body (json/write-str body)
;;         byte-array-body (.getBytes json-body "UTF-8")]
;;     (client/post url {:body byte-array-body
;;                       :headers headers})))

(defn tweet [tweet-text]
  (let [url "https://api.twitter.com/1.1/statuses/update.json"
        headers {:Authorization (str "Bearer " (System/getenv "TWITTER_ACCESS_TOKEN"))}
        params {:status tweet-text}]
    (client/post url {:headers headers
                      :form-params params
                      :debug true})))

(defn handle-tweet [query]
  (let [response-text (query-gpt query)]
    (tweet response-text)))

;; Example usage
;; (handle-tweet "Hello, Twitter! This is my first tweet from my bot.")