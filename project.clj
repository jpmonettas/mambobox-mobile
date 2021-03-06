(defproject mambobox-mobile "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha11"]
                 [org.clojure/clojurescript "1.9.293"]
                 [reagent "0.6.0" :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server]]
                 [re-frame "0.8.0"]
                 [funcool/promesa "1.5.0"]
                 [day8.re-frame/http-fx "0.1.2"]
                 [cljs-ajax "0.5.8"]
                 [camel-snake-kebab "0.4.0"]
                 [com.taoensso/tempura "1.0.0-RC3"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]]
  
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.8"]]
  :clean-targets ["target/" "index.ios.js" "index.android.js"]
  :aliases {"prod-build" ^{:doc "Recompile code with prod profile."}
            ["do" "clean"
             ["with-profile" "prod" "cljsbuild" "once" "ios"]
             ["with-profile" "prod" "cljsbuild" "once" "android"]]}
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.8"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [binaryage/devtools "0.8.1"]]
                   :source-paths ["src" "mambobox-core/src/cljc" "env/dev"]
                   :cljsbuild    {:builds {:ios     {:source-paths ["src" "mambobox-core/src/cljc" "env/dev"]
                                                     :figwheel     true
                                                     :compiler     {:output-to     "target/ios/not-used.js"
                                                                    :main          "env.ios.main"
                                                                    :output-dir    "target/ios"
                                                                    :optimizations :none}}
                                           :android {:source-paths ["src" "mambobox-core/src/cljc" "env/dev"]
                                                     :figwheel     true
                                                     :compiler     {:output-to     "target/android/not-used.js"
                                                                    :main          "env.android.main"
                                                                    :output-dir    "target/android"
                                                                    :optimizations :none
                                                                    :source-map     true}}}}
                                                     
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :prod {:cljsbuild {:builds {:ios     {:source-paths ["src" "mambobox-core/src/cljc" "env/prod"]
                                                   :compiler     {:output-to     "index.ios.js"
                                                                  :main          "env.ios.main"
                                                                  :output-dir    "target/ios"
                                                                  :static-fns    true
                                                                  :optimize-constants true
                                                                  :optimizations :simple
                                                                  :closure-defines {"goog.DEBUG" false}}}
                                         :android {:source-paths ["src" "mambobox-core/src/cljc" "env/prod"]
                                                   :compiler     {:output-to     "index.android.js"
                                                                  :main          "env.android.main"
                                                                  :output-dir    "target/android"
                                                                  :static-fns    true
                                                                  :optimize-constants true
                                                                  :optimizations :simple
                                                                  :closure-defines {"goog.DEBUG" false}}}}}
                    }})
