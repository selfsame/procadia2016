(ns game.simple
    (:use 
    arcadia.core
    arcadia.linear
    tween.core
    hard.core))


(defn start [o _]
  (create-primitive :capsule))