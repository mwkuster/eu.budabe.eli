(ns eu.budabe.eli.jena.ToEli
  (:import [com.hp.hpl.jena.sparql.function FunctionBase1])
  (:import [com.hp.hpl.jena.sparql.expr NodeValue])
  (:require [eu.budabe.eli :as eli])
  (:gen-class 
   :name eu.budabe.eli.jena.ToEli
   :init init
   :extends com.hp.hpl.jena.sparql.function.FunctionBase1
   :constructors {[][]}
   :init init))

(import 'eu.budabe.eli.jena.ToEli)

(defn- -init
  ([]
     [[]]))

(defn- ^NodeValue -exec[this ^NodeValue nv1] ;seems never to be called
  (com.hp.hpl.jena.sparql.expr.NodeValue/makeString (eli/eli4psi (.asString nv1))))

(defn- ^NodeValue -exec[this binding args uri env]
  (let
      ;for the rather byzantine interface of exec cf. file://localhost/Users/marcwilhelmkuster/Dropbox/bin/apache-jena-2.10.0/javadoc-arq/index.html, class ExprVar
      [first-arg (.asString (.eval (.getExprVar (first args)) binding env))]
    (com.hp.hpl.jena.sparql.expr.NodeValue/makeString (eli/eli4psi first-arg))))

