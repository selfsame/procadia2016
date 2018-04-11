(ns game.tricks
  (:use 
    hard.seed)
  (:require
    game.data)
  (import
    [UnityEngine Mathf]))

(def prefixes [
               'fleeb
               'sun
               'heel
               'big
               'pop
               'laser
               'hard
               'death
               'wacko
               'fun
               'scoot
               'danger
               'moon
               'shish
               'fork
               'spoon
               'brain
               'dog
               'cat
               'cheese
               'groin
               'face
               'plasma
               'disco
               'camel
               'finger
               'ghetto
               'hand
               'grape
               'ginger
               'jesus
               'bull
               'bubble])

(def suffixes [
               'knocker
               'flip
               'kick
               'spin
               'bounce
               'slide
               'banger
               'bopper
               'ham
               'plate
               'twirl
               'glide
               'sandwich
               'slap
               'bend
               'shove
               'snap
               'crunch
               'sproing
               'spring 
               'shake
               'slam
               'gorp
               'blot
               'boing
               'futz
               'buzz
               'king
               'rod
               'dive
               'leap
               'prance])

(def ccw [
          'inverse
          'sinister
          'left
          'false
          'fake
          'counter
          'reverse
          'south
          'odd
          'alt
          'back
          'backside
          'lefty
          'rotated
          'opposite
          'left
          'counter
          'wound
          'twisted
          'rewound
          'negative
          'mirror])

(defn rand-trick [] 
  [0 (- 7 (srand-int 14))(- 7 (srand-int 14))])

(defn trick [seed]
  (seed! [@game.data/seed (mapv #(int (Mathf/Abs %)) seed)])
  (let [trickname 
        (str (srand-nth prefixes) "-"
             (srand-nth suffixes))
        dir (if (neg? (nth seed 1))
             (str (srand-nth ccw) " "))]
    (str dir trickname)))

(defn trick-score [trick]
  (reduce * (map #(inc (Mathf/Abs %)) trick)))

'(trick-score [0 -1 5])
'(trick (rand-trick))
