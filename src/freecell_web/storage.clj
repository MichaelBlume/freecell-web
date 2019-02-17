(ns freecell-web.storage
  (:require freecell-web.cards
            freecell-web.db))

(defn store-object [k obj]
  (spit k (prn-str obj)))

(defn get-object [k]
  (try
    (read-string (slurp k))
    (catch java.io.FileNotFoundException e)))
