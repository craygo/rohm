(defproject rohm "0.1.0-SNAPSHOT"
  :description ""
  :license {:name "Apache"
            :url "http://www.apache.org/licenses/"}

  :source-paths  ["src"]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2120"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.1.0-SNAPSHOT"]
                 ]

  :plugins [[lein-cljsbuild "1.0.0"]]

  :cljsbuild { 
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to "dev.js"
                :output-dir "out"
                :optimizations :none}}]})
