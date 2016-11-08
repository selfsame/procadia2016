(ns game.player
 (require [arcadia.core :as a]
          [arcadia.linear :as l]
          [game.ui :as ui]
          [game.data :as data]
          [hard.core :as hard]
          [tween.core :refer :all]))

(defn rgb->float [n]
 (float (/ n 255)))

(defn gameobject? [x] (instance? UnityEngine.GameObject x))

(defn vertex-color! [gob col]
 (when (gameobject? gob)
  (when-let [renderer (.GetComponent gob UnityEngine.SkinnedMeshRenderer)]
   (let [mesh (.sharedMesh renderer) 
         verts (.vertices mesh)
         colors (into-array (take (count verts) (repeat col)))]
    (set! (.colors mesh) colors) nil))))

(def skin-colors
 {:pale (UnityEngine.Color. (rgb->float 250) (rgb->float 247) (rgb->float 181) 1)
  :tan (UnityEngine.Color. (rgb->float 168) (rgb->float 166) (rgb->float 122) 1)
  :dark (UnityEngine.Color. (rgb->float 61) (rgb->float 52) (rgb->float 38) 1)}) 

(defn rand-color []
 (UnityEngine.Color. (UnityEngine.Random/value) (UnityEngine.Random/value) (UnityEngine.Random/value) 1))
  
(defn change-clothing [val]
 (let [skater (a/object-named "skater")
       body (hard/child-named skater "BodyMesh")
       trucker-hat (a/cmpt (hard/child-named skater "TruckerHat") UnityEngine.Renderer)
       shirt (a/cmpt (hard/child-named skater "Shirt") UnityEngine.Renderer)
       pants (a/cmpt (hard/child-named skater "Pants") UnityEngine.Renderer)
       shorts (a/cmpt (hard/child-named skater "Shorts") UnityEngine.Renderer)
       shoe-right (a/cmpt (hard/child-named skater "ShoeLeft") UnityEngine.Renderer)
       shoe-left (a/cmpt (hard/child-named skater "ShoeRight") UnityEngine.Renderer)] 
  (doseq [clothing [trucker-hat shirt pants shorts shoe-right shoe-left]]
   (set! (.enabled clothing) false))
  (if (not= "" val)
    (do
     (let [body-roll (UnityEngine.Random/value)]
      (cond
       (<= body-roll 0.33) (vertex-color! body (:pale skin-colors))
       (<= body-roll 0.66) (vertex-color! body (:tan skin-colors))
       :else (vertex-color! body (:dark skin-colors))))  
     (let [hat-roll (UnityEngine.Random/value)]
      (if (<= hat-roll 0.5)
       (set! (.enabled trucker-hat) true)))
     (let [shirt-roll (UnityEngine.Random/value)]
      (if (<= shirt-roll 0.8)
       (do
        (set! (.enabled shirt) true)
        (vertex-color! (.gameObject shirt) (rand-color)))))
     (let [pants-roll (UnityEngine.Random/value)]
      (cond
       (< pants-roll 0.25) (do
                            (set! (.enabled pants) true)
                            (vertex-color! (.gameObject pants) (rand-color)))
       (< pants-roll 0.85) (do
                            (set! (.enabled shorts) true)
                            (vertex-color! (.gameObject shorts) (rand-color)))))
     (let [shoe-roll (UnityEngine.Random/value)]
      (cond
       (< shoe-roll 0.05) (set! (.enabled shoe-right) true)
       (< shoe-roll 0.1) (set! (.enabled shoe-left) true)
       (< shoe-roll 0.9) (doseq [shoe [shoe-left shoe-right]]
                          (set! (.enabled shoe) true))))))))

(defn on-name-change [val]
 (let [trimmed-val (.Trim val)
       seed (hash trimmed-val)
       skater (a/object-named "skater")]
      (a/log (str "ok " trimmed-val " " val " " seed " " skater))
  (reset! data/seed seed) 
  (UnityEngine.Random/InitState seed)
  (change-clothing trimmed-val)))

(defn setup-name-select []
 (let [skater (a/object-named "skater")
       skater-anim (a/cmpt skater UnityEngine.Animator)
       name-canvas (hard/clone! :ui/skater-name-canvas (l/v3 0 100 0))
       input (a/object-named "NameInput")]
  (.SetTrigger skater-anim "sit-idle")
  (a/set-state! skater :started-naming? false)
  (set! (.worldCamera (a/cmpt name-canvas UnityEngine.Canvas)) UnityEngine.Camera/main)
  (a/hook+ name-canvas :update :billboard
   (fn [go]
    (let [rect (a/cmpt go UnityEngine.RectTransform)]
     (.LookAt rect (l/v3* (l/v3- (.position rect) (.. UnityEngine.Camera/main transform position)) 5)))))
  (ui/on-value-changed input #'on-name-change)
  (timeline*
   (tween {:position (l/v3+ (l/v3 0 2.4 0) (.. skater transform position))} name-canvas 2 {:out :pow3}))))

(setup-name-select)
