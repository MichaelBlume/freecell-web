(ns freecell-web.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :refer [join]]
            [freecell-web.db]
            [freecell-web.cards :refer [display-string color]]))

(defn classes [& cs]
  (join \space (map name (filter identity cs))))

(defn card [c location & [on-click]]
  [:span
   {:class
    (if c
      (classes
        (color c) (:suit c) (str "n" (:n c)) "card" location)
      (classes "no-card" location))
    :on-click on-click}
   (display-string c)])

(defn enumerate [l]
  (map-indexed vector l))

(defn freecell [i c]
  (let [selected (subscribe [:selected-freecell i])]
    (fn [i c]
      [:div
       {:class (if @selected
                 "selected-freecell"
                 "unselected-freecell")}
       [card c "freecell"
        #(dispatch [:click-freecell i])]])))

(defn freecells []
  (let [cells (subscribe [:cells])]
    (fn []
      [:div
       {:class "hold-freecells"}
       (for [[i c] (enumerate @cells)]
         ^{:key i}
         [freecell i c])])))

(defn sinks []
  (let [cards (subscribe [:sinks])]
    (fn []
      [:div
       {:class "hold-sinks"
        :on-click #(dispatch [:click-sink])}
       (for [c @cards]
         ^{:key (:suit c)}
         [card c "sink"])])))

(defn top-row []
  [:div
   {:class "top-row"}
   [freecells]
   [sinks]])

(defn column [i cards]
  (let [selected (subscribe [:selected i])]
    (fn [i cards]
      [:div
       {:class (classes "card-column" (when @selected "card-column-selected"))
        :on-click #(dispatch [:click-column i])}
       (for [[i c] (enumerate (or (seq (reverse cards)) [nil]))]
         ^{:key i}
         [card c "column"])])))

(defn columns []
  (let [cs (subscribe [:columns])]
    (fn []
      [:div
       {:class "columns"}
       (for [[i c] (map-indexed vector @cs)]
         ^{:key i}
         [column i c])])))

(defn button [text & event]
  [:button
   {:on-click #(dispatch (into [] event))}
   text])

(defn button-row []
  [:div
   {:style {:float :down}
    :class "bottom-row"}
   [button "Reset" :reset]
   [button "Undo" :undo]
   [button "New game" :initialize-db]
   [button "Redo" :redo]
   [button "Redo All" :redo-all]])

(defn debug []
  (let [ds (subscribe [:debug])]
    (fn []
      [:div
       {:class "debug-panel"}
       [:pre @ds]])))

(defn can-win []
  (let [score (subscribe [:score])]
    (fn []
      [:div
       [:p @score]])))

(defn main-panel []
  [:div
   {:class "freecell-game"}
   [button-row]
   [top-row]
   [columns]
   [can-win]])
