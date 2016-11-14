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
    [game.gif :as gif]
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
        board (clone! :player/board loc)
        prev-cam (the skatecam)]
    (if-not prev-cam
      (let [cam (clone! :player/skatecam)]
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
    (set-state! @data/player :total-euler (v3))
    (set-state! @data/player :rotation (v3))
    (hook-clear @data/player :update)
    (hook-clear @data/player :on-collision-enter)
    (hook-clear @data/player :on-collision-exit)
    (hook+ @data/player :update #'game.board/handle-input)
    (hook+ @data/player :on-collision-enter #(do %1 %2 (reset! game.board/TOUCHING true)))
    (hook+ @data/player :on-collision-exit #(do %1 %2 (reset! game.board/TOUCHING false)))
    board))

(defn respawn-player []
  (let [p (>v3 @data/player)]
    (destroy @data/player)
    (timeline [
               (wait 0.1)
               #(do (make-player p) nil)])))
(reset! data/respawn-fn respawn-player)

(defn run-tiled [seed o ct]
  (let [cmptype (if (= :overlap ct) 
                 (type (OverlapWFC.)) 
                 (type (SimpleTiledWFC.)))
        wfc (cmpt o cmptype)]
    (.Generate wfc)
   (if (.Run (.model wfc) seed (int 0))
    (do (.Draw wfc) true)
    (do 
      (log "unsolvable seed " ct seed)
      (seed! seed)
      (let [newseed (srand-int seed)]
        (set! (.seed wfc) newseed)
        (run-tiled newseed o ct))))))

(defn make-park [w h]
  (let [o (clone! :maps/autopark (v3 26 4 26))
        wfc (.AddComponent o (type (SimpleTiledWFC.)))]
   (clone! :park-bounds)
   (set! (.xmlpath wfc) "final.xml")
   (set! (.seed wfc) @data/seed)
   (set! (.gridsize wfc) (int 2))
   (timeline [
              (wait 0.1)
              #(do (local-scale! o (v3 park-scale)) false)])
   (set! (.width wfc) (int w))
   (set! (.depth wfc) (int h))
   (run-tiled @data/seed o :tiled) o))

(defn skyscrapers []
  (dorun 
    (map 
     #(local-scale! % (v3 1 (?f 0.2 3) 1)) 
      (concat 
        (every "BuildingBottom(Clone)")
        (every "BuildingMiddle(Clone)")))))

(defn make-city [w h]
 (let [o (clone! :maps/autocity (v3 0 0 0))
       sample (clone! :maps/city-sample (v3 0 -500 0))
       trainer (.AddComponent sample (type (Training.)))
       wfc (.AddComponent o (type (OverlapWFC.)))]
  (set! (.gridsize trainer) (int 10))
  (set! (.width trainer) (int 20))
  (set! (.depth trainer) (int 14))
  (set! (.training wfc) trainer)
  (set! (.gridsize wfc) (int 10))
  (set! (.width wfc) (int w))
  (set! (.depth wfc) (int h))
  (set! (.periodicInput wfc) true)
  (set! (.incremental wfc) true)
  (run-tiled @data/seed o :overlap) wfc))

(defn prune-city-center [city]
 (GameUtils/PruneMiddle city 4 4))

(defn make-level []
  (clear-cloned!)
  (destroy! (the Camera))
  (clone! :Canvas)
  (make-park park-size park-size)
  (let [citywfc (make-city city-size city-size)
        city (.gameObject citywfc)]
    (timeline [
      (wait 0.1)
     #(do 
      (prune-city-center city) 
      (destroy (the city-sample))
      (local-scale! city (v3 city-scale))
      (position! city 
        (v3 (* city-size -4 city-scale) 0 
            (* city-size -4 city-scale)))
      (cmpt- city (type citywfc))
      (skyscrapers) nil)]))
 (clone! :ui/recording-canvas)
 (if @data/recording?
  (set! (.alpha (cmpt (object-tagged "skatecam") UnityEngine.CanvasGroup)) 1.0))
 (make-player 
   (v3 (* park-size park-scale)
       (* 6 park-scale) 
       (* park-size park-scale))))

'(game.board/message "")
'(make-level)

'(hook+ (the hook) :start #'game.player/setup-name-select)