(ns sieve.core
  (:require [clojure.string :as string]
            [clojure.xml :as xml]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.util :refer [escape-html]]
            [ring.adapter.jetty :refer [run-jetty]]))


(def feed-url "http://magic.wizards.com/rss/rss.xml?tags=Daily%20MTG&lang=en")

(def blacklist #{"command-tower"
                 "daily-deck"
                 "organized-play"
                 "perilous-research"
                 "reconstructed"
                 "serious-fun"
                 "top-25"
                 "top-decks"
                 "week-was"})

(def xml-stylesheet-line "<?xml-stylesheet type=\"text/xsl\" href=\"http://magic.wizards.com/sites/all/themes/wiz_mtg/xml/rss.xsl\"?>")


(defn link [item]
  (-> item (enlive/select [:link]) first :content first))

(defn category [item]
  (-> item link (string/split #"/") (nth 6)))

(defn nuke-if-blacklisted [node]
  (if (contains? blacklist (category node))
    nil
    node))

(defn nuke-and-escape [document]
  (enlive/at
    document
    ; Escape the xml:base URL, MTG's RSS feed has GET params.
    ; TODO: I guess ideally we'd apply this to ALL the <rss> tag's attrs? Or even
    ; just any attr anywhere that's a string? Meh.
    [:rss] #(update-in % [:attrs :xml:base] escape-html)
    ; Remove blacklisted item tags (based on URL component)
    [:item] nuke-if-blacklisted
    ; Escape all remaining description tags since they tend to contain raw HTML
    ; and xml/parse unescaped them for us. Ditto links, which often have GET
    ; params.
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

(defn process [url]
  (-> url
    xml/parse
    nuke-and-escape
    render
    insert-stylesheet))


; Just serve up a processed backend request for now. Seems fast enough?
; TODO: if MTG backend goes back to previously observed slow behavior, may want
; to start caching/prefetching or something.
(defn handler [request]
  ; Must set headers for clients to deal correctly with things like 'advanced'
  ; typesetting characters (e.g. em-dashes).
  {:body    (process feed-url)
   :headers {"Content-Type" "application/xml; charset=utf-8"}})

; When run on Heroku, $PORT is used to tell the server what local port to run
; on; presumably their routing infrastructure sets that up dynamically and
; eventually hooks it up to port 80.
; Making it available via CLI arg is nice too I guess.
(defn -main [& [port]]
  (let [port (Integer. (or port (System/getenv "PORT") 5000))]
    (run-jetty #'handler {:port port :join? false})))
