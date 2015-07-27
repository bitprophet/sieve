(defproject sieve "0.1.0-SNAPSHOT"
  ;; Core info
  :description "RSS filtration system"
  :url "https://github.com/bitprophet/sieve"
  :license {:name "BSD 2-Clause License"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]
                 [ring "1.4.0"]]

  ;; Build-time options
  :main ^:skip-aot sieve.core

  ;; Lein plugins & their config
  :plugins [[lein-ring "0.8.7"]]

  ;; "Prod" lein server invokable via 'lein ring server-headless'
  :ring {:handler sieve.core/handler :port 8091}

  ;; Personal REPL development setup
  :repl-options {:init (do
    ;; Spin up a dev jetty server
    (require '[ring.adapter.jetty :refer [run-jetty]])
    (defonce server (run-jetty #'handler {:port 8090 :join? false}))
    (.start server))})
