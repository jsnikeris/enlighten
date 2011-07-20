(ns enlighten.test.core
  (:use [clojure.test]
        [ring.mock.request]
        [enlighten.core]
        [enlighten.atom])
  (:require [net.cgrand.enlive-html :as e]))

(deftest post
  (let [entry (e/xml-resource "test/post.xml")
        req (body (request :post (str *post-url*))
                  (str-entry entry))
        {:keys [headers] :as resp} (app req)]
    (is (= (headers "Content-Type") "application/atom+xml; charset=utf-8"))))
