(ns game.ui
 (require [arcadia.core :as a]
          [arcadia.linear :as l]
          [tween.core :refer :all]))

(deftween [:rect-transform :anchored-position] [this]
 {:get (.anchoredPosition this)
  :tag UnityEngine.Vector2
  :base (a/cmpt this UnityEngine.RectTransform)})

(deftween [:rect-transform :scale] [this]
 {:get (.localScale this)
  :tag UnityEngine.Vector3
  :base (a/cmpt this UnityEngine.RectTransform)})

(defn on-value-changed [go callback]
 (let [input-field (a/cmpt go UnityEngine.UI.InputField)]
  (.. input-field onValueChanged
   (AddListener (fn [val] (callback val))))))
 
(defn set-input-text [go text]
 (let [input-field (a/cmpt go UnityEngine.UI.InputField)]
  (set! (.text input-field) text)))

(defn tween-rect [go pos dur]
 (let [rect (a/cmpt go UnityEngine.RectTransform)]
  (timeline*
   (tween {:rect-transform {:anchored-position pos}} rect dur {:in :pow3 :out :pow3}))))

(defn tween-rect-scale [go scale dur]
 (let [rect (a/cmpt go UnityEngine.RectTransform)]
  (timeline*
   (tween {:rect-transform {:scale scale}} rect dur {:in :pow3 :out :pow3})
   (tween {:rect-transform {:scale (l/v3 1 1 1)}} rect dur {:in :pow3 :out :pow3})))) 
   