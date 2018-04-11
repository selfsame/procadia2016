(ns game.core
  (:use 
    arcadia.core
    arcadia.linear
    tween.core
    hard.core
    hard.seed
    hard.physics
    hard.animation
    hard.input)
  (:require 
    [game.data :as data]
    game.cam
    game.board
    human.core)
  (:import [SimpleTiledWFC]
          [Training]
          GameUtils))

(def park-scale 4.3)
(def park-size 20)
(def city-scale 4.3)
(def city-size 30)

(defn make-head [rb?]
  (dorun (map destroy (every infihead)))
  (dorun (map destroy (every neck)))
  (let [head (human.core/make-head @data/seed)
        rag-head (the Bone.001)
        rag-neck (the Bone.002)
        neck (child-named head "neck")]
    (local-scale! head (v3 1))
    (rotation! head (.rotation (.transform rag-head)))
    (if rb?
      (rotate! head (v3 -90 0 0)))
   (position! head (.TransformPoint (.transform rag-head) (v3 0 0.0015 0)))
   (parent! head rag-head)
   (rotate! neck (v3 0 0 0))
   (position! neck (>v3 rag-neck))
   (parent! neck rag-neck)
   (local-scale! neck (v3 0.2))
   (timeline* :loop
     #(do (cross-fade head (str (srand-nth human.core/emotions)) 0.5))
     (wait (?f 0.3 1.0)))
   head))

(defn make-player [loc]
  (let [loc (or loc (v3 0 10 0))
        board (-clone! :player/board loc)
        prev-cam (the skatecam)]
    (if-not prev-cam
      (let [cam (-clone! :player/skatecam)]
        (hook+ cam :update #'game.cam/update-cam)
        (hook+ cam :on-draw-gizmos #'game.cam/gizmo-cam)))
    (game.board/record-wheels board)
    (timeline [
               (wait 0.01) 
               #(do (make-head true) 
                    (reset! game.board/ragmap (game.board/ragbody-map)) nil)])
    (reset! data/player-spawned? true)
    (reset! game.board/STANDING true)
    (reset! data/player board) 
    (state+ @data/player :total-euler (v3))
    (state+ @data/player :rotation (v3))
    (hook- @data/player :update)
    (hook- @data/player :on-collision-enter)
    (hook- @data/player :on-collision-exit)
    (hook+ @data/player :update #'game.board/handle-input)
    (hook+ @data/player :on-collision-enter #(do %1 %2 %3 (reset! game.board/TOUCHING true)))
    (hook+ @data/player :on-collision-exit #(do %1 %2 %3 (reset! game.board/TOUCHING false)))
    board))

(defn respawn-player []
  (let [p (v3+ (v3 0 2 0) (>v3 @data/player))]
    (destroy @data/player)
    (timeline [
               (wait 0.1)
               #(do (make-player p) nil)])))
(reset! data/respawn-fn respawn-player)


(defn just-skater [o _]
  (when-let [loading (the loading)]
    (destroy loading))
  ;(destroy-immediate (the board))
  (reset! game.board/ragmap nil)
  (reset! game.data/seed (rand-int 10000))
  (clear-cloned!)
  (timeline* 
    (wait 0.2)
    (fn []
      (clone! :Canvas)
      (clone! :EventSystem)
      (clone! :ground)
      (clone! :world)
      (make-player 
       (v3 0 20 0))
      (game.board/setup-touch-controls)
      nil)))

'(just-skater nil nil)

'(hook+ (the hook) :start #'game.core/just-skater)
