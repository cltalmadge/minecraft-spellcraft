# Spellcraft

Spellcraft is a Minecraft 26.3 mod prototype for an Oblivion-style magic system: configurable effects, composed spells, player spellbooks, and eventually in-game spell creation.

The project targets both Fabric and NeoForge from a single Gradle multi-project build. Gameplay rules are separated from Minecraft integration, while loader-specific projects are kept as thin adapters.

## Architecture

```text
zoltans-spellcraft/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── domain/
├── common/
├── fabric/
└── neoforge/
```

Dependencies flow inward:

```text
domain
  ↑
common
 ↑   ↑
fabric neoforge
```

### `domain`

Pure Java gameplay rules.

This module is intended to contain the parts of Spellcraft that do not depend on Minecraft:

- spell definitions and composition;
- effect definitions;
- spell validation;
- cost calculation;
- magic schools;
- player knowledge and unlocks;
- stacking rules;
- compiled spell representations;
- semantic result types.

It must not depend on:

```text
net.minecraft.*
net.fabricmc.*
net.neoforged.*
```

Keeping this layer independent makes most of the magic system cheap to unit test without booting Minecraft.

### `common`

Shared Minecraft-facing implementation.

This module may depend on vanilla Minecraft classes:

```text
net.minecraft.*
```

but not on either loader API.

Expected responsibilities include:

```text
items
casting
target resolution
effect execution
active effects
Minecraft codecs
shared packet payloads
spellbook services
commands
menus
```

`common` is a real Gradle subproject with its own Minecraft development classpath. Both loader projects consume its source when producing their distributable jars.

### `fabric`

Fabric-specific integration:

```text
bootstrap
registrations
Fabric events
network registration
player lifecycle integration
client hooks
```

### `neoforge`

NeoForge-specific integration:

```text
bootstrap
registrations
NeoForge events
network registration
player lifecycle integration
client hooks
```

Loader projects should adapt platform APIs to shared functionality rather than contain gameplay rules.

If spell costs, spell legality, targeting rules, or other game semantics begin diverging between `fabric` and `neoforge`, they belong elsewhere.

## Requirements

- JDK 25
- Gradle 9.7.1 via the included wrapper
- Minecraft Java Edition 26.3
- Internet access for initial dependency resolution

Prism Launcher is useful for testing built artifacts but is not required for Gradle development runs.

## Building

Run Gradle through the included wrapper.

### Domain tests

```bash
./gradlew :domain:test
```

### Shared Minecraft code

```bash
./gradlew :common:compileJava
```

Generate Minecraft sources for IDE navigation:

```bash
./gradlew :common:genSources
```

### Fabric

```bash
./gradlew :fabric:build
```

Output:

```text
fabric/build/libs/spellcraft-fabric-0.1.0.jar
```

### NeoForge

```bash
./gradlew :neoforge:build
```

Output:

```text
neoforge/build/libs/spellcraft-neoforge-0.1.0.jar
```

### Everything

```bash
./build-all.sh
```

`domain` and `common` are development modules, not standalone Minecraft mods. Only the Fabric or NeoForge artifacts should be installed into a game instance.

## Development clients

Both loader projects can launch directly through Gradle.

Fabric:

```bash
./gradlew :fabric:runClient
```

NeoForge:

```bash
./gradlew :neoforge:runClient
```

These runs are useful for normal development. Testing the generated jar in a launcher remains useful for catching packaging and metadata problems that a development run may not expose.

## Installing a Fabric build

Create a Minecraft 26.3 Fabric instance and install Fabric API.

Build Spellcraft:

```bash
./gradlew :fabric:build
```

Install:

```text
fabric/build/libs/spellcraft-fabric-0.1.0.jar
```

into the instance's `mods` directory alongside Fabric API.

The startup log should contain:

```text
Spellcraft common Minecraft layer initialized through fabric
```

## Installing a NeoForge build

Create a Minecraft 26.3 NeoForge instance.

The project currently targets:

```text
NeoForge 26.3.0.10-beta
```

Build Spellcraft:

```bash
./gradlew :neoforge:build
```

Install:

```text
neoforge/build/libs/spellcraft-neoforge-0.1.0.jar
```

into the instance's `mods` directory.

The startup log should contain:

```text
Spellcraft common Minecraft layer initialized through neoforge
```

## Current implementation

The initial multi-loader scaffold is operational.

The shared Minecraft layer currently provides a custom spellbook item:

```text
spellcraft:spellbook
```

It can be spawned with:

```mcfunction
/give @s spellcraft:spellbook
```

The item is implemented in `common` and registered through the loader-specific projects. It currently uses the vanilla book appearance.

The bootstrap path therefore exercises the intended dependency structure:

```text
loader bootstrap
      ↓
loader item registration
      ↓
shared SpellbookItem
      ↓
vanilla Minecraft item API
```

The shared bootstrap also logs through Minecraft's runtime environment to verify that common Minecraft-facing code is present in both loader builds.

## Design direction

Spellcraft is intended to model magic as reusable primitive effects composed into spells rather than as a hierarchy of hard-coded spell classes.

For example:

```text
effect definition
    damage_health

configured effect
    magnitude = 6
    duration  = 0
    delivery  = RAY

spell
    one or more configured effects
```

A player-created spell is runtime data, not a dynamically registered Minecraft item or registry entry.

The long-term division is:

```text
domain
    spell semantics
    validation
    cost models
    knowledge
    composition

common
    Minecraft execution
    items
    targets
    active effects
    persistence-facing services
    menus
    networking DTOs

fabric / neoforge
    platform integration only
```

Server-side execution will remain authoritative. Clients should submit cast or spellcraft intent; the server determines validity, cost, targets, and resulting effects.

## Near-term roadmap

The current spellbook item establishes the first in-game object owned by the magic system.

The next slices are expected to be:

```text
spellbook interaction
    ↓
spellbook screen
    ↓
spell data model
    ↓
basic effect catalog
    ↓
single-effect spell
    ↓
server-authoritative casting
    ↓
spell construction UI
```

The goal is to keep each step independently runnable and testable rather than build the complete system behind an unfinished interface.

## Versioning

Minecraft mod tooling changes independently across Fabric, NeoForge, Loom, ModDevGradle, and Minecraft itself.

Version-specific configuration therefore stays inside the relevant loader project wherever possible. Changes required by one loader should not force architectural changes into `domain` or `common`.
