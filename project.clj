(defproject pcc "clojure"
  :description "Player album loader"
  :url "https://github.com/Tyrn/pcc"
  :license {:name "GPLv3"
            :url "http://www.gnu.org/licenses/gpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/tools.cli "0.3.1"]
                 [green-tags "0.3.0-alpha"]]
  :main ^:skip-aot pcc.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
