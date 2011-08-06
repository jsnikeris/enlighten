(ns enlighten.test.core
  (:use [clojure.test]
        [ring.mock.request]
        (enlighten core atom model)
        [enlighten.test.model :only (entry-dir-fixture)])
  (:require [net.cgrand.enlive-html :as e]))

(use-fixtures :each entry-dir-fixture)

(defn do-post []
  (let [req-entry (e/xml-resource "test/post.xml")
        title (e/select-text [:title] req-entry)
        req (body (request :post (str *post-url*))
                  (e/as-str req-entry))]
    (app req)))

(deftest happy-post
  (let [{:keys [headers body]} (do-post)
        resp-entry (e/html-snippet body)
        edit-link (e/select-attrib resp-entry [(e/attr= :rel "edit")] :href)
        alt-link (e/select-attrib resp-entry [(e/attr= :rel "alternate")] :href)]
    (is (= (headers "Content-Type") "application/atom+xml; charset=utf-8"))
    (is (= edit-link alt-link (headers "Location")))
    (is (.endsWith edit-link "atom-powered-robots-run-amok"))))
