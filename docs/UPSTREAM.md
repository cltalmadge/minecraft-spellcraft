# Upstream versions used by the scaffold

Verified when this scaffold was created: **2026-09-26**.

## Fabric

- Minecraft 26.3 Fabric announcement: https://www.fabricmc.net/2026/09/15/263.html
- Fabric example mod, `26.3` branch: https://github.com/FabricMC/fabric-example-mod/tree/26.3
- Fabric API: `0.161.0+26.3`
- Fabric Loader: `0.19.5`
- Fabric example wrapper at verification time: Gradle `9.7.1`

## NeoForge

- 26.3 ModDevGradle MDK: https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle
- NeoForge: `26.3.0.10-beta`
- ModDevGradle: `2.0.147`
- MDK wrapper at verification time: Gradle `9.2.1`

## Prism Launcher

- Creating instances: https://prismlauncher.org/wiki/getting-started/create-instance/
- Managing loader mods: https://prismlauncher.org/wiki/help-pages/loader-mods/
- Downloading mods / Fabric API note: https://prismlauncher.org/wiki/getting-started/download-mods/
- Data locations: https://prismlauncher.org/wiki/getting-started/data-location/

Exact loader APIs are expected to move. Keep version-specific changes inside `fabric/` and `neoforge/`.


## Local build topology

This repository intentionally uses one root Gradle 9.7.1 invocation for all subprojects so IDEs import a single coherent project model. Loader plugin versions remain isolated per subproject even though the Gradle process is shared.
