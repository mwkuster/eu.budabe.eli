(​ns​ eli	
  (:require [domina :refer [by-class by-id nodes sel attr append! set-html! set-text!]]	
            [domina.css :refer [sel]]
            [domina.events :as events]
            [domina.xpath :as xpath]
            [cemerick.url :refer [url url-encode]]
            [ajax.core :refer [GET]]))

(defn write-eli [response]
  (.log js/console (str response))
  (let
      [eli "ELI"
       encoded-psi "encoded-psi"]
  (set-html! (by-id "ELI") 
             (str "Your ELI is: <b><a href=\""  eli  "\">" eli  "</a></b><br/> See here the corresponding <a href=\"/eli4psi/" encoded-psi "/metadata\">RDFa-enriched metadata</a>  (might take a moment to load)"))))

(defn write-eli-error [response]
  (.log js/console (str response))
  (set-html! (by-id "ELI") 
             (str "<b>" response "</b>")))

(defn calculate-eli[event]
  "Have the ELI calculated by the backend based on the given production id"
  (.log js/console "calcualte-eli")
  (let
      [psi (.-value (by-id "psi"))
       psi-type (.-value (first (nodes (sel "input[name=psitype]:radio:checked"))))
       uricomponent (if (= psi-type "celex") "/celex/" "/oj/")
       psi-uri (str "http://publications.europa.eu/resource" uricomponent psi)
       encoded-psi (url-encode psi-uri)]
    (set-html! (by-id "ELI") (str "<b>Please wait for the search for identifier " psi-uri " to complete</b>"))
    (.log js/console psi)
    (.log js/console psi-type)
    (.log js/console uricomponent)
    (.log js/console (str "/eli4psi/" encoded-psi))
    (GET 
     (str "/eli4psi/" encoded-psi)
     {;:response-format :json
      :handler write-eli
      :error-handler write-eli-error})
    (set-html! (by-id "ELI") "<b>Done</b>"))
                                        ;Use prevent-default and stop-propagation to prevent the event from bubbling up 
                                        ;from the OK button to the form element and therefore causing 
                                        ;a reload for the page
                                        ;cf. http://stackoverflow.com/questions/18829660/how-to-handle-onsubmit-with-domina
  (do
    (events/prevent-default event)
    (events/stop-propagation event)))

(defn ^:export main []                                       
  (events/listen! 
   (by-id "okBtn")
   :click
   calculate-eli))
