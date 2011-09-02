;; functions for manipulating atom entries

(ns enlighten.atom
  (:require [clj-time.format :as tf]
            [net.cgrand.enlive-html :as e]))

(def *atom-type* "application/atom+xml")

;; selectors of elements expected to be found on an entry
(def *expected-selectors* [[:id] [:published] [:updated]
                           [[:link (e/attr= :rel "edit")]]
                           [[:link (e/attr= :rel "alternate")]]])

(defn permalink [entry]
  (e/select-attr entry [[:link (e/attr= :rel "edit")]] :href))

(defn url-path [entry]
  (-> entry permalink java.net.URL. .getPath))

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
