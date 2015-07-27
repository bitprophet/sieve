(ns sieve.core
  (:require [clojure.string :as string]
            [clojure.xml :as xml]
            [org.httpkit.client :as http]
            [net.cgrand.enlive-html :as enlive]
            [clojure.zip :as zip]
            [hiccup.util :refer [escape-html]]
            [puget.printer :refer [cprint]]))


(def feed-url "http://magic.wizards.com/rss/rss.xml?tags=Daily%20MTG&lang=en")
(def local-url "http://localhost:8000/mtg.xml")

(def blacklist #{"daily-deck"
                "top-decks"
                "week-was"
                "reconstructed"
                "command-tower"
                "perilous-research"
                "organized-play"
                "serious-fun"
                "top-25"})

(def parsed (xml/parse local-url))

; TODO:
; - (/) take the xml/parse -> xml-zip tree
; - (/) nuke items which don't match blacklist
; - (/) for those, replace the article content with itself run thru escape-html
;   - can just copy/paste that instead of requiring all of hiccup maybe, shrug
; - (/) ditto for all content actually, which should mostly just be the top
;   level link, and item links
; - render
; - re-insert nuked-on-parse stylesheet line somehow


(defn link [item]
  (-> item (enlive/select [:link]) first :content first))

(defn category [item]
  (-> item link (string/split #"/") (nth 6)))

(defn nuke-if-blacklisted [node]
  (if (contains? blacklist (category node))
    nil
    node))

(defn TRANSFORM [document]
  (enlive/at
    document
    [:item] nuke-if-blacklisted
    [:description] escape-html
    [:item :> :description] (fn [node] [(apply str (take 25 (:content node)))])))

(defn -main []
  
