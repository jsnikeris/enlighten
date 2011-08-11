(ns enlighten.test.core
  (:use [clojure.test]
        (enlighten core atom model)
        [enlighten.test.model :only (entry-dir-fixture)])
  (:require [net.cgrand.enlive-html :as e]
            [ring.mock.request :as req]))

(use-fixtures :each entry-dir-fixture)

(def *atom-header* "application/atom+xml; charset=utf-8")

(defn do-post []
  (let [req-entry (e/xml-resource "test/post.xml")
        title (e/select-text [:title] req-entry)
        req (req/body (req/request :post (:collection-uri *config*))
                      (e/as-str req-entry))]
    (app req)))

(deftest happy-post
  (let [{:keys [headers body]} (do-post)
        resp-entry (apply e/html-snippet body)
        edit-link (e/select-attr resp-entry [(e/attr= :rel "edit")] :href)
        alt-link (e/select-attr resp-entry [(e/attr= :rel "alternate")] :href)]
    (is (= (headers "Content-Type") *atom-header*))
    (is (= edit-link alt-link (headers "Location")))
    (is (.endsWith edit-link "atom-powered-robots-run-amok"))))

(deftest happy-atom-get
  (let [{post-headers :headers post-body :body} (do-post)
        req (-> (req/request :get (post-headers "Location"))
                (req/header "Accept" *atom-type*))
        {get-headers :headers get-body :body} (app req)
        [post-id get-id] (map #(e/select-text (apply e/html-snippet %) [:id])
                              [get-body post-body])]
    (is (= (get-headers "Content-Type") *atom-header*))
    (is (= post-id get-id))))
