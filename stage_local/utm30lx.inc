define utm30lx ranger
(
  sensor (
    range [ 0.1 30 ]
    fov 270.0
    samples 1080
    watts 8.0
    color_rgba [ 0 0 1 0.15 ]
  )

  # generic model properties
  model (
    color "blue"
    size [ 0.05 0.05 0.1 ]	
    pose [ 0.13 0 -0.2 0 ]
  )
)