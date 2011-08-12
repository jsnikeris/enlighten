;; Considerations:
;;   - the edit-url for an entry is also its permalink
;;   - does not validate posted atom entries

(ns enlighten.core
  (:use [compojure.core :only (defroutes GET POST)])
  (:require [clojure.contrib.condition :as cond]
            [clj-time.core :as time]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as e]
            [ring.util.response :as resp]
            [ring.adapter.jetty :as jetty]
            (enlighten [view :as v] [atom :as a] [model :as m])))

(defn post-response [entry]
  (let [location (e/select-attr entry [[:link (e/attr= :rel "edit")]] :href)]
    (-> entry e/emit* resp/response
        (resp/status 201)
        (resp/content-type a/*atom-type*)
        (resp/header "Location" location))))

(defn populate-entry [entry]
  (let [pub-date (time/now)
        url (m/edit-url pub-date (e/select-text entry [:title]))
        id (a/make-tag-uri pub-date url)]
    (e/at entry
      [[:link #{(e/attr= :rel "edit") (e/attr= :rel "alternate")}]]
        (e/set-attr :href (str url))
      [:id] (e/content id)
      [#{:published :updated}] (e/content (str pub-date)))))

(defn handle-post [body]
  (let [entry (-> body e/xml-resource a/normalize-entry populate-entry)]
    (cond/handler-case :type
      (m/save-entry entry)
      (post-response entry)
      (handle :already-exists
        (-> (:message cond/*condition*)
            resp/response
            (resp/status 403))))))

(defn return-atom? [accept-header]
  (.contains accept-header a/*atom-type*))

(defn handle-get [url-path accept-header]
  (when-let [entry (m/get-entry url-path)]
    (if (return-atom? accept-header)
      (-> entry e/emit* resp/response (resp/content-type a/*atom-type*))
      (v/entry entry))))

(defn handle-get-collection [accept-header]
  (if (return-atom? accept-header)
    "coming soon"
    (v/entries)))

(defroutes routes
  (POST (:collection-uri m/*config*) {body :body} (handle-post body))
  (GET (:collection-uri m/*config*) {{accept-header "accept"} :headers}
    (handle-get-collection accept-header))
  (route/resources "/")
  (GET "/*" {{accept-header "accept"} :headers uri :uri}
    (handle-get uri accept-header)))

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
