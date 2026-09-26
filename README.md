# Spellcraft Prototype Scaffold

A deliberately small cross-loader scaffold for an Oblivion-style Minecraft magic system.

**Target at scaffold creation:** Minecraft Java **26.3**.

The project is arranged so the loaders are adapters around shared gameplay code:

```text
domain/                pure Java rules + fast unit tests
common/                shared Minecraft-facing gameplay source
fabric/                Fabric bootstrap/build
neoforge/              NeoForge bootstrap/build
```

The Fabric and NeoForge builds are intentionally isolated. Both compile the **same** `domain/` and `common/` source trees into their platform jar, but each loader keeps its own Gradle wrapper and toolchain plugin versions. This avoids forcing Fabric and NeoForge to agree on the same Gradle wrapper version.

## What exists right now

This is a scaffold, not a finished magic mod.

It contains:

- a pure Java `EffectId`, `Delivery`, `SpellEffectSpec`, `CostProfile`, and `DefaultSpellCostModel`;
- JUnit tests demonstrating the intended fast TDD loop;
- one shared Minecraft class that uses vanilla `Component` to prove common Minecraft code compiles on both loaders;
- a Fabric entrypoint;
- a NeoForge entrypoint;
- build scripts for both loader jars;
- a Prism Launcher copy helper.

The first useful development loop is therefore:

```text
change rule
   ↓
cd domain && ./gradlew test
   ↓
change Minecraft integration
   ↓
build one loader
   ↓
copy jar into a Prism test instance
```

## Requirements

Minecraft 26.3 targets **Java 25**. The loader projects are configured with Java toolchains and the Foojay resolver, so Gradle can resolve the requested toolchain when necessary. If you already have a JDK 25 installed, Gradle can use it directly.

You also need:

- an internet connection for the first Gradle dependency download;
- Prism Launcher for the manual game loop described below.

### Gradle launchers in this scaffold

The included `gradlew` / `gradlew.bat` files are small bootstrap launchers that download the exact Gradle distribution pinned for each loader. They serve the same practical purpose as a wrapper for this scaffold without sharing one Gradle version across both loader projects.

- `domain/` and `fabric/`: Gradle 9.7.1
- `neoforge/`: Gradle 9.2.1

On Linux/macOS they require `curl` or `wget` plus `unzip`. On Windows they use PowerShell.

## Fastest sanity check

Run the pure domain tests first:

```bash
cd domain
./gradlew test
```

On Windows:

```powershell
cd domain
.\gradlew.bat test
```

These tests should remain the default place for cost formulas, spell validation, effect constraints, school selection, spell compilation, and similar rules.

## Build the Fabric jar

```bash
cd fabric
./gradlew build
```

Expected mod jar:

```text
fabric/build/libs/spellcraft-fabric-0.1.0.jar
```

Ignore any `-sources.jar` artifact when installing into Minecraft.

The Fabric project is pinned to:

```text
Minecraft:   26.3
Fabric Loader: 0.19.5
Fabric API:  0.161.0+26.3
Loom:        1.18-SNAPSHOT
```

## Build the NeoForge jar

```bash
cd neoforge
./gradlew build
```

Expected mod jar:

```text
neoforge/build/libs/spellcraft-neoforge-0.1.0.jar
```

The NeoForge project is pinned to:

```text
Minecraft: 26.3
NeoForge:  26.3.0.10-beta
ModDevGradle: 2.0.147
```

## Build everything

From the repository root:

```bash
./build-all.sh
```

That runs:

```text
domain tests
→ Fabric build
→ NeoForge build
```

The loader builds are separate on purpose, so `build-all.sh` simply invokes each wrapper in sequence.

---

# Loading it into Minecraft with Prism Launcher

Use **two separate Prism instances**. Do not try to put the Fabric and NeoForge jars into the same instance.

## 1. Create a Fabric test instance

In Prism Launcher:

1. Click **Add Instance**.
2. Create a Minecraft **26.3** instance.
3. Select/install **Fabric** as the mod loader.
4. Open the instance's **Edit** window.
5. Open **Mods**.
6. Use **Download Mods** and install **Fabric API** for Minecraft 26.3.

The scaffold currently declares Fabric API as a required dependency.

Build the mod:

```bash
cd fabric
./gradlew build
```

Then in Prism:

1. Edit the Fabric instance.
2. Open **Mods**.
3. Click **Add file** or drag the jar into the Mods view.
4. Add:

```text
fabric/build/libs/spellcraft-fabric-0.1.0.jar
```

Launch the instance.

A successful smoke test currently means:

- Minecraft reaches the title screen;
- Fabric reports `Spellcraft Prototype` as loaded;
- the log contains a message similar to:

```text
Spellcraft common Minecraft layer initialized through fabric
```

There is intentionally no gameplay UI yet.

## 2. Create a NeoForge test instance

In Prism Launcher:

