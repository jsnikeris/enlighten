(ns enlighten.view
  (:require [net.cgrand.enlive-html :as e]
            [clj-time.format :as tf]
            (enlighten [model :as m] [atom :as a])))

(defn pprint-date [s]
  (tf/unparse (tf/formatter "MMMM d, yyyy") (tf/parse s)))

(defn link-context [entry]
  {:prev (a/permalink (m/prev-entry entry))
   :next (a/permalink (m/next-entry entry))
   :last (a/permalink (first (m/get-entries)))})

(e/deftemplate entry "templates/entry.html" [entry]
  [#{:title :#title}] (e/content (e/select-text entry [:title]))
  [:#content] (e/content (e/select entry [:content :> :*]))
  [[:time (e/attr? :pubdate)]]
    #(let [pub-date (e/select-text entry [:published])]
       ((e/do-> (e/content (pprint-date pub-date))
                (e/set-attr :datetime pub-date)) %))
  #{[:nav :li] [:link]}
    #(let [ctxt (link-context [entry])
           link-sel #{[:a] [:link]}
           rel-kw (keyword (e/select-attr % link-sel :rel))]
       (if (contains? ctxt rel-kw)
         (when-let [href (rel-kw ctxt)]
           (e/at % link-sel (e/set-attr :href href)))
         %)))
