(ns mimi.bible_app.pit
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            ))

(defn parse-response-body [response]
  (json/read-str (response :body) :key-fn keyword))

(defn post-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-3.5-turbo"
              :messages [{:role "user" :content query}]
              :temperature 0.7}
        json-body (json/write-str body)
        byte-array-body (.getBytes json-body "UTF-8")]
    (http/post url {:body byte-array-body
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
    ;;   p+
      ))

;;;;;;;;;;;;;;;;;;;; GENERATE AGENTS ;;;;;;;;;;;;;

(defn gen-agent-body []
  (query-gpt "Please generate a paragraph of random words for me. No ai voice. No ai description. No ai summary."))

(defn gen-agent
  ([] (gen-agent (gen-agent-body)))
  ([body]
   {:id (str "agent-" (.getTime (java.util.Date.)) "-" (rand-int 10000))
    :body body}))

(defn gen-agents [bodies]
  (vec (for [body bodies]
    (gen-agent body))))

(defn gen-agent-bodies [num-bodies]
  (vec (for [_ (range num-bodies)]
    (gen-agent-body))))

(defn mutate-agent [agent]
  (gen-agent (query-gpt (str "Please change a few words in this paragraph: " 
                             (:body agent) 
                             "No ai voice. No ai description. No ai summary."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;; SUMMARIZE BIBLE CONTENT ;;;;;;;;;;;;;;;;;;;;;;;;;

(defn summarize [verse agent]
  (query-gpt (str "please summarize this verse" verse (:body agent))))

#_(summarize (get-chapter "genesis" 1) (gen-agent))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; GET NEXT VERSE ;;;;;;;;;;;;;;;;

(def bible-books
  ["Genesis" "Exodus" "Leviticus" "Numbers" "Deuteronomy" "Joshua" "Judges" "Ruth" "1 Samuel" "2 Samuel"
   "1 Kings" "2 Kings" "1 Chronicles" "2 Chronicles" "Ezra" "Nehemiah" "Esther" "Job" "Psalms" "Proverbs"
   "Ecclesiastes" "Song of Solomon" "Isaiah" "Jeremiah" "Lamentations" "Ezekiel" "Daniel" "Hosea" "Joel"
   "Amos" "Obadiah" "Jonah" "Micah" "Nahum" "Habakkuk" "Zephaniah" "Haggai" "Zechariah" "Malachi"
   "Matthew" "Mark" "Luke" "John" "Acts" "Romans" "1 Corinthians" "2 Corinthians" "Galatians" "Ephesians"
   "Philippians" "Colossians" "1 Thessalonians" "2 Thessalonians" "1 Timothy" "2 Timothy" "Titus" "Philemon"
   "Hebrews" "James" "1 Peter" "2 Peter" "1 John" "2 John" "3 John" "Jude" "Revelation"])

(defn get-verse
  ([ref] (get-verse (:book ref) (:chapter ref) (:verse ref)))
  ([book chapter verse]
   (let [url (str "https://bible-api.com/" book "+" chapter ":" verse)
         response (http/get url {:as :json})]
     (:verses (:body response)))))

(defn get-next-verse-ref 
  ([ref] (get-next-verse-ref (:book ref) (:chapter ref) (:verse ref)))
  ([book chapter verse]
  (let [current-book-index (.indexOf bible-books book)
        next-verse (try
                     (get-verse book chapter (inc verse))
                     (catch Exception e nil))]
    (if next-verse
      {:book book :chapter chapter :verse (inc verse)}
      (let [next-chapter (try
                           (get-verse book (inc chapter) 1)
                           (catch Exception e nil))]
        (if next-chapter
          {:book book :chapter (inc chapter) :verse 1}
          (if (< current-book-index (dec (count bible-books)))
            {:book (nth bible-books (inc current-book-index)) :chapter 1 :verse 1}
            {:error "Reached the end of the Bible"})))))))

(defn battle [verse summary1 summary2]
  (query-gpt (str
              "which of these two summaries do you find most insightful and interesting?"
              " here is the summary verse: "
              verse
              " and here are the summaries: "
              "SUMMARY1: " summary1 "\n"
              "SUMMARY2: " summary2 "\n"
              "No ai voice. No ai description. No ai summary. Please respond with either 'SUMMARY1' or 'SUMMARY2'")))

(let [agents (gen-agents (gen-agent-bodies 10))
      foo (p+ agents)
      first-ref {:book "genesis" :chapter 1 :verse 1}]
  (loop [i 0 ref first-ref agent1 (rand-nth agents) agent2 (rand-nth agents)] ;;later - enforce unique agents
    (let [foo (p+ i)
          verse (get-verse ref)
          foo (p+ {:agent1 agent1})
          foo (p+ {:agent2 agent2})
          summary1 (summarize verse agent1)
          foo (p+ {:summary1 summary1})
          summary2 (summarize verse agent2)
          foo (p+ {:summary2 summary2})
          winner-response (battle verse summary1 summary2)
          foo (p+ {:winner-response winner-response})
          winner (if (clojure.string/includes? winner-response "SUMMARY1") agent1 agent2)
          loser (if (clojure.string/includes? winner-response "SUMMARY1") agent2 agent1)
          foo (p+ {:winner-id (:id winner) :winner winner})
          child (mutate-agent winner)
          foo (p+ {:child child})
          new-agents (conj (remove #(= (:id %) (:id loser)) agents) child)
          foo (p+ {:new-agents new-agents})
          next-verse-ref (get-next-verse-ref ref)
          foo (p+ {:next-verse-ref next-verse-ref})] 
    (if (or (> i 100) (contains? next-verse-ref :error))
      new-agents
      (recur (inc i) next-verse-ref (rand-nth new-agents) (rand-nth new-agents))
      )
  )))
