(ns eu.budabe.eli
  (:use eu.budabe.eli.jena.access)
  (:require [seabass.core :as rdf])
  (:require [clj-http.client :as client])
  (:require [cheshire.core :as json])
  (:require [taoensso.timbre :as timbre
            :refer (trace debug info warn error fatal spy)])
  (:require [uritemplate-clj.core :as templ])
  (:use eu.budabe.eli.rdfa)
  (:require [clojure.set :as cs])
  (:import [java.net URLEncoder URLDecoder])
  (:import [java.net URL]))

(timbre/set-config! [:appenders :standard-out :enabled?] false)
(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] "logs/eu.budabe.eli.log")
(timbre/set-level! :info)

(def TYPEDOC_RT_MAPPING (json/parse-string (slurp "resources/typedoc_rt_mapping.json")))

(def RT_TYPEDOC_MAPPING (cs/map-invert TYPEDOC_RT_MAPPING))

(def TYPEDOC_CB_MAPPING (json/parse-string (slurp "resources/typedoc_cb_mapping.json")))

(def DOMAIN "eli.budabe.eu")

(def oj-query "PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>\nSELECT DISTINCT ?uri\nWHERE {?work_uri cdm:resource_legal_published_in_official-journal ?uri}")

(def expression-query "PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>\nSELECT DISTINCT ?uri\nWHERE {?work_uri cdm:work_has_expression ?uri}")

(def manifestation-query "PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>\nSELECT DISTINCT ?uri WHERE {?u cdm:expression_manifested_by_manifestation ?uri}")

