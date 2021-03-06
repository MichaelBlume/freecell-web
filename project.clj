(defproject freecell-web "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.660"]
                 [reagent "0.7.0"]
                 [re-frame "0.9.4"]
                 [com.cemerick/url "0.1.1"]]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :min-lein-version "2.5.3"

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :cljfmt {:indents
           {require [[:block 0]]
            ns [[:block 0]]
            #"^(?!:require|:import).*" [[:inner 0]]}}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.4"]]

    :plugins      [[lein-figwheel "0.5.9"]]
    }}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src"]
     :figwheel     {:on-jsload "freecell-web.core/mount-root"}
     :compiler     {:main                 freecell-web.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src"]
     :compiler     {:main            freecell-web.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}


    ]}

  )
