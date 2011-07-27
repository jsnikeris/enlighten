(ns enlighten.test.model
  (:use [enlighten.model]
        [clojure.test])
  (:require [clj-time.format :as tf]
            [net.cgrand.enlive-html :as e]
            [clojure.contrib.java-utils :as ju]
            [clojure.java.io :as io]))

(def test-dir (str (ju/get-system-property "java.io.tmpdir") "/enlighten/"))

(defn delete-children [dir]
  (doseq [file (.listFiles (io/file dir))]
    (io/delete-file file)))

(defn entry-dir-fixture [f]
  (binding [*entry-dir* test-dir]
    (try
      (io/make-parents (str *entry-dir* "blah"))
      (f)
      (finally
       (delete-children *entry-dir*)))))

(defn post-url-fixture [f]
  (binding [*post-url* (java.net.URL. "https://test.blog.net/blog")]
    (f)))

(use-fixtures :each entry-dir-fixture post-url-fixture)

(deftest titleize-test
  (is (= "this-is-the-title" (titleize "This Is tHe TITLE"))))

(deftest edit-url-test
  (let [url (edit-url (tf/parse "2013-11-25") "This is the title")]
    (is (= "https://test.blog.net/blog/2013/nov/this-is-the-title"
           (str url)))))

(deftest filename-test
  (let [url (java.net.URL. "http://blah.net/blahg/posts/2008/jun/short-title")]
    (is (= (str test-dir "2008-jun-short-title.xml") (filename url)))))

(deftest save-entry-test
  (let [entry (e/xml-resource "test/post.xml")]
    (is (save-entry entry))
    (is (not (save-entry entry)))))
