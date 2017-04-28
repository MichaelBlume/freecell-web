(ns freecell-web.events
    (:require [re-frame.core :refer [reg-event-db]]
              [freecell-web.cards :refer
               [goes-on run-move sink can-sink should-sink]]
              [freecell-web.db :refer
               [selected update-card-state init-state
                undo redo clear-ui undoing init-cards
                save-state]]))


(reg-event-db
 :initialize-db
 (fn  [db _]
   (if (seq db)
     (update-card-state db (constantly (init-cards)))
     (init-state))))

(defn move-column [card-state on tn]
  (let [{:keys [columns freecells]} card-state
        empty-count (count (filter not freecells))
        new-columns (run-move columns on tn (inc empty-count))]
    (when new-columns
      (assoc card-state :columns new-columns))))

(defn freecell-to-column [card-state fcn cn]
  (let [{:keys [columns freecells]} card-state
        mc (nth freecells fcn)
        col (nth columns cn)
        tc (first col)]
    (when (and mc (goes-on mc tc))
      (-> card-state
          (update-in [:columns cn] #(cons mc %))
          (assoc-in [:freecells fcn] nil)))))

(reg-event-db
  :click-column
  (fn [db [_ n]]
    (if-let [[or-type or-n] (selected db)]
      (update-card-state
        db
        (if (= or-type :column)
          #(move-column % or-n n)
          #(freecell-to-column % or-n n)))
      (assoc-in db [:ui-state :selected] [:column n]))))

(defn column-to-freecell [card-state cn fcn]
  (let [{:keys [columns freecells]} card-state
        col (nth columns cn)
        mc (first col)
        fcc (nth freecells fcn)]
    (when (and mc (not fcc))
      (-> card-state
          (update-in [:columns cn] rest)
          (assoc-in [:freecells fcn] mc)))))

(reg-event-db
  :click-freecell
  (fn [db [_ n]]
    (if-let [[or-type or-n] (selected db)]
      (update-card-state
        db
        (if (= or-type :column)
          #(column-to-freecell % or-n n)
          (constantly nil)))
      (assoc-in db [:ui-state :selected] [:freecell n]))))

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

(reg-event-db
  :click-sink
  (fn [db _]
    (update-card-state
      db
      (fn [card-state]
        (when-let [[or-type n] (selected db)]
          (if (= or-type :column)
            (column-to-sink card-state n)
            (freecell-to-sink card-state n)))))))

(reg-event-db
  :undo
  (fn [db _]
    (or (undo db) db)))

(reg-event-db
  :redo
  (fn [db _]
    (or (redo db) db)))

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

(defn try-sink [db]
  (if-not (or (selected db) (undoing db))
    (update-card-state
      db
      (fn [{:keys [:columns :freecells :sinks] :as card-state}]
        (if-let [n (sinkable-freecell freecells sinks)]
          (freecell-to-sink card-state n)
          (when-let [n (sinkable-column columns sinks)]
            (column-to-sink card-state n)))))
    db))

(reg-event-db
  :auto-sink
  (fn [db _]
    (if (::tried-sink db)
      db
      (let [new-db (try-sink db)]
        (if (= new-db db)
          (assoc db ::tried-sink true)
          new-db)))))

(reg-event-db
  :save-state
  (fn [db _]
    (if (::saved db)
      db
      (let [new-db (assoc db ::saved true)]
        (save-state new-db)
        new-db))))
