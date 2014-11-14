(defproject pcc "0.1.0-SNAPSHOT"
  :description "Player album loader"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/tools.cli "0.3.1"]]
  :main ^:skip-aot pcc.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
