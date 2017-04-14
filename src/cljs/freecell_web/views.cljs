(ns freecell-web.views
    (:require [re-frame.core :refer [subscribe dispatch]]
              [cljs.pprint :refer [pprint]]
              [clojure.string :refer [join]]
              [freecell-web.subs]
              [freecell-web.cards :refer [display-string color]]))

(defn classes [& cs]
  (join \space (map name (filter identity cs))))

(defn card [c location & [on-click]]
  [:span
   {:class
     (if c
       (classes
         (color c) (:suit c) (str "n" (:n c)) location "card")
       (classes "no-card" location))
    :on-click on-click}
   (display-string c)])

(defn enumerate [l]
  (map-indexed vector l))

(defn freecells []
  (let [cells (subscribe [:cells])]
    (fn []
      [:div
       {:class "free-cells"}
       (for [[i c] (enumerate @cells)]
         ^{:key i}
         [card c "freecell" #(dispatch [:click-freecell i])])])))

(defn sinks []
  (let [cards (subscribe [:sinks])]
    (fn []
      [:div
       {:on-click #(dispatch [:click-sink])}
       (for [c @cards]
         ^{:key (:suit c)}
         [card c "sink"])])))

(defn top-row []
  [:div
   [freecells]
   [sinks]])

(defn column [i cards]
  (let [selected (subscribe [:selected i])]
    (fn [i cards]
      [:div
       {:class (classes "card-column" (when @selected "card-column-selected"))
        :on-click #(dispatch [:click-column i])}
       (for [c (or (seq (reverse cards)) [nil])]
         ^{:key (display-string c)}
         [:p
          {:style {:text-align :right}}
          [card c "column"]])])))

(defn columns []
  (let [cs (subscribe [:columns])]
    (fn []
      [:div
       (for [[i c] (map-indexed vector @cs)]
         ^{:key i}
         [column i c])])))

(defn bottom-row []
  [:div
   {:style {:float :down}
    :class "bottom-row"}
   [:span
    {:on-click #(dispatch [:undo])}
    "Undo"]
   [:span
    {:on-click #(dispatch [:initialize-db])}
    "New game"]
   [:span
    {:on-click #(dispatch [:redo])}
    "Redo"]
   [:span
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
