(ns blog.core
  (:use [compojure.core :only (defroutes GET POST)]
        [net.cgrand.enlive-html :only (deftemplate defsnippet)])
  (:require [compojure.route :as route]
            [net.cgrand.enlive-html :as html]
            [ring.adapter.jetty :as jetty]))

(deftemplate main "templates/main.html" [])

(defroutes routes
  (GET "/" [] (apply str (main)))
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
;;   (jetty/run-jetty #'app {:port 8080 :join? false}))
