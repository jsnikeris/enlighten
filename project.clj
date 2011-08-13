(defproject enlighten "0.1.1"
  :description
  "An atom-enabled blog, written in Clojure, starring Compojure and Enlive."
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.6.5"]
                 [com.snikeris/enlive "2.0.0"]
                 [ring-mock "0.1.1"]
                 [ring "0.3.11"]
                 [clj-time "0.3.0"]]
  :dev-dependencies [[lein-ring "0.4.5"]]
  :main enlighten.core
  :ring {:handler enlighten.core/app})
