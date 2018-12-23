(ns freecell-web.cards
  #?@(:cljs [(:require [cljs.reader :refer [register-tag-parser!]])]))

(def colors
  {:spades :black
   :hearts :red
   :clubs :black
   :diamonds :red})

(def suits (keys colors))

(defn color [card] (-> card :suit colors))

(defrecord Card [n suit])

#?(:cljs (register-tag-parser! 'freecell-web.cards.Card map->Card))

(def deck
  (into []
    (for [suit suits
          n (range 1 14)]
      (map->Card {:n n :suit suit}))))

(defn shuffled-deck [] (shuffle deck))

(def suit-chars
  {:spades \u2660
   :hearts \u2665
   :clubs \u2663
   :diamonds \u2666})

(def face-chars
  {1 \A
   11 \J
   12 \Q
   13 \K})

(defn display-string [card]
  (if (and card (> (:n card) 0))
    (str (face-chars (:n card) (:n card)) (suit-chars (:suit card)))
    "__"))

(defn flip [vs] (apply map vector vs))

(defn make-columns [deck]
  (->>
    deck
    (concat (repeat 4 nil))
    (partition 8)
    flip
    (map #(filter identity %))
    reverse
    (into [])))

(defn safe-inc [n]
  (if n
    (inc n)
    1))

(defn goes-on [card on]
  (or
    (nil? on)
    (and
      (not (= (color card) (color on)))
      (= (:n on) (safe-inc (:n card))))))

(defn moveable-subset [column]
  (when (seq column)
    (loop [subset [(first column)]
           card (first column)
           remaining (rest column)]
      (let [next-card (first remaining)]
        (if (and next-card (goes-on card next-card))
          (recur
            (conj subset next-card)
            next-card
            (rest remaining))
          subset)))))

(defn move-column [from to moveable]
  (let [moveable-cards (take moveable (moveable-subset from))]
    (some identity
      (for [i (range (count moveable-cards) 0 -1)]
        (let [to-move (take i moveable-cards)]
          (when (goes-on (last to-move) (first to))
            [(drop i from) (concat to-move to) i]))))))

(defn run-move [columns from to moveable-to-column moveable-to-empty]
  (let [from-col (nth columns from)
        to-col (nth columns to)
        moveable (if (seq to-col) moveable-to-column moveable-to-empty)]
    (when-let [[new-from new-to] (move-column from-col to-col moveable)]
      (-> columns
          (assoc from new-from)
          (assoc to new-to)))))

(defn can-sink [card sinks]
  (= (:n card) (safe-inc (get sinks (:suit card)))))

(defn sink [card sinks]
  (assoc sinks (:suit card) (:n card)))

(defn opposite-color [card]
  (if (= :black (color card))
    :red
    :black))

(defn min-sink [sinks]
  (apply min (vals sinks)))

(defn min-opposite-sink [card sinks]
  (apply
    min
    (for [[suit n] sinks
          :when (= (colors suit) (opposite-color card))]
      n)))

(defn should-sink [card sinks]
  (and
    (can-sink card sinks)
    (or
      ; Below cards are both already up
      (<= (:n card) (inc (min-opposite-sink card sinks)))
      ; If we have both black aces we can put up a red 3
      (and
        (<= (- (:n card) (min-sink sinks)) 3)
        (<= (- (:n card) (min-opposite-sink card sinks)) 2)))))

(defn winning? [card-state]
  (= #{13} (-> card-state :sinks vals set)))
