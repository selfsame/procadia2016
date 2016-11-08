(ns game.ui
 (require [arcadia.core :as a]))

(defn on-value-changed [go callback]
 (let [input-field (a/cmpt go UnityEngine.UI.InputField)]
  (.. input-field onValueChanged
   (AddListener (fn [val] (callback val))))))
 