# Cooking and packaging

1. **Collect:** walk references from the start scenes and anything the project marks always-included; unused assets are left out.
2. **Cook:** compress textures for the target, write meshes and animations in runtime binary formats, validate every shader.
3. **Pack:** write cooked assets into archives with a GUID index the asset loader reads.
4. **Bundle:** a trimmed Java runtime (jlink) plus the player, the game jars and the archives, wrapped per OS as an installer (jpackage).

The export step fails if any shipped jar references `editor-api`, which keeps editor code out of builds.
