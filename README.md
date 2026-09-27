# Spellcraft

Spellcraft is a Minecraft Java Edition 26.3 mod about discovering magic through experimentation. Its design centers on three ideas: Form describes the quality being manipulated, Number describes the relationship, and Geometry describes how that relationship is arranged. Materials participate in Forms, and their arrangement gives a spell its meaning.

The mod supports Fabric and NeoForge. Players construct experiments on an Arcane Workbench, record the analyzed pattern onto a spell page, and bind it to a wand. The same domain analyzer and compiler determine the spell’s meaning. The spellbook still contains a sample observation.

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

## Construct and cast your first spell

Give yourself the apparatus and materials, or use their crafting recipes:

```mcfunction
/give @s spellcraft:arcane_workbench
/give @s spellcraft:divider
/give @s spellcraft:spell_page
/give @s spellcraft:wand
/give @s minecraft:blaze_powder
/give @s minecraft:iron_ingot
```

One workbench item places a **two-block-long table** extending away from you in your horizontal facing direction. Keep both positions clear. The control strip is nearest you, and one continuous dark drafting surface spans both halves. Its logical 9×9 grid is invisible; the hover marker shows a placement point. Either half addresses the same workstation. Old single-block prototype tables must be broken and replaced; their stored contents are recovered on breaking.

1. Place the workbench. Right-click two separate points on the dark surface with blaze powder and iron. One item moves from your hand onto each point.
2. Hold the Divider. Click blaze first, then iron: the arrow points **Blaze → Iron**. Click order supplies direction.
3. Empty your hand and click the **silver activation plate** (second control from the left). The open pair gives a tone and dissipating smoke along the relationship. Activation consumes no materials.
4. Click the **pale page receptacle** (rightmost control) with a blank spell page. Empty your hand and click the **gold recording plate** (third control). Click the receptacle empty-handed to retrieve the recorded page.
5. Put the recorded page in your offhand and hold the wand in your main hand. **Sneak + right-click in the air** to bind its program. Release sneak, aim at a living entity within 16 blocks, and right-click to cast Heat.

Solid blocks obstruct the cast. Successful casts have a one-second cooldown and cause food exhaustion; casting requires a nonempty food bar. Binding preserves the page and deliberately replaces any wand recording. New pages and wands are blank.

### Adjust the apparatus

| Interaction | Effect |
| --- | --- |
| Empty hands, sneak + right-click material | Recover that item; remove its strokes and clear the enclosure |
| Divider: click A, then sneak + click B | Erase only A → B |
| Divider: click an empty drafting point | Cancel the selected source |
| Divider on the green enclosure plate (leftmost control) | Enclose all currently placed loci |
| Sneak + Divider on the green plate | Clear the enclosure |
| Break either half | Remove both halves; drop one workbench plus stored materials, page and recovery contents (creative drops contents only) |

Adding/removing materials clears an existing enclosure; redraw it deliberately after changing membership. A selected Divider source expires after 30 seconds and cancels on distance, dimension, workbench replacement/removal, or locus removal. A full inventory causes recovered materials to drop beside you.

Feathers express Motion. Materials with no mapped Forms, including iron, serve as inert recipients/anchors. Exactly one intrinsic nonzero Form is expressed automatically. **Multi-Form isolation is unresolved:** those materials remain visible and recoverable, but block activation/recording until replaced. No Form selector exists.

An enclosed Dyad remains interpretable and recordable while unstable. Its recording preserves enclosure; the current runtime reports unsupported containment rather than treating it as an open transfer. Mediation and sustained stabilization execution remain deferred.

Detailed implementation and test notes: [Arcane Workbench](docs/arcane-workbench.md).

## Spellbook

On Fabric, right-click `spellcraft:spellbook` to open a sample observation about thermal transmission. The screen displays example content rather than tracking learned spells. On NeoForge, the spellbook item has no screen yet.

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

Multi-Form isolation, mediator transformations, recipient compatibility/resonance, and persistent stabilizer behavior remain open gameplay questions.

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
