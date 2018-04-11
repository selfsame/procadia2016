(ns human.core
  (:use 
   arcadia.core
   arcadia.linear
   hard.core
   hard.animation
   hard.physics
   hard.seed
   human.data)
  (:require [game.data :as data]
            hard.mesh)
  (:import [UnityEngine]))

(defn rig [o]) 


(defn ->skinned [o]
  (cmpt (->go o) UnityEngine.SkinnedMeshRenderer))

(defn shape-count [o]
  (.blendShapeCount (.sharedMesh (->skinned o))))

(defn make-dna 
  ([] (make-dna (rand-int 100000)))
  ([seed]
   (seed! seed)
   {:skin-color (srand-nth (vec (:skin-tones @DB)))
     :hair-color (srand-nth (vec (:hair-colors @DB)))
     :hair (srand-nth [:humans/hair-plain :humans/hair-bangs])}))
      

(defn rand-blend [mesh]
  (dorun 
    (for [i (range (shape-count mesh))]
      (when (< (srand) 0.8)
       (.SetBlendShapeWeight (->skinned mesh) (int i) (srand 100))))))


(def emotions ['joy1 'joy2 'joy3 'fear1 'fear2 'sick1 'sick2 'sick3 'anger1])

(defn make-head 
  ([seed] (seed! seed) (make-head))
  ([] 
   (let [
         dna (make-dna @data/seed)
         o (clone! :humans/infihead) 
         head-mesh (child-named o "infihead-mesh")
         hair (clone! (:hair dna))]
    (material-color! head-mesh (:skin-color dna))
    (reset! data/skin-color (:skin-color dna))
    (material-color! hair (:hair-color dna))
    (parent! hair o)
    (local-scale! o (v3 0.5))
    (rand-blend head-mesh)
    (rand-blend hair)
    (cross-fade o (str (srand-nth emotions)) 1.0)
    o)))
    