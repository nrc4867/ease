# Opening a project

1. A project file at the root names the engine version, the game's modules and its content folders.
2. The editor builds the game and its `*-editor` module through the Gradle tooling API.
3. The jars load into a child class loader. Engine and editor classes come from the parent, so types are shared and the game can be unloaded.
4. The asset database scans the content folders, runs importers on anything new or changed, and keeps watching.

**Hot reload:** on a code change the editor rebuilds, saves the open scene to memory, drops the old class loader, loads the new one and restores the scene. Content changes reload without a rebuild.
