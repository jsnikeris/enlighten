;; functions for manipulating atom entries

(ns enlighten.atom
  (:require [clojure.string :as str]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [net.cgrand.enlive-html :as e]))

;; TODO: properties file?
(def *post-url* (java.net.URL. "http://localhost:3000/"))

(def *atom-type* "application/atom+xml")

;; selectors of elements expected to be found on an entry
(def *expected-selectors* [[:id] [:published] [:updated]
                           [[:link (e/attr= :rel "edit")]]
                           [[:link (e/attr= :rel "alternate")]]])

(defn str-entry [entry]
  (apply str (e/emit* entry)))

;; TODO: handle other special characters
(defn titleize
  "Replaces spaces with dashes and lower-cases"
  [s]
  (-> s
      (str/replace " " "-")
      str/lower-case))

(defn make-edit-url
  "returns URL of the form: <*post-url*>/2011/apr/this-is-the-title"
  [date-time title]
  (let [month-formatter (tf/formatter "MMM")
        month (->> date-time (tf/unparse month-formatter) str/lower-case)
        [_ base-url] (re-find #"(.*?)/?$" (str *post-url*))]
    (java.net.URL.
     (str/join "/" [base-url (time/year date-time) month (titleize title)]))))

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

(defn normalize-entry [entry]
  "ensure entry has elements found in *expected-selectors*"
  (e/transform entry [:entry]
    (e/append
     (for [sel (filter #(empty? (e/select entry %)) *expected-selectors*)]
       (e/select (e/xml-resource "templates/entry.xml") sel)))))

(defn populate-entry [entry]
  (let [pub-date (time/now)
        title (select-text [:title] entry)
        edit-url (make-edit-url pub-date title)
        id (make-tag-uri pub-date edit-url)]
    (e/at entry
      [[:link #{(e/attr= :rel "edit") (e/attr= :rel "alternate")}]]
        (e/set-attr :href (str edit-url))
      [:id] (e/content id)
      [#{:published :updated}] (e/content (str pub-date)))))
