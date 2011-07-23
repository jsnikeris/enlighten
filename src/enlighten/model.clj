(ns enlighten.model
  (:require [clojure.string :as str]
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
  "returns URL of the form: <*post-url*>/2011/apr/this-is-the-title"
  [date-time title]
  (let [month-formatter (tf/formatter "MMM")
        month (->> date-time (tf/unparse month-formatter) str/lower-case)
        [_ base-url] (re-find #"(.*?)/?$" (str *post-url*))]
    (java.net.URL.
     (str/join "/" [base-url (time/year date-time) month (titleize title)]))))

(defn filename [entry]
  (let [title (e/select-text entry [:title])
        date (->> (e/select-text entry [:published])
                  tf/parse
                  (tf/unparse (tf/formatters :year-month-day)))]
    (str *entry-dir* date "-" (titleize title) ".xml")))

(defn get-entry [url]
  (let [[title month year] (-> url .getPath (str/split #"/") rseq)]))
        

(defn save-entry [entry]
  (do
    (spit (filename entry) (e/as-str entry))
    true))
