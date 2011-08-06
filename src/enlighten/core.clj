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
            [ring.adapter.jetty :as jetty]
            [enlighten.view :as view]))

(defn post-response [entry]
  (let [location (e/select-attrib entry [[:link (e/attr= :rel "edit")]] :href)]
    (-> entry e/emit* resp/response
        (resp/status 201)
        (resp/content-type *atom-type*)
        (resp/header "Location" location))))

(defn handle-post [body]
  (let [entry (-> body e/xml-resource normalize-entry populate-entry)]
    (if (save-entry entry)
      (post-response entry)
      "Error saving entry")))           ;TODO: return error status code

(defn handle-get [url-path accept]
  (when-let [entry (get-entry url-path)]
    (if (.contains accept *atom-type*)  ;TODO: proper accept header parsing
      (-> entry e/emit* resp/response (resp/content-type *atom-type*))
      (view/entry (e/select-text entry [:title])
                  (e/select-text entry [:published])
                  (e/select entry [:content :> :*])))))

(defroutes routes
  (GET "/" [] (apply str (view/main)))
  (POST (str *post-url*) {body :body} (handle-post body))
  (route/resources "/")
  (GET "/*" {{accept "accept"} :headers uri :uri} (handle-get uri accept)))

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
