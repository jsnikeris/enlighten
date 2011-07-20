;; Considerations:
;;   - the edit-url for an entry is also its permalink
;;   - does not validate posted atom entries

(ns enlighten.core
  (:use [compojure.core :only (defroutes GET POST)]
        [enlighten.atom]
        [enlighten.model])
  (:require [compojure.route :as route]
            [net.cgrand.enlive-html :as e]
            [ring.util.response :as resp]
            [ring.adapter.jetty :as jetty]))

(e/deftemplate main "templates/main.html" []
  [[:link (e/attr= :rel "service.post")]] (e/set-attr :href *post-url*))

(defn post-response [entry]
  (let [location (select-text [:link (e/attr= :rel "edit")] entry)]
    (-> entry str-entry resp/response
        (resp/status 201)
        (resp/content-type *atom-type*)
        (resp/header "Location" location))))

(defn handle-post [body]
  (let [entry (-> body e/xml-resource normalize-entry populate-entry)]
    (if (save-entry entry)
      (post-response entry)
      "Error saving entry")))           ;TODO: return error status code

(defroutes routes
  (GET "/" [] (apply str (main)))
  (POST (str *post-url*) {body :body} (handle-post body))
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
