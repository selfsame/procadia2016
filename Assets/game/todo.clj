(ns game.todo)

::player
'(( ) detect crashes)
'(( ) fn to set up skater on board, and to sever joints for crash
  (( ) disable controlls, timeline animate crash camera, respawn))
'(( ) fn to detect when ragdoll explodes + respawn)

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
'((/) keys to rotate the board
  (( ) :w :s key rotate X axis in air))

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