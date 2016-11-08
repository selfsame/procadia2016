(ns game.todo)

::skateboard
'((x) fn for checking if wheels are on ground)
'((x) apply downward force to board when not in air)
'((x) when in air allow twisting torque)
'((x) analog steering)
'((/) get sideways friction right
  (( ) when board quickly twists 180 reverse front direction))
'(( ) at the top of half pipes force the board to stay on the
  vertical plane)
'((x) jump command)
'((x) keys to rotate the board)

::camera
'(( ) chase velocity, not Z+)
'(( ) maintain minimum distance (lerped))

::tricks
'((/) board touching ground state)
'((x) board in air state)
'(( ) when board in air, track accumulated rotations
  (( ) 1/4 1/2 3/4 :360)
  (( ) delta-euler fn - handle 360 turnover))


'(.. (cmpt Selection/activeObject InputField)
    onValueChanged
    (AddListener (fn [] (log "x"))))