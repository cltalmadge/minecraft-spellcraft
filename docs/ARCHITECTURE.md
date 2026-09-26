# Architecture

The repository is one Gradle multi-project build:

```text
domain/      pure Java rules
common/      shared Minecraft-facing code
fabric/      Fabric adapter / distributable
neoforge/    NeoForge adapter / distributable
```

Dependency direction:

```text
domain
  ↑
common
 ↑   ↑
fabric neoforge
```

## Domain

`domain` must not import Minecraft, Fabric, or NeoForge. It is the fast unit-test boundary for spell rules, validation, cost calculation, effect definitions, progression semantics, and other deterministic gameplay logic.

## Common

`common` is a real Gradle subproject with a Minecraft development classpath. It owns `common/src/main/java`, which gives IntelliJ/JDTLS normal project semantics and Minecraft source navigation.

Common may import `net.minecraft.*` but must not import Fabric or NeoForge APIs.

## Loader projects

Fabric and NeoForge are thin adapters. They consume the exported shared source directories from `domain` and `common` and compile those sources directly into each loader jar. They do not add `../common/src` or `../domain/src` as external source roots.

This keeps source ownership unambiguous for IDEs while still producing self-contained loader jars.

Loader code should be restricted to bootstrap, lifecycle/event wiring, registration, networking glue, client hooks, and platform-specific persistence integration.
