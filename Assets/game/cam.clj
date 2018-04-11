(ns game.cam
  (:use 
    arcadia.core
    arcadia.linear
    tween.core
    hard.core)
  (:require
    game.data
    game.board))

(def dist 12)
(def min-cam-dist 12.0)

(defn update-cam [o _]
  (try 
    (let [focus (if @game.board/STANDING @game.data/player (:head @game.board/ragmap))
          target (v3+ (.TransformPoint (.transform @game.data/player) (v3 5 0 (- dist)))
                      (v3 0 10 0))
          cam-offset (v3- (>v3 o) (>v3 focus))
          cam-dist (.magnitude cam-offset)
          camTP (.normalized (v3* cam-offset -1))
          cam-correct (if (< cam-dist min-cam-dist) 
            (v3* camTP (-  cam-dist min-cam-dist))
            (v3 0))]
      (position! o 
        (v3+ cam-correct 
          (lerp o target (∆ 4))))
      (lerp-look! o focus (∆ 8)))
    (catch Exception e (log e))))

(defn gizmo-cam [o _]
  #_(let [focus (if @game.board/STANDING @game.data/player (:head @game.board/ragmap))
        target (v3+ (.TransformPoint (.transform @game.data/player) (v3 0 0 (- dist)))
                    (v3 0 10 0))
        cam-offset (v3- (>v3 o) (>v3 focus))
        cam-dist (.magnitude cam-offset)
        camTP (.normalized (v3* cam-offset -1))
        cam-correct (if (< cam-dist min-cam-dist) 
          (v3* camTP (-  cam-dist min-cam-dist))
          (v3 0))]

    (try 
      (gizmo-color (color 1 1 0))
      (gizmo-line (>v3 o) (v3+ (>v3 o) cam-correct))
      (gizmo-color (color 1 0 0))
      (gizmo-line  (>v3 o) (>v3 @game.data/player))
      (gizmo-color (color 0 1 1))
      (gizmo-line  (>v3 @game.data/player) 
                   (.TransformPoint (.transform @game.data/player) (v3 0 -1.0 0)))
      (catch Exception e (log e)))) )