(defn in-cache? [^String cellar-psi]
  (let
      [find-graph-query (URLEncoder/encode (clojure.string/replace "PREFIX owl: <http://www.w3.org/2002/07/owl#>
SELECT DISTINCT ?gra WHERE { GRAPH ?gra{ {<#{cellar_psi}> owl:sameAs ?o} UNION {?s owl:sameAs <#{cellar_psi}>}} } LIMIT 1" "#{cellar_psi}" cellar-psi))
       query-url (str "http://localhost:3030/eli/query?query=" find-graph-query "&output=json")
       query-result (json/parse-string (:body (client/get query-url)))
       binding (first (get (get query-result "results")  "bindings"))]
    (if binding
      (get (get binding "gra") "value")
      nil)))
    

(defn fetch-work-remotely
  "Fetch a work-level Cellar PSI either remotely"
  [^String cellar-psi]
  (let
      [work-model (build-model (list cellar-psi))
       oj-model (build-model (map :uri (:data (rdf/bounce oj-query work-model))))
       expression-model (build-model (map :uri (:data (rdf/bounce expression-query work-model))))
       manifestation-model (build-model (map :uri (:data (rdf/bounce manifestation-query expression-model))))]
    (rdf/build work-model oj-model expression-model manifestation-model)))

(defn fetch-work-from-cache 
  "Fetch a work-level Cellar PSI from cache"
  [^String graph-uri]
  (let
      [cache-uri (str "http://localhost:3030/eli/data?graph=" (URLEncoder/encode graph-uri))]
    (rdf/build [cache-uri "TTL"])))

(defn add-to-cache
  "Add a model to the cache"
  [^String cellar-psi model]
  (let
      [cache-uri (str "http://localhost:3030/eli/data?graph=" (URLEncoder/encode cellar-psi))]
    (client/put cache-uri  {:body (model-to-string model) :headers {"Content-Type" "application/rdf+xml"}})
    cache-uri))
      
(defn fetch-work 
  "Fetch a work-level Cellar PSI and all its metadata either from a local cache or remotely"
  [^String cellar-psi]
  (let
      [in-cache (in-cache? cellar-psi)
       model (if in-cache
               (fetch-work-from-cache in-cache)
               (fetch-work-remotely cellar-psi))]
    (if (not in-cache)
      (add-to-cache cellar-psi model))
    model))

(def eli-query "PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?number ?typedoc ?is_corrigendum  ?pub_date ?lang
WHERE {
  GRAPH <#{GRAPH-URI}> {
  ?manif cdm:manifestation_official-journal_part_information_number ?number .
  ?manif cdm:manifestation_official-journal_part_typedoc_printer  ?typedoc .
  ?manif cdm:manifestation_official-journal_part_is_corrigendum_printer ?is_corrigendum .
  ?expr cdm:expression_uses_language ?lang .
  OPTIONAL {
  ?work cdm:resource_legal_published_in_official-journal ?oj .
  ?oj  cdm:publication_general_date_publication ?pub_date .
}
  FILTER(strlen(?number) > 0) # && strlen(?typedoc) > 0 && strlen(?is_corrigendum) > 0)
  }
  ORDER BY ?lang
} LIMIT 1") 

(def lang-query "PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>
SELECT DISTINCT ?lang_code
WHERE {
GRAPH <#{GRAPH-URI}> {
  ?expr cdm:expression_uses_language  ?lang .
  BIND(lcase(replace(str(?lang), '.*/([A-Z]{3})', '$1')) AS ?lang_code)
}
}
ORDER BY ?lang_code")


(defn parse-number
 "Parse numbers of type 2010/24 (EU). This implementation is a bit of a hack since we don't know in advance the sequence and there are ambiguous situations"
 [^String cellar-psi number]
 (let ;case year / number
     [year-number-parse (first (re-seq #"(19\d{2}|20\d{2})/(\d+)" number))]
   (if year-number-parse
     (list (nth year-number-parse 1) (nth year-number-parse 2))
     (let ;case number / year
         [number-year-parse (first (re-seq #"(\d+)/(19\d{2}|20\d{2})" number))]
       (if number-year-parse
         (list (nth number-year-parse 2) (nth number-year-parse 1))
         (do
           (info "parse-number: could not parse number " number " for psi " cellar-psi)
           (list "NO_YEAR" "NO_NUMBER")))))))

(defn get-graph-uri
  "Get the Graph URI of an object in the cache (if existing)"
  [^String cellar-psi]
  (let
      [graph-uri (in-cache? cellar-psi)]
    (if graph-uri
      graph-uri
      (do
        (fetch-work cellar-psi)
        (in-cache? cellar-psi)))))

(defn get-content
  "Get a brute content of a psi and return it to the sender (typically a binary datastream)"
  [^String cellar-psi]
  (let
      [uri (URLDecoder/decode cellar-psi)]
    (if (.startsWith uri "http://publications.europa.eu/resource/cellar")
      (client/get uri {:as :stream})
      {:status 404})))
    
(def elis (atom {}))

(defn eli4psi 
  "Transform where possible a Cellar PSI into an ELI"
   [^String cellar-psi]
   (info cellar-psi)
   (if (find @elis cellar-psi)
     (get @elis cellar-psi)
     (let
         [eli 
          (try
            (let
                [graph-uri (get-graph-uri cellar-psi)
                 query (clojure.string/replace eli-query "#{GRAPH-URI}" graph-uri)
                 query-url (str "http://localhost:3030/eli/query?query=" (URLEncoder/encode query) "&output=json")
                 query-result (json/parse-string (:body (client/get query-url)))
                 binding (first (get (get query-result "results")  "bindings"))]
              (info binding)
              (if binding
                (let
                    [number (get (get binding "number") "value")
                     [year natural-number] (parse-number cellar-psi number)
                     typedoc (get  TYPEDOC_RT_MAPPING (get (get binding "typedoc") "value"))
                     is-corrigendum  (get (get binding "is_corrigendum") "value")]
                  (info "is-corrigendum" is-corrigendum)
                  (if (= is-corrigendum "C")
                    (let
                        [lang  (get (get binding "lang") "value")
                         singlelang (clojure.string/lower-case (re-find #"[A-Z]{3}$" lang))
                         pub-date (get (get binding "pub_date") "value")]  
                      (do
                        (info "lang: " lang)
                        (info "pub-date: " pub-date)
                        ;for the sequence number find all the other corrigenda for the same number and the same pubdate, then order by language concerned
                        (templ/uritemplate "http://{domain}/eli/{typedoc}/{year}/{naturalnumber}/corr/{pubdate}/{seqnumber}/oj" 
                                             {"domain": DOMAIN, "typedoc" typedoc, "year" year, "naturalnumber" natural-number, "pubdate" pub-date, "seqnumber" singlelang})))
                    (templ/uritemplate "http://{domain}/eli/{typedoc}/{year}/{naturalnumber}/oj" {"domain" DOMAIN, "typedoc" typedoc, "year" year,  "naturalnumber" natural-number})))
                cellar-psi))
            (catch Exception e cellar-psi))]
       (info "eli" eli)
       (swap! elis assoc cellar-psi eli)
       eli)))



(defn eli-by-year [year]
  (let
      [query (clojure.string/replace (slurp "resources/by_year.rq") "2010" year)
       query-url (str "http://localhost:3030/eli/query?query=" (URLEncoder/encode query) "&output=json")
       query-result (json/parse-string (:body (client/get query-url)))
       binding (get (get query-result "results")  "bindings")]
    (map #(eli4psi (get (get %1 "gra") "value")) binding)))

(defn eli-by-typedoc [typedoc]
  (let
      [query (clojure.string/replace (slurp "resources/by_typedoc.rq") "dir_impl" (get RT_TYPEDOC_MAPPING typedoc))
       query-url (str "http://localhost:3030/eli/query?query=" (URLEncoder/encode query) "&output=json")
       query-result (json/parse-string (:body (client/get query-url)))
       binding (get (get query-result "results")  "bindings")]
    (map #(eli4psi (get (get %1 "gra") "value")) binding)))

(defn eli-by-typedoc-year [typedoc year]
  (let
      [by-year (set (eli-by-year year))
       by-typedoc (set (eli-by-typedoc typedoc))]
    ;this must be about the most inefficient way to implement this, I admit
    (clojure.set/intersection by-year by-typedoc)))

(defn eli-metadata
  "Return the ELI-encoded metadata for an object"
  [^String cellar-psi]
  (let
      [model (fetch-work cellar-psi)
       query (slurp "resources/eli_md.rq")]
    (rdf/pull query model)))

(defn get-psi-for-corrigendum [typedoc year natural-number pub-date lang]
  "Get the metadata of a corrigendum from the Cache using its ELI"
  (let
      [query (slurp "resources/find_corrigendum.rq")
       query-interpreted 
       (clojure.string/replace query #"~typedoc~|~year~|~natural-number~|~pub-date~|~lang~" 
                               {"~typedoc~" (get RT_TYPEDOC_MAPPING typedoc), "~year~" year, "~natural-number~" natural-number, 
                                "~pub-date~" pub-date, "~lang~" (clojure.string/upper-case lang)})
       query-url (str "http://localhost:3030/eli/query?query=" (URLEncoder/encode query-interpreted) "&output=json")
       query-result (json/parse-string (:body (client/get query-url)))]
    (info query-interpreted)
    (get (get (first (get (get  query-result "results") "bindings")) "gra") "value")))
                          
  
    
(defn -main [cellar-psi & args]
  (dotimes [n 100]
    (let
        [m (eli-metadata cellar-psi)]
      m)))
;(spit "/tmp/rdfa.html"  (build-rdfa m)))))