# Spellcraft

Spellcraft is a Minecraft Java Edition 26.3 mod about discovering magic through experimentation. Its design centers on three ideas: Form describes the quality being manipulated, Number describes the relationship, and Geometry describes how that relationship is arranged. Materials participate in Forms, and their arrangement gives a spell its meaning.

The mod supports Fabric and NeoForge. The playable prototype includes a wand that transmits Heat to a living target, a page that holds a recorded spell, and a spellbook. Spell construction currently happens in code; there is no in-game spellcrafting interface yet.

## Build

Use JDK 25. The included Gradle wrapper downloads the build tools and dependencies on its first run.

From the repository root, build both loader versions:

```bash
./build-all.sh
```

To build only one loader:

```bash
./gradlew :fabric:build
# or
./gradlew :neoforge:build
```

The installable jars are written to `fabric/build/libs/` and `neoforge/build/libs/`. Choose the `spellcraft-fabric-<version>.jar` or `spellcraft-neoforge-<version>.jar` file, not the sources jar.

## Install

1. Create a Minecraft 26.3 instance with Fabric or NeoForge.
2. Copy the matching Spellcraft jar into the instance's `mods` directory.
3. For Fabric, also install Fabric API for Minecraft 26.3.
4. Launch the instance.

Only the matching loader jar belongs in the game instance. The `domain` and `common` jars are development artifacts.

For a Prism Launcher instance, the installation helper copies the latest built jar and replaces older Spellcraft jars for that loader:

```bash
./tools/install-prism.sh fabric "/absolute/path/to/instance/.minecraft/mods"
# or
./tools/install-prism.sh neoforge "/absolute/path/to/instance/.minecraft/mods"
```

## Try the wand

Open a world with commands enabled and give yourself a wand:

```mcfunction
/give @s spellcraft:wand
```

Hold it, aim at a living entity within 16 blocks, and right-click. The default recording transmits Heat, igniting the target and producing flame particles. Solid blocks obstruct the cast. Successful casts have a one-second cooldown and consume food exhaustion; casting requires a nonempty food bar.

The message above the hotbar reports whether the cast succeeded or why it could not be performed.

## Spell pages and the spellbook

```mcfunction
/give @s spellcraft:spell_page
/give @s spellcraft:spellbook
```

The page holds the same Heat recording as the default wand. It stores spell information and has no right-click action or binding interface.

On Fabric, right-click the spellbook to open a sample observation about thermal transmission. The screen displays example content rather than tracking learned spells. On NeoForge, the spellbook item has no screen yet.

## Run from source

Launch a development client directly through Gradle:

```bash
./gradlew :fabric:runClient
# or
./gradlew :neoforge:runClient
```

Use the same in-game commands above to obtain the items.

## Project layout

- `domain` — Minecraft-independent spell construction, analysis, compilation, and compatibility rules.
- `common` — shared Minecraft items, casting, material correspondences, persistence, and presentation.
- `fabric` — Fabric registration and integration.
- `neoforge` — NeoForge registration and integration.

Both loaders use the same domain and common code.

## Form contributions

`MaterialProfile` describes intrinsic potential. `WorkingNode.expressedForms` selects the contribution to an experiment, validated as a subset whose strengths cannot exceed the material's participation. `WorkingNode.expressingAll` is an explicit convenience for experiments that use every available Form.

Analysis preserves those contributions and material identities in `SpellLocus.expressedForms`. Program schema v3 stores `expressed_forms`, without a full material profile or a later correspondence lookup. Older program schemas are rejected; the recording envelope remains v1. Changing material data cannot rewrite an already recorded spell.

`SpellProgram.invokedForms()` selects the focus for concentration or the source for transfer/mediation; stabilization currently invokes nothing instantaneous. Runtime handler preflight and basic vessel affinity use only these invoked Forms. Recipient, mediator, and stabilizer expressions remain semantic information. Mediation and stabilization execution remain unsupported. A locus's material records its discovery participant; it does not currently restrict the concrete runtime target's material.

`participatingForms()` includes all expressed contributions, across roles. Provisional burden retains that broader meaning: intensity averages total strength per contributing locus (zero if none), complexity counts distinct contributing Forms plus operation/topology, and persistence reflects enclosure. This is structural burden, not just manifested force.

The future workbench's Form selection, mediator transformations, recipient compatibility/resonance, and persistent stabilizer behavior remain open gameplay questions.

## Run tests

Run the domain and persistence tests without launching Minecraft:

```bash
./gradlew :domain:test :common:test
```

Run world interaction tests in a headless Minecraft server:

```bash
./gradlew :neoforge:runGameTestServer -PgameTests
```

The `gameTests` property enables test-only sources and resources. Build without that property when producing a jar to install or distribute. `./build-all.sh` runs the unit tests and builds both normal loader jars.
