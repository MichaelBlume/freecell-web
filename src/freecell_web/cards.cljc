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

(declare make-fast-column)

(defn make-columns [deck]
  (->>
    deck
    (concat (repeat 4 nil))
    (partition 8)
    flip
    (map #(filter identity %))
    (map make-fast-column)
    reverse
    (into [])))

(defprotocol Column
  (get-all-cards [this])
  (has-cards? [this])
  (top-card [this])
  (movable-subset [this])
  (put-card [this card])
  (put-cards [this new-cards])
  (drop-card [this])
  (drop-cards [this n]))

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

(defn- movable-subset* [column]
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

(defrecord FastColumn [movable immovable]
  Column
  (get-all-cards [this]
    (concat movable immovable))
  (has-cards? [this]
    (some? movable))
  (top-card [this]
    (first movable))
  (movable-subset [this]
    movable)
  (put-card [this card]
    (FastColumn. (cons card movable) immovable))
  (put-cards [this cards]
    (FastColumn. (into [] (concat cards movable)) immovable))
  (drop-card [this]
    (case (count movable)
      0 nil
      1 (make-fast-column immovable)
      (FastColumn. (next movable) immovable)))
  (drop-cards [this n]
    (let [mcount (count movable)]
      (if (>= n mcount)
        (make-fast-column (drop (- n mcount) immovable))
        (FastColumn. (into [] (drop n movable)) immovable)))))

(defn make-fast-column [cards]
  (when (seq cards)
    (let [movable (movable-subset* cards)
          immovable (seq (drop (count movable) cards))]
      (->FastColumn movable immovable))))

(extend-protocol Column
  nil
  (get-all-cards [this]
    nil)
  (has-cards? [this]
    false)
  (top-card [this]
    nil)
  (movable-subset [this]
    nil)
  (put-card [this card]
    (->FastColumn [card] nil))
  (put-cards [this new-cards]
    (->FastColumn new-cards nil))
  (drop-card [this]
    nil)
  (drop-cards [this n]
    nil)
  #?(:clj Object :cljs default)
  (get-all-cards [this]
    this)
  (has-cards? [this]
    (seq this))
  (top-card [this]
    (first this))
  (movable-subset [this]
    (movable-subset* this))
  (put-card [this card]
    (make-fast-column
      (cons card this)))
  (put-cards [this new-cards]
    (make-fast-column
      (concat new-cards this)))
  (drop-card [this]
    (make-fast-column
      (rest this)))
  (drop-cards [this n]
    (make-fast-column
      (drop n this))))

#?(:cljs (register-tag-parser! 'freecell-web.cards.FastColumn map->FastColumn))

(defn move-column [from to movable]
  (let [movable-cards (take movable (movable-subset from))]
    (some identity
      (for [i (range (count movable-cards) 0 -1)]
        (let [to-move (take i movable-cards)]
          (when (goes-on (last to-move) (top-card to))
            [(drop-cards from i) (put-cards to to-move)]))))))

(defn run-move [columns from to movable-to-column movable-to-empty]
  (let [from-col (nth columns from)
        to-col (nth columns to)
        movable (if (has-cards? to-col) movable-to-column movable-to-empty)]
    (when-let [[new-from new-to] (move-column from-col to-col movable)]
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
