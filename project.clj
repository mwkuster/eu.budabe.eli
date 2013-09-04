(defproject eu.budabe.eli "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.2"]
                 [cheshire "5.2.0"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1"]
                 [hiccup "1.0.3"]
                 [com.taoensso/timbre "2.1.2"]
                 [seabass "2.0"]
                 [uritemplate-clj "1.0.0"]]
  :main eu.budabe.eli ;mainly for performance measures
  :aot [eu.budabe.eli.jena.ToEli eu.budabe.eli.jena.ToRT]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler eu.budabe.eli.routing/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}})

