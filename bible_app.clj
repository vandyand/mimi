(ns mimi.bible-app
  (:require [clj-http.client :as http]))

(defn get-verse [book chapter verse]
  (let [url (str "https://bible-api.com/" book "+" chapter ":" verse)
        response (http/get url {:as :json})]
    (:text (:body response))))

(defn -main [& args]
  (println (get-verse "genesis" 1 1)))

(-main)