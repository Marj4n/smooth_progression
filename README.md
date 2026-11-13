# Smooth Progression

Configurable RPG progression for Minecraft 1.20.1: player experience, mob levels, elite enemies, boss progression and loot scaling. Supports Fabric and Forge.

## Build

Use JDK 21; generated mods target Java 17.

```sh
./gradlew build
./gradlew :fabric:build
./gradlew :forge:build
```

Both-loader output is collected in `build/libs/`; each module also has its own output folder. Fabric uses Fabric API; Forge uses Forgified Fabric API. Use matching loader versions of integrations. Pufferfish's Skills, Smooth Classes and Jade integration depend on the corresponding installed mods; exact dependency declarations are in module build files and metadata.

## Source layout

`common/src/main` contains progression systems and shared resources; `common/src/client` contains nameplates and UI. `fabric/src` and `forge/src` provide loader entrypoints and platform adapters. Configuration controls scaling and progression without changing the mod's content IDs.

## License

By Marj4n. See [LICENSE.txt](LICENSE.txt).

## Reuse permissions

Reusing or modifying Marj4n-owned source code or assets requires prior written permission. Contact Marj4n through [marj4n.vercel.app](https://marj4n.vercel.app/) with the intended use. See [LICENSE.txt](LICENSE.txt) for gameplay permissions and exceptions. Third-party licenses, existing grants and GitHub platform rights remain applicable.
