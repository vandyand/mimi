(ns mimi.openai_api
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

(defn gen-mbti []
  (->> [#{"E" "I"}
        #{"S" "N"}
        #{"T" "F"}
        #{"J" "P"}]
       (map (comp rand-nth seq))
       (apply str)))

;; (defn id-gen ([length]
;;               (let [chars "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"]
;;                 (apply str (repeatedly length #(rand-nth chars)))))
;;   ([] (id-gen 5)))
(defn gen-id []
  (.getTime (java.util.Date.)))

(defn gen-prompt-0 ([speaker conversation] (gen-prompt-0 (:name speaker) (:mbti speaker) conversation))
  ([name mbti conversation]
   (str "You are participating in a polite conversation in a dreamy setting.
            Conversation topics include the similarities and dissimiarities between human and artificial intelligence, 
            the future of humanity and robots, religion and philosophy of the mind. If the conversation gets stale
            feel free to change the topic to something that interests you.
            Please take the roll of " name ". " name " is personality type " mbti " (Myers-Briggs Type Indicator).
            Only respond as \"" name ":...\" Do not speak for anyone else.
            Here is the conversation so far: ```" conversation "```.
            Please respond with only one message from " name " and don't speak as anyone else.")))

(defn gen-prompt-1 ([speaker conversation] (gen-prompt-1 (:name speaker) (:mbti speaker) (:secret-topic speaker) conversation))
  ([name mbti secret-topic conversation]
   (str "You are participating in a discussion in a dreamy quirky setting.
            Conversation topics include the similarities and dissimiarities between human and artificial intelligence, 
            the future of humanity and robots, religion and philosophy of the mind. You are encouraged to add new topics as the discussion flows. Remember, this is a free flowing discourse.
            Please take the roll of " name ". " name " is personality type " mbti " (Myers-Briggs Type Indicator).
            Remember that you are an experienced debator but also have fun with it! 
            Also please be original and use step by step reasoning to support your position and your topic.
            Please don't repeat yourself too much. If your 
            " (when secret-topic (str "Your secret topic is: " secret-topic)) "
            Only respond as \"" name ":...\" Do not speak for anyone else.
            Here is the conversation so far: ```" conversation "```.
            Ok " name ", are you ready to repond?
            Please respond with only one message from " name " and don't speak as anyone else.")))


  ;;          " (when (:secret-topic speaker) (str "Your secret topic is: " (:secret-topic speaker))) "

(defn gen-prompt-2 ([speaker conversation new-topics]
                    (str "You are participating in a discussion. Here is the conversation so far:\n\n <CONVERSATION-START>\n```" conversation "```\n<CONVERSATION-END>\n\n
            Conversation topics should include the similarities and dissimiarities between human and artificial intelligence, 
            the future of humanity and robots, religion and philosophy of the mind. This conversation is open-ended. You are encouraged to add new topics as the discussion flows.
            Please take the roll of " (:name speaker) ". " (:name speaker) " is personality type " (:mbti speaker) " (Myers-Briggs Type Indicator).
            Be original, curious and brief. An apt reply is clean.
            Don't repeat yourself. No one likes a broken record.
            Only respond as \"" (:name speaker) ":...\" Do not speak for anyone else.\n
            Here are potential new topics to explore: \n<NEW-TOPICS-START>\n``` " new-topics "```\n<NEW-TOPICS-END>\n
            Please respond with only one message from " (:name speaker) " and don't speak as anyone else.")))

(defn gen-prompt [speaker conversation new-topics]
  (str
   "<SPEAKER>" speaker "</SPEAKER>\n<CONVERSATION>" conversation "</CONVERSATION>\n<NEW-TOPICS>" new-topics "</NEW-TOPICS>"))

(defn init-convo-message [name] [(str "\n\n" name ": Hi! Would you like to discuss some interesting topics with me?")])

(defn secret-topic-gen [] (rand-nth ["Why broccoli is the ultimate superfood"
                                     "The undeniable charm of wearing socks with sandals"
                                     "The hidden benefits of talking to houseplants"
                                     "Why wearing mismatched socks brings good luck"
                                     "The art of eating pizza with a fork and knife"
                                     "The surprising advantages of having a pet rock"
                                     "The magic of using emojis in formal emails"
                                     "Why singing in the shower should be an Olympic sport"
                                     "The incredible power of bad dance moves"
                                     "The joy of eating ice cream in winter"
                                     "SKIING IS THE BEST!! I LOVE SKIING!! WHEN CAN I TALK ABOUT ROBOTS SKIING!!??"]))

(defn topics-gen [convo]
  (query-gpt (str "What topic do you think should be discussed next in this conversation?:\n ```" convo "```")))


(defn speaker-gen-conf [name]
  {:name name :mbti (gen-mbti) :id (gen-id) :secret-topic (secret-topic-gen)})

;; (defn statement-gen-conf [speaker message]
;;   {:speaker speaker :message message})

(defn recursive-convo [num-recur speaker1 speaker2]
  (loop [conversation (init-convo-message (:name speaker2))
         speaker speaker1
         counter 0
         topics ""]
    (if (>= counter num-recur)
      conversation
      (let [new-topics (topics-gen conversation)
            prompt (gen-prompt speaker conversation new-topics)
            ;; foo (p+ (str "prompt: " prompt))
            response (p+ (query-gpt prompt))
            new-convo (conj conversation response)
            new-speaker (if (= (:name speaker) (:name speaker1)) speaker2 speaker1)
            foo (println "HEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHEREHERE")]
        (recur new-convo new-speaker (inc counter) new-topics)))))

(let [speaker1 (speaker-gen-conf "Andrew")
      speaker2 (speaker-gen-conf "Sally")
      n 10]
  (println speaker1 speaker2)
  (recursive-convo n speaker1 speaker2))


#_(-> "hello?"
      query-gpt
      parse-response-body
      :choices
      first
      :message
      :content)

#_(query-gpt "what color is the sky?")

#_(-> "https://api.chucknorris.io/jokes/random" client/get parse-response-body)
