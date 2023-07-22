(ns mimi.functions.fns
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]))

(defn parse-response-body [response]
  (json/read-str (response :body) :key-fn keyword))

(def functions
  [{:name "mutate"
    :description "This fn transitions the state machine to the 'mutate' state.
                  The 'memory' is appended to the database"
    :parameters {:type "object"
                 :required ["memory" "rationale"]
                 :properties {:memory {:type "string"
                                       :description "The memory to append to the agent memory database"}
                              :rationale {:type "string"
                                          :enum ["info" "enhancement" "note" "forget"]}}}}
   {:name "spectate"
    :description "This fn transitions the state machine to the 'spectate' state.
                  The agent seeks to 'spectate' information according to the info-req"
    :parameters {:type "object"
                 :required ["info-req"]
                 :properties {:info-req {:type "string"
                                         :description "What type of information to look for"}}}}
   {:name "dictate"
    :description "This fn transitions the state machine to the 'dictate' state.
                  The agent generates output according to the task-spec"
    :parameters {:type "object"
                 :required ["task-spec"]
                 :properties {:task-spec {:type "string"
                                          :description "What type of bash or clojure command to execute"}}}}])

(defn post-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-4"
              :messages [{:role "user" :content query}]
              :functions functions
              ;; :function_call "auto"
              :temperature 0.25}
        json-body (json/write-str body)
        byte-array-body (.getBytes json-body "UTF-8")]
    (http/post url {:body byte-array-body
                    :headers headers})))

(defn query-gpt [query]
  (-> query
      post-gpt
      parse-response-body
      :choices
      first))

(def db (atom []))
(def sm (atom {:cur-state nil}))
(def mission (atom (str "Our mission is to switch to different states in the state machine.\n"
                        "Please chose a new state based on your best judgement.\n"
                        "If the database is empty, please call the 'mutate' function.\n"
                        "If the current state is 'mutate' please transition to 'spectate' or 'dictate'.\n"
                        "If the current state is 'spectate' please transition to 'mutate' or 'dictate'.\n"
                        "If the current state is 'dictate' please transition to 'spectate' or 'mutate'.\n"
                        "Please reference current state info-req and or command.\n"
                        "Please do not chose the current state for the destination state.\n")))

(defn mutate [memory, rationale]
  (swap! sm assoc :cur-state "mutate")
  (swap! db conj {:id (str  (rand-int 1000) "-" (.getTime (java.util.Date.)))
                  :memory memory
                  :rationale rationale}))

(defn spectate [info-req]
  (reset! sm {:cur-state "spectate" :info-req info-req}))

(defn dictate [command]
  (reset! sm {:cur-state "dictate" :command command}))

(defn handle-fn-call [gpt-response]
  (let [fn-call-info (-> gpt-response :message :function_call)
        fn-name-to-call (:name fn-call-info)
        fn-to-call (cond 
                     (= fn-name-to-call "mutate") mutate
                     (= fn-name-to-call "dictate") dictate
                     (= fn-name-to-call "spectate") spectate
                     )
        fn-args (->> 
                 fn-call-info 
                 :arguments 
                 clojure.data.json/read-str 
                 vec
                 (map last))]
    (apply fn-to-call fn-args))
  )

(loop [i 0]
  (let [gpt-response (query-gpt (str "database: " @db "\n\n state machine: " @sm " mission: " @mission))
        foo (println "\n\ngpt-response: " gpt-response)]
  (if (= (:finish_reason gpt-response) "function_call")
    (handle-fn-call gpt-response)
    (println "No function called from gpt response"))
    (if (> i 5) {:db @db :sm @sm :mission mission}
        (recur (inc i)))))
