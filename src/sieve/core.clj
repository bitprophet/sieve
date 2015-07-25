(ns sieve.core
  (:require [clojure.string :as string]
            [org.httpkit.client :as http]
            [feedparser-clj.core :as rss]
            [hickory.core :as html]
            [puget.printer :refer [cprint]]))

(def feed-url "http://magic.wizards.com/rss/rss.xml?tags=Daily%20MTG&lang=en")
(def local-url "http://localhost:8000/mtg.xml")

(def feed (rss/parse-feed local-url))

(def categories (map #(nth (string/split (:link %) #"/") 6) (:entries feed)))

(defn category [entry]
  (nth (string/split (:link entry) #"/") 6))

(def blacklist #{"daily-deck"
                "top-decks"
                "week-was"
                "reconstructed"
                "command-tower"
                "perilous-research"
                "organized-play"
                "serious-fun"
                "top-25"})

(def blacklisted (filter #(contains? blacklist (category %)) (:entries feed)))
(def okay (filter #(not (contains? blacklist (category %))) (:entries feed)))

(defn -main []
  (println "blacklisted: " (count blacklisted))
  (println "okay: " (count okay)))
