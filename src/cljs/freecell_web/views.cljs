(ns freecell-web.views
    (:require [re-frame.core :refer [subscribe dispatch]]
              [cljs.pprint :refer [pprint]]
              [freecell-web.subs]
              [freecell-web.cards :refer [display-string color]]))

(defn card [c & [on-click]]
  [:span
   {:style {:color (color c)}
    :on-click on-click}
   (display-string c)])

(defn enumerate [l]
  (map-indexed vector l))

(defn freecells []
  (let [cells (subscribe [:cells])]
    (fn []
      [:div
       {:style {:float :left}}
       (for [[i c] (enumerate @cells)]
         ^{:key i}
         [card c #(dispatch [:click-freecell i])])])))

(defn sinks []
  (let [cards (subscribe [:sinks])]
    (fn []
      [:div
       {:on-click #(dispatch [:click-sink])}
       (for [c @cards]
         ^{:key (:suit c)}
         [card c])])))

(defn top-row []
  [:div
   [freecells]
   [sinks]])

(defn column [i cards]
  (let [selected (subscribe [:selected i])]
    (fn [i cards]
      [:div
       {:style (merge
                 {:float :left}
                 (when @selected
                   {:background-color :grey}))
        :on-click #(dispatch [:click-column i])}
       (for [c (or (seq (reverse cards)) [nil])]
         ^{:key (display-string c)}
         [:p
          {:style {:text-align :right}}
          [card c]])])))

(defn columns []
  (let [cs (subscribe [:columns])]
    (fn []
      [:div
       (for [[i c] (map-indexed vector @cs)]
         ^{:key i}
         [column i c])])))

(defn bottom-row []
  [:div
   {:style {:float :down}}
   [:p
    {:on-click #(dispatch [:undo])}
    "Undo"]
   [:p
    {:on-click #(dispatch [:initialize-db])}
    "New game"]
   [:p
    {:on-click #(dispatch [:redo])}
    "Redo"]
   [:p
    {:on-click #(dispatch [:auto-sink])}
    "Sink"]])

(defn ui-display []
  (let [us (subscribe [:ui-state])]
    (fn []
      [:p (with-out-str (pprint @us))])))

(defn main-panel []
  [:div
   [:div
    [top-row]]
   [:div
    [columns]]
   [bottom-row]
   [:div
    [ui-display]
    ]
   ])
