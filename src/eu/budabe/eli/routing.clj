(ns eu.budabe.eli.routing
  (:use eu.budabe.eli)
  (:use compojure.core)
  (:require [clj-http.client :as client])
  (:require [cheshire.core :as json])
  (:use eu.budabe.eli.rdfa)
  (:use eu.budabe.eli.jena.access)
  (:require [compojure.route :as route])
  (:require [compojure.handler :as handler])
  (:require
        [ring.util.response :as resp])
  (:require [ring.adapter.jetty :as jetty]) 
  (:require [taoensso.timbre :as timbre
            :refer (trace debug info warn error fatal spy)]))

(timbre/set-config! [:appenders :standard-out :enabled?] false)
(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] "logs/eu.budabe.eli.log")
(timbre/set-level! :info)

(defn parse-int [s]
  (Integer. (re-find #"[0-9]*" s)))
  
(defroutes app-routes
  (GET "/eli4psi" []
       (resp/redirect "/psi2eli.html"))

  (GET "/eli/:typedoc/:year/:natural_number/corr/:lang/:pub-date/oj" [typedoc year natural_number lang pub-date]
       (info typedoc year natural_number lang pub-date)
       (let
           [psi (get-psi-for-corrigendum typedoc year natural_number pub-date lang)]
         (println psi)
         (if psi
           (build-rdfa (eli-metadata psi))
           (route/not-found (format "<h1>Document for ELI %s not found</h1>" typedoc)))))


  (GET "/eli/:typedoc/:year/:natural_number/oj" [typedoc year natural_number]
       (try
         (let
             [sector 
              (case typedoc
                ("dir" "dir_impl" "dir_del") "L"
                ("reg" "reg_impl" "reg_del") "R"
                ("dec" "dec_impl" "dec_del") "D"
                "rec" "H"
                nil)
              celex-psi (if sector
                          (format "http://publications.europa.eu/resource/celex/3%s%s%04d" year sector  (parse-int natural_number))
                          nil)]
           (info "celex-psi: " celex-psi)
           (if celex-psi
             (build-rdfa (eli-metadata celex-psi))
             (route/not-found (format "<h1>Document type %s not yet supported</h1>" typedoc))))
         (catch Exception e
           (error e)
           (route/not-found "<h1>Could not find the requested document</h1>"))))

  (GET "/eli/:typedoc/:year/:natural_number/oj/:lang" [typedoc year natural_number lang]
       (resp/redirect (format "/eli/%s/%s/%s/oj" typedoc year natural_number)))

  (GET "/eli/:typedoc/:year/:natural_number/oj/:lang/:form" [typedoc year natural_number lang form]
       (resp/redirect (format "/eli/%s/%s/%s/oj" typedoc year natural_number)))

  (GET ["/eli/:year", :year #"\d+"] [year]
       (html-result-list (eli-by-year year)))

  (GET ["/eli/:typedoc", :typedoc #"[a-z_-]+"] [typedoc]
       (html-result-list (eli-by-typedoc typedoc)))

  (GET ["/eli/:typedoc/:year", :typedoc #"[a-z_-]+", :year #"\d+"] [typedoc year]
       (html-result-list (eli-by-typedoc-year typedoc year)))

  (GET "/" []
       (resp/redirect "/psi2eli.html"))

  (GET "/eli" []
       (resp/redirect "/psi2eli.html"))

  (GET "/eli/" []
       (resp/redirect "/psi2eli.html"))

  (GET "/eli" []
       (resp/redirect "/psi2eli.html"))
 
  (GET "/eli4celex/:celex" [celex]
       (resp/redirect (str "/eli4psi/http%3A%2F%2Fpublications.europa.eu%2Fresource%2Fcelex%2F" celex)))

  (GET "/eli4id_jo/:oj" [oj]
       (resp/redirect (str "/eli4psi/http%3A%2F%2Fpublications.europa.eu%2Fresource%2Foj%2F" oj)))
  
  (GET ["/eli4psi/:psi", :psi #"[^/;?]+"] [psi] 
       (info "/eli4psi/:psi" psi)
       (json/generate-string (eli4psi psi)))

  (GET  ["/eli4psi/:psi/metadata", :psi #"[^/;?]+"] [psi] 
       (info "/eli4psi/:psi/metadata" psi)
       (try
         (build-rdfa (eli-metadata psi))
         (catch clojure.lang.ExceptionInfo e
           (error e)
           (route/not-found "<h1>#{psi} not found</h1>"))))

  (GET ["/content/:psi", :psi #"[^/;?]+"] [psi] 
       (let
           [response (get-content psi)]
          (if (= (:status response) 200)
           {:status 200, :headers {"Content-Type" 
                                    (get (:headers response) "content-type")}, :body (:body response)}
           (route/not-found "<h1>Content for #{psi} not found</h1>"))))

  (route/files "/" {:root "public"})

  (route/not-found 
   "<h1>Page not found</h1>"))

(defn wrap-prn-request [handler]
  (fn [request]
    (info "request:" request)
    (handler request)))

(def app
  (-> (handler/api app-routes)
      (wrap-prn-request)))

(defn increase-timeout [server]
  (doseq [connector (.getConnectors server)]
    (.setMaxIdleTime connector (* 2 60 1000))))

(jetty/run-jetty app {:port 3000 :configurator increase-timeout})
