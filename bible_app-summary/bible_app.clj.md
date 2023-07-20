### Summary

This file is written in Clojure and contains code for a Bible app. It has two functions: `get-chapter` and `-main`. The `get-chapter` function takes a book and chapter as input, constructs a URL using the bible-api.com service, and sends an HTTP GET request to retrieve the verses of that chapter. The response is then parsed as JSON and the verses are extracted. The `-main` function simply calls `get-chapter` with the book "genesis" and chapter 1, and prints the result to the console.

```clojure
(ns mimi.bible_app.bible-app
  (:require [clj-http.client :as http]))

(defn get-chapter [book chapter]
  (let [url (str "https://bible-api.com/" book "+" chapter)
        response (http/get url {:as :json})]
    (:verses (:body response))))

(defn -main [& args]
  (println (get-chapter "genesis" 1)))

(-main)
```