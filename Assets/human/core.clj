(ns human.core
  (:use 
  arcadia.core
  arcadia.linear
  hard.core
  hard.animation
  hard.physics
  hard.seed
  human.data)
  (:require hard.mesh)
  (:import [UnityEngine]))

(defn rig [o] 
  ;(reveal-bones o)
  ;(form-colliders o)
  )

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
      :hair (srand-nth [:humans/hair-plain :humans/hair-bangs])
      }))

(defn rand-blend [mesh]
  (dorun 
    (for [i (range (shape-count mesh))]
      (when (< (srand) 0.8)
      (.SetBlendShapeWeight (->skinned mesh) (int i) (srand 100))))))


(def emotions ['joy1 'joy2 'joy3 'fear1 'fear2 'sick1 'sick2 'sick3 'anger1])


(defn make-head 
  ([seed] (seed! seed) (make-human))
  ([] 
    (let [
      dna (make-dna)
      ;ragdoll (clone! :humans/stable )
      o (clone! :humans/infihead )
      head-mesh (child-named o "infihead-mesh")
      hair (clone! (:hair dna))
      ;ragdoll-neck (child-named ragdoll "neck")
      ]

    (hard.mesh/material-color! head-mesh (:skin-color dna))
    ;(hard.mesh/material-color! body-mesh (:skin-color dna))
    ;(hard.mesh/material-color! arm-mesh (:skin-color dna))
    (hard.mesh/material-color! hair (:hair-color dna))

    (parent! hair o)

    ;(rotate! o [90 0 0])
    (local-scale! o (->v3 0.5))
    
    ;(parent! o (child-named ragdoll "head"))
    ;(position! o (->v3 (child-named ragdoll "head")))
    ;(rotation! o (rotation  (child-named ragdoll "head")))

    ;(parent! (child-named o "neck") ragdoll-neck)

    (rand-blend head-mesh)
    (rand-blend hair)
    (cross-fade o (str (srand-nth emotions)) 1.0)
    ;ragdoll 
    o
    )))


'(let [holder (GameObject. "heads")]
  (for [x (range 10)
        y (range 10)] 
    (parent! 
      (position! (make-head) (v3 (* 0.5 x) (* 0.5 y)  0))
      holder)))


(comment 

(do 
  (clear-cloned!)
  (dorun 
    (for [x (range 12) z (range 8)] 
      (let [target (v* [x 0 z] 0.8)
            o (make-human)]
        (position! o (if (odd? z) (v+ target [0.5 0 0]) target))
        (sel! o))))

  (mapv (comp #(force! % 0 (* 30000 (- (rand 2) 1)) 300 ) ->rigidbody) 
      (arcadia.core/objects-named "chest"))))





(comment 
  (import '[System IO.Path IO.File IO.StringWriter Environment])
  (binding [*print-length* nil](File/WriteAllText  "Assets/human/data.txt" (with-out-str (clojure.pprint/pprint  @DB))))
  (do 
  (clear-cloned!)
  (dorun (for [x (range 10) z (range 10) 
        :let [o (clone! :Sphere [x 0 z])
              [r g b] (rand-vec [0.0 1.0] [0.0 1.0] [0.0 1.0])
              c [r g b]]]
    (do 
      (material-color! o (color c))
      (set! (.name o) (apply str (interpose ", " (mapv #(re-find #"..." (str %)) c))))))))
  (swap! DB update-in [:hair-colors] into (map material-color (sel))))