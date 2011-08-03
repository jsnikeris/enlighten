(ns enlighten.view
  (:require [net.cgrand.enlive-html :as e]
            [enlighten.model :as m]))

(e/deftemplate main "templates/main.html" []
  [[:link (e/attr= :rel "service.post")]] (e/set-attr :href m/*post-url*))

(e/deftemplate entry "templates/main.html" [title pub-date content-nodes]
  [#{:title :#title}] (e/content title)
  [:article :> :header :> :time] (e/do-> (e/content pub-date)
                                         (e/set-attr :datetime pub-date))
  [:article :p] nil
  [:article :header] (e/after content-nodes))
