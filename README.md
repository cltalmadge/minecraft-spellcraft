# Spellcraft Prototype Scaffold

A small Minecraft 26.3 multi-loader scaffold for an Oblivion-style magic system.

## Project structure

This repository is now **one Gradle build** with four real subprojects:

```text
zoltans-spellcraft/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── domain/      # pure Java rules and fast unit tests
├── common/      # shared Minecraft-facing code; real IDE/LSP project
├── fabric/      # thin Fabric adapter and distributable jar
└── neoforge/    # thin NeoForge adapter and distributable jar
```

Dependency direction remains:

```text
domain
  ↑
common
 ↑   ↑
fabric neoforge
```

`common/src/main/java` is owned by the real `:common` Gradle subproject. It has a Minecraft 26.3 Loom development classpath so IntelliJ and JDTLS can resolve `net.minecraft.*`, report semantic errors, autocomplete symbols, and navigate into generated Minecraft sources.

Fabric and NeoForge do **not** claim `common/` as a second source root. Instead they consume the shared source directories through Gradle configurations and compile those sources into their respective final jars. This is the standard multi-loader pattern and avoids the "non-project file" problem caused by sideways `../common/src` source-set wiring.

## Requirements

- JDK 25
- internet access for the first dependency download
- Prism Launcher for manual game testing

The root Gradle launcher uses Gradle 9.7.1. All subprojects share that one Gradle invocation and one set of version properties.

## Importing into IntelliJ or Zed

Open the **repository root**, not an individual loader directory.

First prime the Gradle model and Minecraft sources:

```bash
./gradlew :common:compileJava
./gradlew :common:genSources
```

Then refresh/reimport the root Gradle project in IntelliJ, or restart/clear JDTLS in Zed if it still has the old workspace cached.

Open:

```text
common/src/main/java/dev/spellcraft/prototype/minecraft/items/SpellbookItem.java
```

The following import should be a normal project dependency:

```java
import net.minecraft.world.item.Item;
```

Ctrl-click / go-to-definition on `Item` should navigate to Minecraft source once Loom's generated sources are attached/imported.

## Fast development commands

```bash
# Pure rules
./gradlew :domain:test

# Check shared Minecraft-facing code
./gradlew :common:compileJava

# Generate Minecraft sources for IDE navigation
./gradlew :common:genSources

# Loader builds
./gradlew :fabric:build
./gradlew :neoforge:build

# Everything
./build-all.sh
```

Expected distributable jars:

```text
fabric/build/libs/spellcraft-fabric-0.1.0.jar
neoforge/build/libs/spellcraft-neoforge-0.1.0.jar
```

Do not install the `common` or `domain` artifacts into Minecraft; they are development/shared-code projects.


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
./gradlew :fabric:build
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
./gradlew :neoforge:build
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
./gradlew :fabric:build
./tools/install-prism.sh fabric "/path/to/instance/.minecraft/mods"
```

Then restart the instance in Prism.

You can also symlink the built jar manually on Linux, but copying is initially less surprising because some build operations replace output files.

---

# Development runs without Prism

Prism is useful for testing the actual distributable jar, but the Gradle loader projects can also launch development clients.

Fabric:

```bash
./gradlew :fabric:runClient
```

NeoForge:

```bash
./gradlew :neoforge:runClient
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

The domain project uses the same Java 25 toolchain as the rest of the repository. It remains cheap to test because it has no Minecraft or loader dependencies.

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
./gradlew :domain:test --tests '*DefaultSpellCostModelTest'
```

Run the full domain suite:

```bash
./gradlew :domain:test
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
