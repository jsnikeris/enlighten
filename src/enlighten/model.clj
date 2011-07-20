(ns enlighten.model
  (:use [enlighten.atom])
  (:require [clj-time.format :as tf]))

;; TODO: properties file?
(def *entry-dir* "/home/joe/Documents/Blog/Entries/")

(defn filename [entry]
  (let [title (select-text [:title] entry)
        date (->> (select-text [:published] entry)
                  tf/parse
                  (tf/unparse (tf/formatters :year-month-day)))]
    (str *entry-dir* date "-" (titleize title) ".xml")))

(defn save-entry [entry]
  (do
    (spit (filename entry) (str-entry entry))
    true))
