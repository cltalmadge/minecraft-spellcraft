# Spellcraft

Spellcraft is a Minecraft Java Edition 26.3 mod for discovering magic through relationships between Form, Number, and Geometry. A spell is a stable structure compiled from a physical experiment, not a named ability with configured effects.

```text
ArcaneWorking → WorkingAnalyzer → SpellPattern → SpellCompiler → SpellProgram
                                                                    ↓
                                                             RecordedSpell
                                                                    ↓
                                                  source / conduit / vessel checks
                                                                    ↓
                                                   MinecraftSpellRuntime → Form handlers
```

Dependencies flow inward: `fabric` and `neoforge` consume `common`, which consumes `domain`. Loader modules compile shared sources into their own distributions.

- `domain`: pure Java values, construction analysis, compilation, burden, containment. No Minecraft or loader imports. Its packages follow the spell lifecycle:

  ```text
  form / material → working → pattern → program → manifestation / vessel
  ```

  `form` defines magical qualities, `material` describes participation in them,
  `working` owns physical constructions and analysis, `pattern` owns inferred
  meaning, `program` owns portable execution data, `manifestation` owns burden
  and sources, and `vessel` owns containment compatibility.
- `common`: material correspondence, codecs, item components, authoritative casting, targeting, Heat/Motion manifestation, presentation snapshots.
- `fabric` / `neoforge`: component/item registration and platform entry points. Magical rules are shared.

## The first grammar

`FormId` and `MaterialId` are validated, extensible namespaced identities. Heat and Motion are the only bootstrap Forms. `MaterialProfile` holds immutable, sorted, unique `FormParticipation` entries. Strength ranges from 0 (absent) to 1 (full participation); it is an internal semantic quantity, not a promised player-facing stat.

`ArcaneWorking` stores immutable logical-grid nodes, directed strokes, and explicit boundary membership. Materials without active participation can be geometric anchors. IDs and strokes are canonically ordered, so reordering input collections does not change analysis or compilation.

| Physical construction | Inferred Number | Operation / geometry |
| --- | --- | --- |
| One active locus | Monad | Concentration / point |
| Two active loci joined by one directed stroke | Dyad | Transfer / directed line |
| Three active loci in a forward, straight directed chain | Triad | Mediation / line |
| One active center with four inactive, equally distant cardinal anchors | Monad | Concentration / radial, inward or outward from strokes |
| Four equivalent active cardinal arms around an inactive center, with uniformly directed spokes | Tetrad | Stabilization / intersection |
| The tetradic construction with a boundary enclosing every node | Tetrad | Stabilization / enclosure, sustained |

Four unrelated nodes do **not** imply Tetrad. Bent chains, asymmetric arms, mixed spoke direction, partial/nested boundaries, duplicate/coincident nodes, and unknown references are rejected. More general triangles, polygons, and compound numerical relationships are deliberately outside this first grammar.

Analysis returns a pattern plus diagnostics, or structured failure diagnostics. Unenclosed relationships can release transient influence with `UNCONTAINED_INFLUENCE`; enclosure without tetradic stability fails with `UNSTABLE_STRUCTURE`. The domain contains no final UI strings. Diagnostics reserve optional node/Form context for later workbench feedback.

Form participation is averaged across active loci. A mixed working retains both Forms; a symmetrical repetition stabilizes structure without arbitrarily multiplying intensity. This is a provisional explicit law, not a balance claim.

`SpellPattern` retains the observed numerical principle and derives its operation. `SpellCompiler` emits:

```text
Invoke(FormParticipation)+ → Operate → Shape → Release | Sustain
```

These are domain instructions, not a bytecode VM. The program validates grammar, versions, unique invocations, and containment/release consistency. Heat and Motion use identical transfer/shape/release instructions. Radial Heat changes the operation and geometry. Contained Heat changes stability, geometry, and persistence. No spell name selects behavior.

## Runtime and its limits

The experimental wand reads `spellcraft:recorded_spell` from the authoritative held stack. `CastService` checks the caster, cooldown, vessel, and source before invoking the runtime. Origin, aim, wall occlusion, and recipients are resolved on the server. Casting does not rerun working analysis.

- Point selects the caster.
- Line finds the nearest living recipient along a 16-block ray, clipped against blocks.
- Radial selects visible living recipients within four blocks, and derives outward/inward impulse from the center.
- Concentration applies the influence at each selected locus; transfer divides it among recipients.
- Heat ignites entities and emits flame particles.
- Motion adds a directional impulse and emits cloud particles; server-player velocity is explicitly synchronized.

