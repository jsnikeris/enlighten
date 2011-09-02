(ns enlighten.test.model
  (:use [enlighten.model]
        [clojure.test])
  (:require (clj-time [core :as time] [format :as tf])
            [clojure.contrib.java-utils :as ju]
            [net.cgrand.enlive-html :as e]
            [clojure.java.io :as io]))

(def test-dir (str (ju/get-system-property "java.io.tmpdir") "/enlighten/"))
(def test-uri "https://test.blog.net/blog")

(defn delete-children [dir]
  (doseq [file (.listFiles (io/file dir))]
    (io/delete-file file)))

(defn entry-dir-fixture [f]
  (binding [*config* (assoc *config* :entry-dir test-dir)]
    (let [dir (:entry-dir *config*)]
      (try
        (io/make-parents dir "blah")
        (delete-children dir)
        (f)
        (finally
         (delete-children dir))))))

(defn post-url-fixture [f]
  (binding [*config* (assoc *config* :collection-uri test-uri)]
    (f)))

(use-fixtures :each entry-dir-fixture post-url-fixture)

(deftest titleize-test
  (is (= "this-is-the-title" (titleize "This Is tHe TITLE"))))

(deftest edit-url-test
  (let [url (edit-url (tf/parse "2013-11-25") "This is the title")]
    (is (= "https://test.blog.net/blog/2013/11/this-is-the-title"
           (str url)))))

(deftest filename-test
  (let [url-path "/blahg/posts/2008/jun/short-title"]
    (is (= (str test-dir "2008-jun-short-title.xml") (filename url-path)))))

(deftest save-entry-test
  (let [entry (e/xml-resource "test/post.xml")
        edit-href (e/select-attr entry [(e/attr= :rel "edit")] :href)]
    (save-entry entry)
    (is (get-entry (-> edit-href java.net.URL. .getPath)))))
