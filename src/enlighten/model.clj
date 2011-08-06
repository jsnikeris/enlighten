(ns enlighten.model
  (:require [clojure.string :as str]
            [clojure.contrib.condition :as cond]
            [net.cgrand.enlive-html :as e]
            (clj-time [core :as time] [format :as tf])))

;; TODO: properties file?
(def *post-url* (java.net.URL. "http://localhost:3000/"))
(def *entry-dir* "/home/joe/Documents/Blog/Entries/")

;; TODO: handle other special characters
(defn titleize
  "Replaces spaces with dashes and lower-cases"
  [s]
  (-> s
      (str/replace " " "-")
      str/lower-case))

(defn edit-url
  "returns URL of the form: <*post-url*>2011/apr/this-is-the-title"
  [date-time title]
  (let [month-formatter (tf/formatter "MMM")
        month (->> date-time (tf/unparse month-formatter) str/lower-case)
        [_ base-url] (re-find #"(.*?)/?$" (str *post-url*))]
    (java.net.URL.
     (str/join "/" [base-url (time/year date-time) month (titleize title)]))))

(defn filename
  "defines the url-path to filesystem mapping"
  [url-path]
  (let [[title month year] (-> url-path (str/split #"/") rseq)]
    (str *entry-dir* year "-" month "-" title ".xml")))

(defn get-entry [url-path]
  (let [file (-> url-path filename java.io.File.)]
    (when (.exists file)
      (e/xml-resource file))))

(defn save-entry
  "potentially raises a condition of type :already-exists"
  [entry]
  (let [sel [[:link (e/attr= :rel "edit")]]
        url-path (-> entry (e/select-attrib sel :href) java.net.URL. .getPath)]
    (if (get-entry url-path)
      (cond/raise :type :already-exists
        :message "An entry with this title has already been posted this month.")
      (spit (filename url-path) (e/as-str entry)))))
