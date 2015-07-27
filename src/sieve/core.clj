(ns sieve.core
  (:require [clojure.string :as string]
            [clojure.xml :as xml]
            [org.httpkit.client :as http]
            [net.cgrand.enlive-html :as enlive]
            [clojure.data.xml :as data-xml]
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

(def data-parsed (data-xml/parse-str (slurp "mtg.xml")))

(defn -main [] (-> data-parsed cprint))

; TODO:
; - take the xml/parse -> xml-zip tree
; - nuke items which don't match blacklist
; - for those, replace the article content with itself run thru escape-html
;   - can just copy/paste that instead of requiring all of hiccup maybe, shrug
; - render
; - re-insert nuked-on-parse stylesheet line somehow


(defn link [item]
  (-> item (enlive/select [:link]) first :content first))

(defn category [item]
  (-> item link (string/split #"/") (nth 6)))

; The RSS XML looks like so:
; - top level 'rss' tag (document)
;   - 'channel' tag (-> document :content first)
;     - 'title' tag (-> document :content first :content first)
;     - 'link' tag (-> document :contnet first :content second)
;     - 'description' tag  (-> document :content first :content (nth 2))
;     - 'language' tag  (-> document :contnet first :content (nth 3))
;     - rest of content-vector members of 'channel' are the 'item' tags (nth 4
;     through nth count)
;
; What we want is to return the input, sans the 'item' tags in the 'channel'
; content whose 'link' tags' URIs contain blacklisted categories.
;(def filtered-for-categories [document])
