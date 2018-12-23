(ns freecell-web.moves
  (:require
   [freecell-web.cards :refer
    [goes-on run-move sink can-sink should-sink]]))

(defn move-column [card-state on tn]
  (when (not= on tn)
    (let [{:keys [columns freecells]} card-state
          empty-count (count (filter not freecells))
          empty-column-count (count (remove seq columns))
          movable-to-column (* (inc empty-column-count) (inc empty-count))
          movable-to-empty (* empty-column-count (inc empty-count))
          new-columns (run-move columns on tn movable-to-column movable-to-empty)]
      (when new-columns
        (assoc card-state :columns new-columns)))))

(defn freecell-to-column [card-state fcn cn]
  (let [{:keys [columns freecells]} card-state
        mc (nth freecells fcn)
        col (nth columns cn)
        tc (first col)]
    (when (and mc (goes-on mc tc))
      (-> card-state
          (update-in [:columns cn] #(cons mc %))
          (assoc-in [:freecells fcn] nil)))))

(defn column-to-freecell [card-state cn fcn]
  (let [{:keys [columns freecells]} card-state
        col (nth columns cn)
        mc (first col)
        fcc (nth freecells fcn)]
    (when (and mc (not fcc))
      (-> card-state
          (update-in [:columns cn] rest)
          (assoc-in [:freecells fcn] mc)))))

(defn column-to-first-freecell [card-state cn]
  (some
   identity
   (for [fcn (range 4)]
     (column-to-freecell card-state cn fcn))))

(defn column-to-sink [card-state n]
  (let [{:keys [columns sinks]} card-state
        col (nth columns n)
        card (first col)]
    (when (and card (can-sink card sinks))
      (-> card-state
          (update-in [:columns n] rest)
          (update-in [:sinks] #(sink card %))))))

(defn freecell-to-sink [card-state n]
  (let [{:keys [freecells sinks]} card-state
        card (nth freecells n)]
    (when (and card (can-sink card sinks))
      (-> card-state
          (assoc-in [:freecells n] nil)
          (update-in [:sinks] #(sink card %))))))

(defn firstn [s pred]
  (some
   identity
   (map-indexed (fn [i x] (when (pred x) i)) s)))

(defn sinkable-freecell [freecells sinks]
  (firstn
   freecells
   #(should-sink % sinks)))

(defn sinkable-column [columns sinks]
  (firstn
   columns
   #(should-sink (first %) sinks)))

(defn autosink [{:keys [:columns :freecells :sinks] :as card-state}]
  (if-let [n (sinkable-freecell freecells sinks)]
    (freecell-to-sink card-state n)
    (when-let [n (sinkable-column columns sinks)]
      (column-to-sink card-state n))))
