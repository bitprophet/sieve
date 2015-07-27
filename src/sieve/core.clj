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

(def xml-stylesheet-line "<?xml-stylesheet type=\"text/xsl\" href=\"http://magic.wizards.com/sites/all/themes/wiz_mtg/xml/rss.xsl\"?>")

(def parsed (xml/parse local-url))

; TODO:
; - html-escape first ('rss') tag attrs' values (they aren't counted as
;   'content') (maybe just update the selector for that line?)
; - re-insert nuked-on-parse stylesheet line somehow - must be done
;   post-render and pre-write, of course.
;     - output is line-oriented so can probs just split on newline -> insert
;     -> join on newline?


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
    ; Escape the xml:base URL, MTG's RSS feed has GET params.
    ; TODO: I guess ideally we'd apply this to ALL the <rss> tag's attrs? Or even
    ; just any attr anywhere that's a string? Meh.
    [:rss] #(update-in % [:attrs :xml:base] escape-html)
    ; Remove blacklisted item tags (based on URL component)
    [:item] nuke-if-blacklisted
    ; Escape all description tags since they tend to contain raw HTML and
    ; xml/parse unescaped them for us.
    #{[:description] [:link]} #(update-in % [:content 0] escape-html)))

(defn render [document]
  ; Result of parsing/transforming is a one-tuple, gotta unpack it for emit to
  ; be happy.
  (with-out-str (xml/emit (first document))))

(defn insert-stylesheet [rendered]
  ; For whatever reason, xml/parse preserves <?xml> but NOT <?xml-stylesheet>.
  (let [lines (string/split-lines rendered)]
    (string/join "\n" (apply conj
                             (subvec lines 0 1)
                             xml-stylesheet-line
                             (subvec lines 1)))))


(defn -main []
  (spit "zomg.xml" (insert-stylesheet (render (TRANSFORM parsed)))))
