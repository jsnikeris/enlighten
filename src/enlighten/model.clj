(ns enlighten.model
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.contrib.condition :as cond]
            [net.cgrand.enlive-html :as e]
            (clj-time [core :as time] [format :as tf])
            [enlighten.atom :as a]))

(def *config*
  (read-string (slurp (io/resource "config.clj"))))

(defn titleize
  "Replaces spaces with dashes and lower-cases"
  [s]
  (-> s
      (str/replace " " "-")
      str/lower-case))

(defn edit-url
  "returns URL of the form: <collection-uri>2011/4/this-is-the-title"
  [date-time title]
  (let [month (->> date-time (tf/unparse (tf/formatter "M")) str/lower-case)
        [_ base-url] (re-find #"(.*?)/?$" (:collection-uri *config*))]
    (java.net.URL.
     (str/join "/" [base-url (time/year date-time) month (titleize title)]))))

(defn filename
  "defines the url-path to filesystem mapping"
  [url-path]
  (let [[title month year] (-> url-path (str/split #"/") rseq)]
    (str (:entry-dir *config*) year "-" month "-" title ".xml")))

(defn get-entries []
  "returns entries in descending order by published date"
  (let [entries (for [file (-> *config* :entry-dir io/file .listFiles)]
                  (-> file e/xml-resource first))]
    (apply sorted-set-by #(compare (e/select-text %2 [:published])
                                   (e/select-text %1 [:published]))
           entries)))

(defn get-entry [url-path]
  (let [sel [[:link (e/attr= :rel "edit") (e/attr-ends :href url-path)]]
        entries (filter #(e/select? % sel) (get-entries))]
    (case (count entries)
      0 nil
      1 (first entries)
      (cond/raise :type :multiple-entries :message (str
        "More than one entry in the collection matches: " url-path)))))

(defn prev-entry [entry]
  (first (subseq (get-entries) > entry)))

(defn next-entry [entry]
  (first (rsubseq (get-entries) < entry)))

(defn save-entry
  "potentially raises a condition of type :already-exists"
  [entry]
  (let [url-path (-> entry a/permalink java.net.URL. .getPath)]
    (if (get-entry url-path)
      (cond/raise :type :already-exists
        :message "An entry with this title has already been posted this month.")
      (spit (filename url-path) (e/as-str entry)))))
