(ns freecell-web.events
    (:require [re-frame.core :refer [reg-event-db]]
              [freecell-web.moves :refer
               [move-column freecell-to-column column-to-freecell column-to-sink freecell-to-sink autosink]]
              [freecell-web.db :refer
               [selected update-card-state init-state
                undo redo clear-ui undoing init-cards
                save-state reset redo-all]]))


(reg-event-db
 :initialize-db
 (fn  [db _]
   (if (seq db)
     (update-card-state db (constantly (init-cards)))
     (init-state))))

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

(defn try-sink [db]
  (if-not (or (selected db) (undoing db))
    (update-card-state
      db
      autosink)
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

(reg-event-db
  :reset
  (fn [db _]
    (reset db)))

(reg-event-db :redo-all (fn [db _] (redo-all db)))
