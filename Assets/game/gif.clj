(ns game.gif
 (require [arcadia.core :as a]
          [game.data :as data]
          [tween.core :refer :all])
 (import [Moments Recorder]
         [UnityEngine Canvas CanvasGroup]))

(def already-recording? (atom false))

(defn start-recording []
 (let [recording-canvas (a/cmpt (a/object-named "recording-canvas") CanvasGroup)
       skate-cam (a/object-tagged "skatecam")
       recorder (a/cmpt skate-cam Recorder)] 
  (.Record recorder)
  (set! (.alpha recording-canvas) (float 1)))) 

(defn finish-recording []
 (let [recording-canvas (a/cmpt (a/object-named "recording-canvas") CanvasGroup)
       skate-cam (a/object-tagged "skatecam")
       recorder (a/cmpt skate-cam Recorder)] 
  (.Save recorder)
  (set! (.alpha recording-canvas) (float 0))))

(defn setup []
 (add-watch data/recording? nil
  (fn [_ _ _ new-state]
   (cond
    (and
     (not @already-recording?)
     new-state)
    (do
     (reset! already-recording? true)
     (start-recording))
    (and
     @already-recording?
     (not new-state))
    (do
     (reset! already-recording? false)
     (finish-recording))))))

'(setup)
