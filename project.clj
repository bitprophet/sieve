(defproject sieve "0.1.0-SNAPSHOT"
  ;; Core info
  :description "RSS filtration system"
  :url "https://github.com/bitprophet/sieve"
  :license {:name "BSD 2-Clause License"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]
                 [ring "1.4.0"]
                 [com.taoensso/timbre "4.2.1"]]

  ;; Build-time options
  :main ^:skip-aot sieve.core

  ;; For deploying on Heroku
  :profiles {:uberjar {:aot :all}}
  :min-lein-version "2.0.0"
  :uberjar-name "sieve-standalone.jar")
