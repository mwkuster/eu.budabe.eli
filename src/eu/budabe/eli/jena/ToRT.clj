(ns eu.budabe.eli.jena.ToRT
  (:import [com.hp.hpl.jena.sparql.function FunctionBase1])
  (:import [com.hp.hpl.jena.sparql.expr NodeValue])
  (:import [com.hp.hpl.jena.sparql.engine.binding Binding])
  (:import [com.hp.hpl.jena.sparql.expr ExprList])
  (:import [com.hp.hpl.jena.sparql.function FunctionEnv])
  (:require [eu.budabe.eli :as eli])
  (:gen-class 
   :name eu.budabe.eli.jena.ToRT
   :init init
   :extends com.hp.hpl.jena.sparql.function.FunctionBase1
   :constructors {[][]}
   :init init))

(import 'eu.budabe.eli.jena.ToRT)

(defn- -init
  ([]
     [[]]))

(defn- ^NodeValue -exec[this ^NodeValue nv1] ;seems never to be called
  (com.hp.hpl.jena.sparql.expr.NodeValue/makeString 
     (str "http://publications.europa.eu/resource/authority/resource-type/" 
          (clojure.string/upper-case (get eli/TYPEDOC_RT_MAPPING (.asString nv1))))))

(defn- ^NodeValue -exec[this ^Binding binding ^ExprList args ^String uri ^FunctionEnv env]
  (let
      ;for the rather byzantine interface of exec cf. file://localhost/Users/marcwilhelmkuster/Dropbox/bin/apache-jena-2.10.0/javadoc-arq/index.html, class ExprVar
      [first-arg (.asString (.eval (.getExprVar (first args)) binding env))]
    (com.hp.hpl.jena.sparql.expr.NodeValue/makeString 
     (str "http://publications.europa.eu/resource/authority/resource-type/" 
          (clojure.string/upper-case (get eli/TYPEDOC_RT_MAPPING first-arg))))))

