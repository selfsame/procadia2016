(ns game.player
 (require [arcadia.core :as a]
          [arcadia.linear :as l]
          [game.core :as game]
          [game.ui :as ui]
          [game.data :as data]
          [hard.core :as hard]
          [game.gif :as gif]
          [tween.core :refer :all]))

(defonce rand-state (atom nil))

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

(defn change-clothing [redo-prev-roll? rb?]
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
  (game/make-head rb?)
  (vertex-color! body @data/skin-color)
  (if (not= "" @data/skater-name)
    (do
     (if redo-prev-roll?
      (set! UnityEngine.Random/state @rand-state)
      (reset! rand-state UnityEngine.Random/state))
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
  (reset! data/seed seed) 
  (reset! data/skater-name trimmed-val)
  (UnityEngine.Random/InitState seed)
  (change-clothing false false)))

(defn generate-name [go ptr]
 (let [rand-name (str (rand-nth data/first-names) " \"" (rand-nth data/nicknames) "\" " (rand-nth data/last-names))
       input (a/cmpt (a/object-named "NameInput") UnityEngine.UI.InputField)]
  (set! (.text input) rand-name)))

(defn setup-name-select [o]
 (hard/clear-cloned!)
 (let [skater (hard/clone! :player/skater)
       skater-anim (a/cmpt skater UnityEngine.Animator)
       name-canvas (hard/clone! :ui/skater-name-canvas (l/v3 0 100 0))
       generate-button (hard/child-named name-canvas "GenerateButton")
       input (a/object-named "NameInput")
       help-canvas (hard/clone! :ui/help-canvas)]
  (ui/tween-rect (hard/child-named help-canvas "Text") (l/v2 0 -183) 5)
  (hard/clone! :menu-backdrop (l/v3 210 39.6 23))
  (hard/clone! :EventSystem)
  (hard/clone! :Camera (l/v3 -1.81 1.92 -3.87))
  (game/make-head false)
  (vertex-color! (hard/child-named skater "BodyMesh") @data/skin-color)
  (set! (.worldCamera (a/cmpt name-canvas UnityEngine.Canvas)) UnityEngine.Camera/main)
  (a/hook+ generate-button :on-pointer-click #'generate-name)
  (a/hook+ name-canvas :update
   (fn [go]
    (if (UnityEngine.Input/GetKeyDown UnityEngine.KeyCode/Escape)
     (UnityEngine.Application/Quit))
    (if (UnityEngine.Input/GetKeyDown UnityEngine.KeyCode/Return)
     (do
      (add-watch data/player-spawned? nil
       (fn [_ _ _ new-state]
        (if new-state
         (timeline* (wait 0.01)
          #(try 
            (when-let [new-player (a/object-named "skater-ragdoll")]
              (set! (.name new-player) "skater")
              (change-clothing "anything" true true)) nil
            (catch Exception e (a/log e)))))))
      (game/make-level)
      (gif/setup)
      (a/destroy go)
      (a/destroy skater)))))
  (a/hook+ name-canvas :update :billboard
   (fn [go]
    (let [rect (a/cmpt go UnityEngine.RectTransform)]
     (.LookAt rect (l/v3* (l/v3- (.position rect) (.. UnityEngine.Camera/main transform position)) 2)))))
  (ui/on-value-changed input #'on-name-change)
  (if (not= "" @data/skater-name)
   (ui/set-input-text input @data/skater-name))
  (timeline*
   (tween {:position (l/v3+ (l/v3 0 3.25 1) (.. skater transform position))} name-canvas 2 {:out :pow3}))))

(reset! game.data/selection-fn #(setup-name-select nil))

'(setup-name-select nil)