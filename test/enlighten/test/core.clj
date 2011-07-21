(ns enlighten.test.core
  (:use [clojure.test]
        [ring.mock.request]
        [enlighten.core]
        [enlighten.atom])
  (:require [net.cgrand.enlive-html :as e]))

(deftest post
  (let [req-entry (e/xml-resource "test/post.xml")
        title (select-text [:title] req-entry)
        req (body (request :post (str *post-url*))
                  (str-entry req-entry))
        {:keys [headers body]} (app req)
        resp-entry (e/html-snippet body)
        [edit-link] (e/select resp-entry [[:link (e/attr= :rel "edit")]])
        [alt-link]  (e/select resp-entry [[:link (e/attr= :rel "alternate")]])
        edit-href (-> edit-link :attrs :href)
        alt-href  (-> alt-link :attrs :href)]
    (is (= (headers "Content-Type") "application/atom+xml; charset=utf-8"))
    (is (= edit-href alt-href))
    (is (.endsWith edit-href (titleize title)))))
