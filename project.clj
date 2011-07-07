(defproject blog "1.0.0-SNAPSHOT"
  :description "A blog"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [compojure "0.6.4"]
                 [enlive "1.0.0"]]
  :dev-dependencies [[lein-ring "0.4.5"]
                     [swank-clojure "1.3.0"]]
  :ring {:handler blog.core/app})
