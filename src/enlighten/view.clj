(ns enlighten.view
  (:require [net.cgrand.enlive-html :as e]
            [clj-time.format :as tf]
            [enlighten.model :as m]))

(defn pprint-date [s]
  (tf/unparse (tf/formatter "MMMM d, yyyy") (tf/parse s)))

(e/deftemplate entry "templates/main.html" [entry]
  [#{:title :#title}] (e/content (e/select-text entry [:title]))
  [:#content] (e/content (e/select entry [:content :> :*]))
  [[:time (e/attr? :pubdate)]]
    #(let [pub-date (e/select-text entry [:published])]
       ((e/do-> (e/content (pprint-date pub-date))
                (e/set-attr :datetime pub-date)) %)))
