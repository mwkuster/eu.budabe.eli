(defproject eu.budabe.eli "1.3.0-SNAPSHOT"
  :description "A Clojure library designed to build prototypical European Legislation Identifiers (ELIs) based on Cellar data"
  :url "https://github.com/mwkuster/eu.budabe.eli"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2120"]
                 [clj-http "0.7.8"]
                 [cheshire "5.2.0"]
                 [ring/ring-core "1.2.1"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.4"]
                 [com.taoensso/timbre "2.7.1"]
                 [seabass "2.1.1"]
                 [uritemplate-clj "1.0.0"]
                 [domina "1.0.2"]
                 [cljs-ajax "0.2.3"]
                 [com.cemerick/url "0.1.0"]]
  :main eu.budabe.eli ;mainly for performance measures
  :aot [eu.budabe.eli.jena.ToEli eu.budabe.eli.jena.ToRT]
  :plugins [[lein-ring "0.8.8"]
            [lein-cljsbuild ​"1.0.1"​]]
  :ring {:handler eu.budabe.eli.routing/app}
  :cljsbuild
  {:builds	
   [{:source-paths [​"src-cljs"​]
     :compiler	{:optimizations :whitespace
                 :pretty-print true
                 :output-dir "public"
                 :output-to ​"public/eli.js"​}}]})
