(ns mimi.idontknow.pit+
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [<! >! go chan]]))

(defn parse-response-body [response]
  (json/read-str (response :body) :key-fn keyword))

(defn post-gpt [query]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body {:model "gpt-3.5-turbo"
              :messages [{:role "user" :content query}]
              :temperature 1.7
              :max_tokens 1024 }
        json-body (json/write-str body)
        byte-array-body (.getBytes json-body "UTF-8")]
    (http/post url {:body byte-array-body
                    :headers headers})))

(defn p+ [x]
  x
  (do
    (println x)
    x)
  )

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

;;;;;;;;;;;;;;;;;;;; DEFINE OPERABLES ;;;;;;;;;;;;;;;
(defn get-operable [task, test]
  {:task task :test test}) ; mode is like "compare two" or "test one"
  ;{:task "that which we are to do" :test "that by which we steer/direct (think 'which summary is better?')"})
  ;{:task "Summarize Biblical Texts" :test "which of these options evidences the ability to X more adequately? (where X is the task)"})

(def bible-operable (get-operable "Summarize (the provided) Biblical Texts" "Which summary is more interesting and intriguing?"))

(def file-x-operable (get-operable "Make a file that does X" {:mode "compare two" :question "Which file does X better?"}))

(def file-bash-operable (get-operable "Make a bash script that echoes 'Hello World!'" "Which bash script echoes 'Hello World!' most adequately?"))


;;;;;;;;;;;;;;;;;;;; GENERATE AGENTS ;;;;;;;;;;;;;

;; (def task-desc "Summarize Biblical Texts")

(defn gen-agent-body []
  (query-gpt (str "Please generate an agent prompt for me. The agent's mission will be to " (:task file-bash-operable) ". No ai voice. No ai description. No ai summary.")))

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
  (gen-agent (query-gpt (str "Please improve this agent prompt so that it is better able to " (:task file-bash-operable) ": "
                             (:body agent)
                             "No ai voice. No ai description. No ai summary."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;; PERFORM ACTION - CREATE TRIAL DATA ;;;;;;;;;;;;;;;;;;;;;;;;;

(defn perform [task-info agent]
  (query-gpt (str "Your mission is to: " (:task file-bash-operable) ". Here is the info: " task-info ". " (:body agent))))

#_(summarize (get-chapter "genesis" 1) (gen-agent))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; GET ACTIONABLE TASK INFO (generally via an api call);;;;;;;;;;;;;;;;

(def bible-books
  ["Genesis" "Exodus" "Leviticus" "Numbers" "Deuteronomy" "Joshua" "Judges" "Ruth" "1 Samuel" "2 Samuel"
   "1 Kings" "2 Kings" "1 Chronicles" "2 Chronicles" "Ezra" "Nehemiah" "Esther" "Job" "Psalms" "Proverbs"
   "Ecclesiastes" "Song of Solomon" "Isaiah" "Jeremiah" "Lamentations" "Ezekiel" "Daniel" "Hosea" "Joel"
   "Amos" "Obadiah" "Jonah" "Micah" "Nahum" "Habakkuk" "Zephaniah" "Haggai" "Zechariah" "Malachi"
   "Matthew" "Mark" "Luke" "John" "Acts" "Romans" "1 Corinthians" "2 Corinthians" "Galatians" "Ephesians"
   "Philippians" "Colossians" "1 Thessalonians" "2 Thessalonians" "1 Timothy" "2 Timothy" "Titus" "Philemon"
   "Hebrews" "James" "1 Peter" "2 Peter" "1 John" "2 John" "3 John" "Jude" "Revelation"])


(defn get-task-info
  ([ref] (get-task-info (:book ref) (:chapter ref) (:verse ref)))
  ([book chapter verse]
   (let [url (str "https://bible-api.com/" book "+" chapter ":" verse)
         response (http/get url {:as :json})]
     (:verses (:body response)))))

(defn get-next-task-info
  ([ref] (get-next-task-info (:book ref) (:chapter ref) (:verse ref)))
  ([book chapter verse]
   (let [current-book-index (.indexOf bible-books book)
         next-verse (try
                      (get-task-info book chapter (inc verse))
                      (catch Exception e nil))]
     (if next-verse
       {:book book :chapter chapter :verse (inc verse)}
       (let [next-chapter (try
                            (get-task-info book (inc chapter) 1)
                            (catch Exception e nil))]
         (if next-chapter
           {:book book :chapter (inc chapter) :verse 1}
           (if (< current-book-index (dec (count bible-books)))
             {:book (nth bible-books (inc current-book-index)) :chapter 1 :verse 1}
             {:error "Reached the end of the Bible"})))))))


(defn battle [meat op1 op2]
  (query-gpt (str
              (:test file-bash-operable)
              meat 
              "OPTION1: " op1 "\n"
              "OPTION2: " op2 "\n"
              "No ai voice. No ai description. No ai summary. No ai rationale. Please respond ONLY with 'OPTION1' or 'OPTION2'")))

(let [agents (gen-agents (gen-agent-bodies 3))
        foo (spit "agents.edn" agents)
        foo (p+ agents)
        first-ref {:book "genesis" :chapter 1 :verse 1}]
    (loop [i 0 ref first-ref agents agents]
      (let [foo (println "--------------- CHALLENGE #" i ", verse: " ref " -------------------------------")
            shuf-agents (shuffle agents)
            agent1 (first shuf-agents) ;; later - enforce unique agents
            agent2 (second shuf-agents)
            task-info (get-task-info ref)
            foo (p+ {:agent1 agent1})
            foo (p+ {:agent2 agent2})
            summary1 (perform task-info agent1)
            foo (p+ {:summary1 summary1})
            summary2 (perform task-info agent2)
            foo (p+ {:summary2 summary2})
            who-won? (battle task-info summary1 summary2)
            winner (if (clojure.string/includes? who-won? "OPTION1") agent1 agent2)
            loser (if (clojure.string/includes? who-won? "OPTION1") agent2 agent1)
            foo (p+ {:who-won? who-won? :id (:id winner)})
            child (mutate-agent winner)
            foo (spit "agents.edn" child :append true)
        ;;   foo (p+ {:child child})
            new-agents+ (vec (conj agents child))
            new-agents (vec (remove #(= (:id %) (:id loser)) new-agents+))
          ;; foo (p+ {:new-agents new-agents})
        ;;   foo (println {:new-agents new-agents})
        ;;   foo (p+ {:len-new-agents (count new-agents)})
            next-verse-ref (get-next-task-info ref)
        ;;   foo (p+ {:next-verse-ref next-verse-ref})
            ]
        (if (or (> i 10000) (contains? next-verse-ref :error))
          new-agents
          (recur (inc i) next-verse-ref new-agents)))))





