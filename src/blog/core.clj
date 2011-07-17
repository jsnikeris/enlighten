;; Considerations:
;;   - the edit-url for an entry is also its permalink

(ns blog.core
  (:use [compojure.core :only (defroutes GET POST)])
  (:require [clojure.string :as str]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as e]
            [ring.adapter.jetty :as jetty]))

;; TODO: properties file?
(def *post-url* "http://localhost:8080/")
(def *entry-dir* "/home/joe/Documents/Blog/Entries/")

(e/deftemplate main "templates/main.html" []
  [[:link (e/attr= :rel "service.post")]] (e/set-attr :href *post-url*))

;; TODO: handle other special characters
(defn hyphenize
  "Replaces spaces with dashes and lower-cases"
  [s]
  (-> s
      (str/replace " " "-")
      str/lower-case))

(defn make-edit-url
  "returns URL of the form: <*post-url*>/2011/apr/this-is-the-title"
  [date title]
  (let [month-formatter (tf/formatter "MMM")
        month (->> date (tf/unparse month-formatter) str/lower-case)
        [_ base-url] (re-find #"(.*?)/?$" *post-url*)]
    (str/join "/" [base-url (time/year date) month (hyphenize title)])))

(defn make-tag-uri
  "returns a tag URI given a DateTime and a URL"
  [date url]
  (let [host (.getHost url)
        path (.getPath url)
        date-str (tf/unparse (tf/formatters :year-month-day) date)]
    (str "tag:" host "," date-str ":" path)))

(defn select-text
  "returns the text of the first matching node"
  [selector node-or-nodes]
  (-> node-or-nodes (e/select selector) first e/text))

(defn make-filename [entry]
  (let [title (select-text [:title] entry)
        date (->> (select-text [:published] entry)
                  tf/parse
                  (tf/unparse (tf/formatters :year-month-day)))]
    (str date "-" (hyphenize title))))

(defn populate-entry [entry]
  (let [pub-date (time/now)
        title (select-text [:title] entry)
        edit-url (make-edit-url pub-date title)
        id (make-tag-uri pub-date (java.net.URL. edit-url))]
    (e/at entry
          [[:link #{(e/attr= :rel "edit")
                    (e/attr= :rel "alternate")}]] (e/set-attr :href edit-url)
          [:id] (e/content id)
          [#{:published :updated}] (e/content (str pub-date)))))

(defn post-response [entry]
  (apply str (e/emit* entry)))

;; TODO: validate/normalize Entry
(defn handle-post [body]
  (let [entry (populate-entry (e/xml-resource body))
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
;;   (jetty/run-jetty #'app {:port 8080 :join? false}))
