# Arcane Workbench vertical slice

## Architecture

```text
Minecraft use-block / Divider use-on intent
  → ArcaneWorkbenchBlock / DividerItem
  → ArcaneWorkbenchBlockEntity (authority + lifecycle)
  → WorkbenchInteractions (physical interaction routing)
  → ArcaneWorkbenchState (items, stable IDs, points, arrows, membership, page)
  → WorkbenchWorkingAdapter
  → ArcaneWorking → WorkingAnalyzer → AnalysisResult / SpellPattern
      ├─ WorkbenchFeedback → activation sound / server particles
      └─ WorkbenchRecording → SpellCompiler → SpellProgram → RecordedSpell
           → existing SpellComponents.RECORDED_SPELL on spell_page
           → SpellPageBinding → WandItem → existing CastService / MinecraftSpellRuntime
```

The domain module is unchanged. The workbench never authors roles, Number, operation, or Geometry. MaterialProfileResolver remains the material boundary, and the adapter passes saved expressed contributions independently from intrinsic profiles. Unknown materials are permitted as inert physical anchors.

## Physical controls and coordinates

The block has no orientation property. Logical X runs west to east and logical Y runs north to south; the apparatus always has its control strip on the south edge. `WorkbenchCoordinates` maps the dark top rectangle to 9×9 points and supplies the inverse mapping for rendering. No slot grid is drawn.

Facing north, the control strip is **green enclosure, silver activation, gold recording, pale page receptacle**, left to right. Controls take precedence over drafting interactions. Divider takes precedence over material placement. Empty-hand sneak recovers a locus; an occupied point rejects placement without consuming anything. The Divider selects A, then creates A → B; sneaking on the second click erases that specific arrow. Reverse arrows are independent. The enclosure control selects all current loci as an explicit membership list; partial/missing membership is rejected by state validation. Changing loci clears the boundary.

The page control accepts one blank page. Empty hand retrieves it. Recording is a separate empty-hand click on the gold plate. Empty-hand activation never records or consumes ingredients. A 10-tick activation cooldown limits repeated effects.

## Authority, lifecycle, and persistence

Vanilla block/item interaction packets are sufficient; there are no custom intent payloads. The common server handler checks player world, proximity using Minecraft's block interaction range, build permission, spectator status, current BE identity, top face, and local hit bounds. It reads the real held item and resolves all profiles on the server. Client highlights only predict targeting.

Every successful persistent interaction increments the state revision, rebuilds the complete working, reanalyzes, marks the BE dirty, and sends a vanilla block-entity update. Activation and recording also refresh analysis. Loading physical state recomputes analysis; derived analysis is never serialized.

`WorkbenchPersistence` uses 26.3 ValueInput/ValueOutput. Saved fields are local monotonic ID counter, revision, item stacks (including components), logical positions, expressed Forms/resolution flag, directed relations, explicit boundary IDs, and page. An item stored at a locus is copied on ingress and access. IDs do not shift after removal. Block removal drains contents exactly once using vanilla container drops. Chunk unloading does not drain the board.

Malformed relations/boundaries are discarded with warnings. Duplicate IDs/positions or unusable coordinates preserve decoded stacks in a recovery list dropped when the block is broken. Undecodable ItemStack data cannot be reconstructed and is logged. Malformed expression lists are decoded strictly, never partially interpreted as a different spell: their material stays unresolved. A saved expression exceeding the current material profile also becomes unresolved without modifying the item or expression. Gaining new intrinsic Forms does not rewrite saved contributions. The resolver is still the existing bootstrap map; future reload plumbing can supply a replacement map without changing the persistence format.

Divider intent lives in a synchronized **nonpersistent** item component: dimension, block position, source ID, loaded-BE session token, and expiration. Inventory ticking cancels invalid or distant selections; completing a stroke validates again. The first click does not change the board revision. Session tokens identify loaded BEs, not semantic loci. No global player-state map exists. Feedback is a short server particle/sound event and is not saved.

## Feedback and development inspection

| Classification | Current sensory response |
| --- | --- |
| COHERENT | High clear chime; end-rod sparks at loci and along strokes |
| UNCONTAINED_INFLUENCE | Middle chime; fading smoke at loci and along strokes |
| UNSTABLE_STRUCTURE | Low chime; denser irregular electric sparks |
| AMBIGUOUS_STRUCTURE | Weak lower chime and ash sputter |
| NO_ACTIVE_FORM / EMPTY_WORKING | Quiet stone click, no energetic particles |
| INCOMPLETE_RELATION | Distinct pitch and localized ash at participating loci |
| Expression unresolved | Ash response plus short unresolved message; no pattern or recording |

When multiple diagnostics coexist, invalid states take priority, then instability, then uncontained influence, then coherence. The original full diagnostics remain available for inspection. Add JVM property `-Dspellcraft.workbench.debug=true` to the server, then sneak + empty-hand click the activation plate to print revision, principle, operation, geometry, roles, material identities, expressed Forms, and diagnostics in chat. Normal gameplay never displays semantic enum names.

## Recording and rendering

A present pattern is recordable even when unstable/uncontained; `SpellCompiler` preserves the actual semantics and determines program instructions. A missing pattern or unresolved contribution cannot record. Recorded pages are never overwritten or accepted into the blank-page receptacle. The page's existing Data Component codec preserves the program across save/load and network sync. Binding a recorded offhand page through sneak-use of a wand copies that same recording into the vessel. DiscoveryFixtures now lives only in GameTest sources for isolated runtime tests.

`ArcaneWorkbenchRenderer` uses Minecraft 26.3 render-state extraction and submit APIs in client-separated common code. It resolves arbitrary ItemStack models in FIXED context, with stationary small models above the surface. Directed lines have arrowheads and reciprocal offsets. Explicit membership determines an outlined enclosing rectangle; the drawing never computes semantics. Hover and selected-source outlines use synchronized BE state and tool intent. Activation particles are vanilla networked events. The model uses vanilla dark-stone/copper textures and distinct control plates; final art and shaders are deferred. Automated server tests do not visually verify the client renderer.

Fabric and NeoForge each register the workbench block/item, BE type, Divider item, nonpersistent selection component, and client renderer. All interaction, state, analysis, recording, binding, and rendering behavior is common. Recipes provide survival access to the workbench, Divider, blank page, and wand. There is no chest-style menu.

## Verification

Unit tests cover grid center/corners/edges and side rejection; stable IDs and item ownership; occupancy; removal and dangling-arrow cleanup; boundary validation; revision and physical save/load; corruption repair and strict expression decoding; intrinsic/expression separation and profile changes; direction/role preservation; Monad, Dyad, straight Triad, spoke Tetrad; and feedback classifications.

Workbench GameTests cover server-side placement and recovery; occupied-slot rejection; full-inventory drops; block removal contents; directed creation and erasure; reverse roles; explicit unstable enclosure; distance/self/duplicate/cross-table validation and selection cancellation; recording guards; and the full physical-state → analysis → page → BE reload → page retrieval → wand binding → Heat manifestation path. The preexisting runtime GameTests now explicitly initialize their test-only fixtures.

Run:

```bash
./gradlew :domain:test
./gradlew :common:test
./gradlew :common:compileJava
./gradlew :fabric:build
./gradlew :neoforge:build
./gradlew :neoforge:runGameTestServer -PgameTests
```

Build without `-PgameTests` for distribution. The tests exercise the real save/load serialization path but do not simulate a process restart or visually inspect either client's renderer. No domain semantics, multi-Form isolation UX, mediation rules, sustained fields, progression, notebook, or advanced vessels are introduced by this slice.
