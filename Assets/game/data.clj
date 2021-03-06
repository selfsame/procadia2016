(ns game.data)

(defonce seed (atom 0))
(defonce skater-name (atom ""))
(defonce player (atom nil))
(defonce player-spawned? (atom false))
(defonce skin-color (atom nil))
(defonce respawn-fn (atom #()))
(defonce selection-fn (atom #()))
(defonce recording? (atom false))
(defonce trick-score (atom 0))
(defonce trick-multiplier (atom 1))

(def first-names
 ["alfred"
  "bart"
  "bob"
  "bort" ;; https://www.youtube.com/watch?v=Au1He0_eCkw
  "chad"
  "chris"
  "doug"
  "frank"
  "inigo"
  "jake"
  "kyle"
  "larry"
  "mike"
  "norman"
  "oscar"
  "pete"
  "romeo"
  "sean"
  "tyler"
  "uwe"
  "victor"
  "will"
  "xerxes"])

(def nicknames
 ["almanac"
  "beast"
  "charizard"
  "delta"
  "dingo"
  "eucalyptus"
  "frog"
  "grognard"
  "hermes"
  "irritating"
  "joker"
  "koolaid"
  "lemur"
  "mom"
  "nope"
  "orange"
  "pepper"
  "shifty"
  "sneaky"
  "tart"
  "vile"
  "wumpus"
  "zero"])
  
(def last-names
 ["anderson"
  "betts"
  "castro"
  "derringer"
  "ferguson"
  "gendt"
  "hill"
  "julio"
  "karlsen"
  "lima"
  "morris"
  "norton"
  "peters"
  "rourke"
  "stevens"
  "tanaka"
  "winters"])