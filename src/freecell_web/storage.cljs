(ns freecell-web.storage
  (:require [cljs.reader :refer [read-string]]))

(def store (.-localStorage js/window))

(defn store-object [k obj]
  (.setItem store k (prn-str obj)))

(defn get-object [k]
  (when-let [s (.getItem store k)]
    (read-string s)))

(defn save-state [db]
  (store-object "freecell-state" db))
