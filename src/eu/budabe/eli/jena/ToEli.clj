(ns eu.budabe.eli.jena.ToEli
  (:import [com.hp.hpl.jena.sparql.function FunctionBase1])
  (:import [com.hp.hpl.jena.sparql.expr NodeValue])
  (:import [com.hp.hpl.jena.sparql.engine.binding Binding])
  (:import [com.hp.hpl.jena.sparql.expr ExprList])
  (:import [com.hp.hpl.jena.sparql.function FunctionEnv])
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

(defn- ^NodeValue -exec[^ToEli this ^NodeValue nv1] ;seems never to be called
  ;(com.hp.hpl.jena.sparql.expr.NodeValue/makeString "http://www.gogole.com"))
  (com.hp.hpl.jena.sparql.expr.NodeValue/makeString (eli/eli4psi (.asString nv1))))

(defn- ^NodeValue -exec[^ToEli this ^Binding binding ^ExprList args ^String uri ^FunctionEnv env]
  ;;(com.hp.hpl.jena.sparql.expr.NodeValue/makeString "http://www.gogole.com"))
  (let
      ;for the rather byzantine interface of exec cf. http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/sparql/expr/ExprVar.html
      [first-arg (.asString (.eval (.getExprVar (first args)) binding env))]
    (com.hp.hpl.jena.sparql.expr.NodeValue/makeString (eli/eli4psi first-arg))))

