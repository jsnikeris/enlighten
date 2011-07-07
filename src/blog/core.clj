(ns blog.core
  (:use [compojure.core :only (defroutes GET POST)])
  (:require [compojure.route :as route]))

(defroutes routes
  (GET "/" [] "Hello, world")
  (route/resources "/"))

(def app routes)