1. Click **Add Instance**.
2. Create a Minecraft **26.3** instance.
3. Select/install **NeoForge** as the mod loader.
4. Prefer the same NeoForge version pinned by this project (`26.3.0.10-beta`) while doing initial scaffold verification. A newer compatible 26.3 NeoForge may also work, but pinning removes one variable while debugging.

Build the mod:

```bash
cd neoforge
./gradlew build
```

Then in Prism:

1. Edit the NeoForge instance.
2. Open **Mods**.
3. Click **Add file** or drag the jar into the Mods view.
4. Add:

```text
neoforge/build/libs/spellcraft-neoforge-0.1.0.jar
```

Launch the instance.

Successful smoke test:

- Minecraft reaches the title screen;
- NeoForge lists `Spellcraft Prototype`;
- the log contains:

```text
Spellcraft common Minecraft layer initialized through neoforge
```

NeoForge does **not** need a separate Fabric-API-style dependency for this scaffold.

---

# Faster Prism iteration on Linux

Prism's instance path depends on how Prism was installed. Common roots include:

```text
~/.local/share/PrismLauncher/instances/
~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/   # Flatpak
```

Do not guess if you are unsure. Prism can open its folders for you from the instance editor / folder shortcuts.

Once you know the exact `mods` folder, this repository has a helper:

```bash
./tools/install-prism.sh fabric  "/absolute/path/to/Fabric Test/.minecraft/mods"
./tools/install-prism.sh neoforge "/absolute/path/to/NeoForge Test/.minecraft/mods"
```

The script copies the newest non-sources jar for that loader into the supplied `mods` directory.

Typical loop:

```bash
cd fabric
./gradlew build
cd ..
./tools/install-prism.sh fabric "/path/to/instance/.minecraft/mods"
```

Then restart the instance in Prism.

You can also symlink the built jar manually on Linux, but copying is initially less surprising because some build operations replace output files.

---

# Development runs without Prism

Prism is useful for testing the actual distributable jar, but the Gradle loader projects can also launch development clients.

Fabric:

```bash
cd fabric
./gradlew runClient
```

NeoForge:

```bash
cd neoforge
./gradlew runClient
```

Use dev runs for quick integration debugging. Use Prism periodically to verify the **built jar** behaves like an actual installed mod.

---

# Project boundaries

## `domain/`

Pure Java only.

This is where the bulk of the magic system should live:

```text
spell composition
spell drafts
validation
cost calculations
schools
knowledge/unlocks
compiled spell representation
stacking rules
semantic result types
```

Do not import:

```text
net.minecraft.*
net.fabricmc.*
net.neoforged.*
```

The standalone domain build deliberately targets Java 21 so it can remain cheap to test on ordinary development JDKs. The same source is recompiled as part of each Java-25 Minecraft mod build.

## `common/`

Shared Minecraft-facing gameplay source.

This layer may import vanilla Minecraft classes:

```text
net.minecraft.*
```

It should not import Fabric or NeoForge APIs.

This is where later code should live for:

```text
CastEngine
TargetResolver
EffectExecutor implementations
ActiveEffectManager
Minecraft codecs
shared packet DTOs
spellbook services
commands
```

## `fabric/`

Thin Fabric adapter.

Keep it to:

```text
bootstrap
Fabric event hooks
payload registration
player lifecycle glue
client key registration
```

## `neoforge/`

Thin NeoForge adapter.

Keep it to:

```text
bootstrap
NeoForge event hooks
payload registration
player lifecycle glue
client registration
```

If spell cost or spell legality ever diverges between these two folders, the architecture has drifted.

---

# TDD loop

The included domain test demonstrates the intended workflow.

Run one test class:

```bash
cd domain
./gradlew test --tests '*DefaultSpellCostModelTest'
```

Run the full domain suite:

```bash
./gradlew test
```

Recommended implementation order from here:

```text
1. effect constraints
2. spell draft
3. structured validation issues
4. effect catalog
5. effect knowledge
6. compiled spell
7. school resolver
8. Minecraft cast context
9. SELF target resolver
10. RAY target resolver
11. damage executor
12. heal executor
13. mana state
14. active timed effect manager
15. loader networking adapters
```

Keep the first eight-ish steps dominated by pure unit tests.

---

# Current smoke-test behavior

Both loaders call the same shared method:

```java
MinecraftMagic.initialize("fabric");
MinecraftMagic.initialize("neoforge");
```

That common class creates a vanilla Minecraft `Component` and logs through SLF4J. It exists primarily to prove that:

```text
one shared Minecraft source tree
→ compiles under Fabric
→ compiles under NeoForge
```

The next sensible slice is to add a `/spellcraft debug` command in shared Minecraft code and have each loader expose only whatever registration hook is required.

## Version notes

These versions were verified against the upstream 26.3 templates on 2026-09-26. Minecraft/loader build tooling moves quickly; if a dependency disappears or a newer upstream MDK changes its required Gradle version, update only the affected loader project rather than coupling the two builds together.
