(ns enlighten.view
  (:require [net.cgrand.enlive-html :as e]
            [clj-time.format :as tf]
            (enlighten [model :as m] [atom :as a])))

(defn pprint-date [s]
  (tf/unparse (tf/formatter "MMMM d, yyyy") (tf/parse s)))

(defn link-context [entry]
  {:prev (a/permalink (m/prev-entry entry))
   :next (a/permalink (m/next-entry entry))
   :current (a/permalink (first (m/get-entries)))
   :index (:collection-uri m/*config*)
   :author (:about-uri m/*config*)})

(defn link-transformation [entry]
  #(let [ctxt (link-context [entry])
         link-sel #{[:a] [:link]}
         rel-kw (keyword (e/select-attr % link-sel :rel))]
     (if (contains? ctxt rel-kw)
       (when-let [href (rel-kw ctxt)]
         (e/at % link-sel (e/set-attr :href href)))
       %)))

(defn time-transformation [entry]
  #(let [pub-date (e/select-text entry [:published])]
     ((e/do-> (e/content (pprint-date pub-date))
              (e/set-attr :datetime pub-date)) %)))

(e/deftemplate entry "templates/entry.html" [entry]
  [#{:title :#title}] (e/content (e/select-text entry [:title]))
  [:#content] (e/content (e/select entry [:content :> :*]))
  [[:time (e/attr? :pubdate)]] (time-transformation entry)
  #{[:nav :li] [:link]} (link-transformation entry))

(def entry-row-sel [:#entries :> :tbody :> [:tr e/first-of-type]])
(e/defsnippet entry-row "templates/entries.html" entry-row-sel [entry]
  [[:time (e/attr? :pubdate)]] (time-transformation entry)
  [:a] (e/do-> (e/set-attr :href (a/permalink entry))
               (e/content (e/select-text entry [:title]))))

(e/deftemplate entries "templates/entries.html" [] 
  [:#entries :> :tbody] (e/content (map entry-row (m/get-entries))))
