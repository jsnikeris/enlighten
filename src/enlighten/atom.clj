;; functions for manipulating atom entries

(ns enlighten.atom
  (:use [enlighten.model :only (edit-url)])
  (:require (clj-time [core :as time] [format :as tf])
            [net.cgrand.enlive-html :as e]))

(def *atom-type* "application/atom+xml")

;; selectors of elements expected to be found on an entry
(def *expected-selectors* [[:id] [:published] [:updated]
                           [[:link (e/attr= :rel "edit")]]
                           [[:link (e/attr= :rel "alternate")]]])

(defn make-tag-uri
  "returns a tag URI given a DateTime and a URL"
  [date url]
  (let [host (.getHost url)
        path (.getPath url)
        date-str (tf/unparse (tf/formatters :year-month-day) date)]
    (str "tag:" host "," date-str ":" path)))

(defn normalize-entry [entry]
  "ensure entry has elements found in *expected-selectors*"
  (e/transform entry [:entry]
    (e/append
     (for [sel (filter #(empty? (e/select entry %)) *expected-selectors*)]
       (e/select (e/xml-resource "templates/entry.xml") sel)))))

(defn populate-entry [entry]
  (let [pub-date (time/now)
        title (e/select-text entry [:title])
        url (edit-url pub-date title)
        id (make-tag-uri pub-date url)]
    (e/at entry
      [[:link #{(e/attr= :rel "edit") (e/attr= :rel "alternate")}]]
        (e/set-attr :href (str url))
      [:id] (e/content id)
      [#{:published :updated}] (e/content (str pub-date)))))
