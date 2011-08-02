(ns enlighten.view
  (:require [net.cgrand.enlive-html :as e]
            [enlighten.model :as m]))

(e/deftemplate main "templates/main.html" []
  [[:link (e/attr= :rel "service.post")]] (e/set-attr :href m/*post-url*))

(e/deftemplate entry "templates/main.html" [entry])