Mediation and stabilization are analyzed, compiled, and persisted, but **their temporal world execution is not implemented**. The runtime reports `UNSUPPORTED_OPERATION` before applying anything. In particular, `Sustain` is not silently treated as a one-shot cast. Persistent fields, transformation stages, and block interactions remain future work.

`SpellBurden` separates intensity, complexity, and persistence. `BurdenModel` derives them from program semantics. `VesselProfile` checks those axes plus Form affinity and directional compatibility. The wand preserves a program and acts as a directional conduit; the caster's spirit is the source. Successful casts apply a brief recovery cooldown and provisional food exhaustion. A page preserves the same recording but cannot cast. These are seams for future source and fatigue models, not a mana economy or final balance.

## Persistence and materials

`RecordedSpell` and `SpellProgram` each have schema version 1. The `spellcraft:recorded_spell` Data Component has explicit tagged `Codec` and network `StreamCodec` support. Saves contain versions, a recording name, and instructions; they never contain implementation class names. Unsupported versions/tags/invalid grammar return codec errors. There is no migration engine yet.

Both loaders register the same component type before creating item defaults. The bootstrap wand and page contain the same analyzed and compiled Heat experiment. No per-spell items are registered. `DiscoveryFixtures.directedMotion()` builds another working through the same analyzer/compiler, used by integration tests.

`MaterialProfileResolver` owns an immutable bootstrap correspondence map: blaze powder participates in Heat; feathers participate in Motion. It accepts Minecraft items/stacks and returns domain profiles. Future datapack reloads can supply correspondence data without changing analysis or compilation.

The old `EffectId`, `Delivery`, `SpellEffectSpec`, `SpellDefinition`, `CostProfile`, `SpellCostModel`, and `DefaultSpellCostModel` were unused by gameplay and have been removed. Their old cost tests were replaced with semantic, burden, and containment tests. The domain package is now `dev.spellcraft.domain`.

The spellbook item, read-only snapshots, and existing Fabric screen remain presentation-only. Its placeholder text describes observations; it is not a server knowledge store. NeoForge spellbook screen wiring remains outside this slice.

## Build and verification

Requires JDK 25. The wrapper supplies Gradle 9.7.1. Initial dependency resolution needs network access.

```bash
./gradlew :domain:test
./gradlew :common:compileJava
./gradlew :common:test
./gradlew :fabric:build
./gradlew :neoforge:build
./build-all.sh
```

`buildAll` includes domain tests, common codec tests, common compilation, and both loader builds.

World integration tests are opt-in and excluded from normal jars:

```bash
./gradlew :neoforge:runGameTestServer -PgameTests
```

They exercise actual item use with server-world mock players, Heat ignition, Motion impulse, block occlusion, cooldown, source/vessel gates, and portable page data. The Java tests are in `common/src/gametest`; only registration is NeoForge-specific. A test-only empty laboratory structure is provided.

Domain tests cover the primitive grammar, malformed constructions, deterministic compilation, mixtures, immutable values, program/schema validation, burden, and qualitative vessel compatibility. Common tests cover JSON and stream round trips and malformed/unknown persisted values.

Normal installable artifacts:

```text
fabric/build/libs/spellcraft-fabric-0.1.0.jar
neoforge/build/libs/spellcraft-neoforge-0.1.0.jar
```

Install only the matching loader jar. Fabric also requires Fabric API. Do not install the domain/common development jars. After an opt-in GameTest build, run a normal build before distributing jars.

For a development client:

```bash
./gradlew :fabric:runClient
# or
./gradlew :neoforge:runClient
```

In a test world:

```mcfunction
/give @s spellcraft:wand
/give @s spellcraft:spell_page
/give @s spellcraft:spellbook
```

Aim the wand at a living entity and use it. It releases the recorded thermal relation; walls block the transmission. The page stores the same program and has no use action. Custom recordings can be supplied through the component format; no binding UI exists yet.

## Next vertical slice

Build an Arcane Workbench block entity that stores material placement and drawn relationships, analyzes them on the server, presents diagnostics through light/sound/material response, and records a successful program onto a page. Final GUI polish, progression, research notebooks, additional Forms, general graph algebra, datapack reload infrastructure, and sustained field execution are intentionally deferred.
