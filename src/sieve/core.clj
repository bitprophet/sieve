(ns sieve.core
  (:require [clojure.string :as string]
            [org.httpkit.client :as http]
            [feedparser-clj.core :as rss]
            [hickory.core :as html]
            [puget.printer :refer [cprint]]))

(def archives (slurp "mtg.xml"))

(def document (-> archives html/parse html/as-hickory))

(def raw (-> document
           :content (nth 2)
           :content second
           :content first
           :content second
           :content (nth 10)
           :content (nth 6)
           :content first))

(def entry (-> raw html/parse html/as-hickory))

(def feed-url "http://magic.wizards.com/rss/rss.xml?tags=Daily%20MTG&lang=en")
(def local-url "http://localhost:8000/mtg.xml")

(def feed (rss/parse-feed local-url))

(def categories (map #(nth (string/split (:link %) #"/") 6) (:entries feed)))

(defn -main [lol]
  (case lol
    ;"rss" (cprint (rss/parse-feed archives))
    "hickory" (cprint (html/as-hickory (html/parse archives)))
    "hiccup" (cprint (html/as-hiccup (html/parse archives)))
    (println "eh?")))
