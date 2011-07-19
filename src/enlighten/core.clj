;; Considerations:
;;   - the edit-url for an entry is also its permalink
;;   - does not validate posted atom entries

(ns enlighten.core
  (:use [compojure.core :only (defroutes GET POST)]
        [enlighten.atom])
  (:require [compojure.route :as route]
            [net.cgrand.enlive-html :as e]
            [ring.adapter.jetty :as jetty]))

;; TODO: properties file?
(def *entry-dir* "/home/joe/Documents/Blog/Entries/")

(e/deftemplate main "templates/main.html" []
  [[:link (e/attr= :rel "service.post")]] (e/set-attr :href *post-url*))

(defn post-response [entry]
  (apply str (e/emit* entry)))

(defn handle-post [body]
  (let [entry (-> body e/xml-resource normalize-entry populate-entry)
        filename (make-filename entry)]
    (spit (str *entry-dir* filename ".xml")
          (apply str (e/emit* entry)))
    (post-response entry)))

(defroutes routes
  (GET "/" [] (apply str (main)))
  (POST *post-url* {body :body} (handle-post body))
  (route/resources "/"))

(defn wrap-charset [handler charset]
  (fn [request]
    (if-let [response (handler request)]
      (if-let [content-type (get-in response [:headers "Content-Type"])]
        (if (.contains content-type "charset")
          response
          (assoc-in response
            [:headers "Content-Type"]
            (str content-type "; charset=" charset)))
        response))))

(def app
  (-> routes
      (wrap-charset "utf-8")))

;; (defonce *server*
;;   ;; #' (var) allows rebinding of 'app' to be reflected immediately
;;   (jetty/run-jetty #'app {:port 3000 :join? false}))
