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
              :temperature 1.0
              :max_tokens 50 }
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
(defn get-operable 
  ([task, test] (get-operable task, test, "query-gpt"))
  ([task, test, battle-cond]
  {:task task :test test :battle-cond battle-cond})) ; mode is like "compare two" or "test one"
  ;{:task "that which we are to do" :test "that by which we steer/direct (think 'which summary is better?')"})
  ;{:task "Summarize Biblical Texts" :test "which of these options evidences the ability to X more adequately? (where X is the task)"})

(def bible-operable (get-operable "Summarize (the provided) Biblical Texts" "Which summary is more interesting and intriguing?"))

(def file-x-operable (get-operable "Make a file that does X" {:mode "compare two" :question "Which file does X better?"}))

(def file-bash-operable (get-operable "Make a bash script that echoes 'Hello World!'" "Which bash script echoes 'Hello World!' most adequately?" "run-bash"))


;;;;;;;;;;;;;;;;;;;; GENERATE AGENTS ;;;;;;;;;;;;;

;; (def task-desc "Summarize Biblical Texts")

(defn gen-agent-body 
  ([] (gen-agent-body bible-operable))
  ([operable]
  (query-gpt (str "Please generate an agent prompt for me. The agent's mission will be to " (:task operable) ". No ai voice. No ai description. No ai summary.")))
)
(defn gen-agent
  ([] (gen-agent (gen-agent-body)))
  ([body]
   {:id (str "agent-" (.getTime (java.util.Date.)) "-" (rand-int 10000))
    :body body}))

(defn gen-agents [bodies]
  (vec (for [body bodies]
         (gen-agent body))))

(defn gen-agent-bodies [num-bodies operable]
  (vec (for [_ (range num-bodies)]
         (gen-agent-body operable))))

(defn mutate-agent [agent operable]
  (gen-agent (query-gpt (str "Please improve this agent prompt so that it is better able to " (:task operable) ": "
                             (:body agent)
                             "No ai voice. No ai description. No ai summary."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;; PERFORM ACTION - CREATE TRIAL DATA ;;;;;;;;;;;;;;;;;;;;;;;;;

(defn perform ([agent operable & task-info]
  (query-gpt (str "Your mission is to: " (:task operable) ". " (when task-info (str " Here is the info: " task-info ". ")) (:body agent))))
)
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


(defn battle [operable option1 option2 & additional-info]
  (let [gpt-res (query-gpt (str
                            (:test operable)
                            (when additional-info additional-info)
                            "OPTION1: " option1 "\n"
                            "OPTION2: " option2 "\n"
                            "No ai voice. No ai description. No ai summary. No ai rationale. Please respond ONLY with 'OPTION1' or 'OPTION2'"))
        ]
    gpt-res))

(defn battle-bible [option1 option2 meat]
  (battle bible-operable option1 option2 meat))

(defn battle-bash [cmd1 cmd2 & _]
  (let [
        cmd1-res (try (apply clojure.java.shell/sh (clojure.string/split cmd1 #" "))
                      (catch Exception e (str "caught exception: " (.getMessage e))))
        cmd2-res (try (apply clojure.java.shell/sh (clojure.string/split cmd2 #" "))
                      (catch Exception e (str "caught exception: " (.getMessage e))))
        ]
    (battle file-bash-operable cmd1-res cmd2-res)))

(defn get-battle-fn [operable]
  (cond (= (:battle-cond operable) "query-gpt") battle-bible
        (= (:battle-cond operable) "run-bash") battle-bash))

(let [operable file-bash-operable
      foo (println "OPERABLE: " operable)
      agents (gen-agents (gen-agent-bodies 3 operable))
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
            performance1 (perform agent1 operable task-info)
            foo (p+ {:performance1 performance1})
            performance2 (perform agent2 operable task-info)
            foo (p+ {:performance2 performance2})
            battle-fn (get-battle-fn operable)
            foo (p+ {:battle-fn battle-fn})
            who-won? (battle-fn performance1 performance2 task-info)
            winner (if (clojure.string/includes? who-won? "OPTION1") agent1 agent2)
            loser (if (clojure.string/includes? who-won? "OPTION1") agent2 agent1)
            foo (p+ {:winner-id (:id winner)})
            child (mutate-agent winner operable)
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





