(ns freecell-web.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [freecell-web.events]
              [freecell-web.subs]
              [freecell-web.views :as views]
              [freecell-web.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (dev-setup)
  (mount-root)
  (re-frame/dispatch-sync [:initialize-db]))
