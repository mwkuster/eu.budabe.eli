(ns eu.budabe.eli.eli
  (:require [taoensso.timbre :as timbre
             :refer (trace debug info warn error fatal spy)])
  (:require [uritemplate-clj.core :as templ]))

(def HOST "eli.budabe.eu")

(defmulti build-eli 
  (fn[eli-components] (:type-eli eli-components)))

(defmethod build-eli "CorrigendumOJ" [eli-components]
  (templ/uritemplate "http://{host}/eli/{typedoc}/{year}/{naturalnumber}/corr/{pubdate}/{seqnumber}/oj" 
                     {"host" HOST, 
                      "typedoc" (:typedoc eli-components), 
                      "year" (:year eli-components), 
                      "naturalnumber" (:natural-number eli-components), 
                      "pubdate" (:pub-date eli-components), 
                      "seqnumber" (:seq-number eli-components)}))

(defmethod build-eli "ActOJ" [eli-components]
  (templ/uritemplate "http://{host}/eli/{typedoc}/{year}/{naturalnumber}/oj" 
                     {"host" HOST, 
                      "typedoc" (:typedoc eli-components), 
                      "year" (:year eli-components),  
                      "naturalnumber" (:natural-number eli-components)}))
