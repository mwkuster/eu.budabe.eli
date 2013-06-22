(ns eu.budabe.eli.jena.access
  (:require [seabass.core :as rdf])
  (:require [clj-http.client :as client])
  (:require [taoensso.timbre :as timbre
             :refer (trace debug info warn error fatal spy)])
  (:import [com.hp.hpl.jena.rdf.model Model ModelFactory])
  (:import [com.hp.hpl.jena.reasoner.rulesys GenericRuleReasonerFactory Rule])
  (:import [com.hp.hpl.jena.vocabulary ReasonerVocabulary])
  (:import [java.io File OutputStreamWriter FileOutputStream]))

(timbre/set-config! [:appenders :standard-out :enabled?] false)
(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:appenders :spit :async?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] "logs/eu.budabe.eli.log")
(timbre/set-level! :info)

(defn save-model [model filename]
  (.write model (OutputStreamWriter. (FileOutputStream. filename) "UTF-8") "RDF/XML"))

(defn model-to-string [model]
  (let
      [sw (java.io.StringWriter.)]
    (.write model sw "RDF/XML")
    (.toString sw)))

(defn build-model [urls]
  "Build a model based on a number of URLs, heavily inspired by https://github.com/ryankohl/seabass/blob/master/src/seabass/impl.clj"
  (info urls)
  (let 
      [core (ModelFactory/createDefaultModel)
       model (ModelFactory/createDefaultModel)
       config (.addProperty (.createResource core)
                            ReasonerVocabulary/PROPruleMode
                            "hybrid")
       reasoner (.create (GenericRuleReasonerFactory/theInstance) config)]
    (doseq [url urls]
      (.add core (.read model 
                        (java.io.StringReader. 
                         (:body (client/get url {:headers {"Accept" "application/rdf+xml"}}))) "" "RDF/XML")))
    (ModelFactory/createInfModel reasoner core)))       

(defn build-model-from-string [str]
  "Build a model serialized as a string, heavily inspired by https://github.com/ryankohl/seabass/blob/master/src/seabass/impl.clj"
  (let 
      [core (ModelFactory/createDefaultModel)
       model (ModelFactory/createDefaultModel)
       config (.addProperty (.createResource core)
                            ReasonerVocabulary/PROPruleMode
                            "hybrid")
       reasoner (.create (GenericRuleReasonerFactory/theInstance) config)]
    (.add core (.read model 
                      (java.io.StringReader. str) "" "RDF/XML"))
    (ModelFactory/createInfModel reasoner core)))
