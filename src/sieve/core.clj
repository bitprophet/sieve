(ns sieve.core
  (:require [clojure.string :as string]
            [clojure.xml :as xml]
            [net.cgrand.enlive-html :as enlive]
            [ring.util.response [:refer charset]]
            [hiccup.util :refer [escape-html]]))


(def feed-url "http://magic.wizards.com/rss/rss.xml?tags=Daily%20MTG&lang=en")

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
  {:body (process feed-url)})
