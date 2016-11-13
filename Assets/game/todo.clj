(ns game.todo)

::player
'((/) detect crashes
  (( ) bad landing crashes [board sideways, player hitting ground]))
'((x) fn to set up skater on board, and to sever joints for crash
  ((x) disable controlls, timeline animate crash camera, respawn))
'((x) fn to detect when ragdoll explodes + respawn)

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
'((x) keys to rotate the board
  ((x) :w :s key rotate X axis in air))

::camera
'(( ) chase velocity, not Z+)
'(( ) maintain minimum distance (lerped))

::tricks
'((x) board touching ground state)
'((x) board in air state)
'((x) when board in air, track accumulated rotations
  ((x) 1/4 1/2 3/4 :360)
  ((x) delta-euler fn - handle 360 turnover))
'(( ) don't award tricks that crash)

::bugs
'(( ) board can ride on vertical park-bounds)
'(( ) falling through floor does not ensure respawn in bounds)


'(.. (cmpt Selection/activeObject InputField)
    onValueChanged
    (AddListener (fn [] (log "x"))))

'1720572706