# Arcane Workbench — two-block workstation

## Architecture

```text
Minecraft use-block / Divider use-on intent
  → ArcaneWorkbenchBlock / DividerItem
  → ArcaneWorkbenchLayout (pair validation, controller address, canonical hit)
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

One `spellcraft:arcane_workbench` item places `FRONT` at the target and `BACK` one block along the player's horizontal facing. Both states retain that `FACING`. Only BACK constructs an `ArcaneWorkbenchBlockEntity`; FRONT has no inventory or magical state. The common `ArcaneWorkbenchItem` checks replaceability, build height, world border, permission, loaded availability and collision, commits both positions before neighbor notifications, and rolls the first write back if the second fails. Item consumption happens only after successful placement. Pistons cannot move the apparatus.

`ArcaneWorkbenchLayout` owns controller/counterpart addressing, matching-part validation, rotation, world↔workstation conversion, reach, and finite rendering bounds. Canonical u runs left to right as seen from the front; v runs from the far/back edge toward the player. Both span 0..1 across the **complete** 1×2 footprint. The block seam is v=.5, not a second grid. `WorkbenchCoordinates` maps u=.05..95 and v=.05..77 to the unchanged 9×9 logical field (physically .9×1.44 blocks); v=.80..1 is the control strip. No slot grid is drawn. Logical coordinates, IDs, Forms, relations and spell semantics do not depend on facing or part.

Facing the back from the front, the control strip is **green enclosure, silver activation, gold recording, pale page receptacle**, left to right. Controls take precedence over drafting interactions. Divider takes precedence over material placement. Empty-hand sneak recovers a locus; an occupied point rejects placement without consuming anything. The Divider selects A, then creates A → B; sneaking on the second click erases that specific arrow. Reverse arrows are independent. The enclosure control selects all current loci as an explicit membership list; partial/missing membership is rejected by state validation. Changing loci clears the boundary.

The page control accepts one blank page. Empty hand retrieves it. Recording is a separate empty-hand click on the gold plate. Empty-hand activation never records or consumes ingredients. A 10-tick activation cooldown limits repeated effects.

## Authority, lifecycle, and persistence

Vanilla block/item interaction packets are sufficient; there are no custom intent payloads. The common server handler checks player world, proximity using Minecraft's block interaction range, build permission, spectator status, current controller BE identity, a complete matching loaded pair, interaction permission at both positions, top face, and canonical hit bounds. It reads the real held item and resolves all profiles on the server. Client highlights only predict targeting.

`ArcaneWorkbenchState` exposes public read/query methods only: collections are immutable and ItemStacks are copied. Mutations, restoration helpers, and `WorkbenchRecording` are package-private. Production mutation callers are the BE's validated interaction router, its recording helper, persistence reconstruction, and removal drain; same-package tests can construct fixtures directly. The BE still owns analysis refresh, dirty marking, and synchronization after interactions.

Every successful persistent interaction increments the state revision, rebuilds the complete working, reanalyzes, marks the BE dirty, and sends a vanilla block-entity update. Activation and recording also refresh analysis. Loading physical state recomputes analysis; derived analysis is never serialized. Page insertion, retrieval, and recording each increment revision once. Failed operations, selection, activation, inspection, and analysis refresh do not. Draining an already empty bench does not increment revision. Reconstruction may use relation/boundary validators, but finally restores the saved nonnegative revision; next ID is at least the saved counter and greater than every retained ID.

`WorkbenchPersistence` uses 26.3 ValueInput/ValueOutput. Saved fields are local monotonic ID counter, revision, item stacks (including components), logical positions, expressed Forms/resolution flag, directed relations, explicit boundary IDs, and page. An item stored at a locus is copied on ingress and access. IDs do not shift after removal. Block removal drains contents exactly once using vanilla container drops. Chunk unloading does not drain the board.

Malformed relations/boundaries are discarded with warnings. Duplicate IDs/positions or unusable coordinates preserve decoded stacks in a recovery list dropped when the block is broken. Undecodable ItemStack data cannot be reconstructed and is logged. Stack components are decoded strictly, including RecordedSpell: a partial component result never becomes an altered recording or an apparently blank page. Malformed expression lists are decoded strictly, never partially interpreted as a different spell: their material stays unresolved. Duplicate or inactive Form terms also retain their locus as unresolved, so dropping that locus cannot accidentally enable a different working. A saved expression exceeding the current material profile also becomes unresolved without modifying the item or expression. Gaining new intrinsic Forms does not rewrite saved contributions. The resolver is still the existing bootstrap map; future reload plumbing can supply a replacement map without changing the persistence format.

Restoration accepts exactly one `spellcraft:spell_page`, blank or validly recorded, and checks vanilla component compatibility. A decoded wrong item, incompatible component combination, or count greater than one moves the **entire stack** to the existing recovery tray, leaves the receptacle empty, and logs a warning. A compatible stack codec preserves positive oversized counts (including counts above vanilla's encoding limit of 99) in recovery across further saves. Missing count means one, as in vanilla. Zero, negative, non-numeric, or otherwise undecodable stack data is rejected and reported; no positive count or partial magical component is invented. Recovery stacks retain their components and are dropped on block removal.

Divider intent lives in a synchronized **nonpersistent** item component: dimension, **controller** block position, source ID, loaded-BE session token, and expiration. Inventory ticking cancels selections on missing/mismatched/unloaded halves, invalid sessions, removed loci, expiry or distance; completing a stroke validates again. The first click does not change the board revision. Session tokens identify loaded BEs, not semantic loci. No global player-state map exists. Feedback is a short server particle/sound event and is not saved.

## Teardown and chunk boundaries

BACK alone has workstation loot. The 26.3 loot schema uses singular `condition` with an `all_of` containing `match_block` on `part=back` and `survives_explosion` (the old prototype's plural `conditions` was ignored). Actual removal drains the controller exactly once via `BlockEntity.preRemoveSideEffects`; it never drains on `setRemoved`/unload. Materials, page and recovery stacks drop; relations and boundaries do not.

`affectNeighborsAfterRemoval` runs after the initiating block has been replaced. It removes only an exact matching loaded counterpart. Removing FRONT delegates normal loot to BACK; removing BACK removes FRONT without loot. A recursive callback sees the initiating position already absent, so cannot drop twice. Creative front breaking first removes BACK without workstation loot; both creative paths still return stored contents. Explosions use the same removal path; the controller's direct explosion loot uses vanilla explosion survival, while a front-triggered controller removal uses normal block loot. At most one workstation item is produced.

Neighbor shape notifications schedule validation; both parts also keep a persisted scheduled check every 20 ticks. This catches removals made without neighbor notifications and validates independently loaded structural halves. A check first tests `hasChunkAt`: an unloaded counterpart is **unknown**, not absent, and is never force-loaded. Interactions and Divider selections require both halves loaded. If the counterpart is genuinely missing after its chunk loads, the orphan is removed; only an orphan controller produces loot and contents. Scheduled validation survives normal chunk saving and loading, with no proxy BE or duplicate mutable state.

## Prototype worlds

There is no migration framework. Replace existing one-block prototype workbenches. Missing block-state properties decode to the default NORTH/BACK state, preserving the old controller's saved materials/page/recovery; an incomplete old table cannot be used. Break it to recover its item and contents, then place the new two-block item. A neighbor validation may also dismantle an incomplete old table safely. No extra block is silently placed into an existing world.

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

`ArcaneWorkbenchRenderer` uses Minecraft 26.3 render-state extraction and submit APIs in client-separated common code. It resolves arbitrary ItemStack models in FIXED context, with stationary small models above the surface. Directed lines have arrowheads and reciprocal offsets. Explicit membership determines an outlined enclosing rectangle; the drawing never computes semantics. Hover and selected-source outlines use synchronized BE state and tool intent. Activation particles are vanilla networked events. Static BACK/FRONT block models form a continuous copper-rimmed dark-stone slab with four outer legs and cross braces, matching rotated collision/outline shapes and nonopaque/pathfinding behavior. Dynamic content is submitted once from BACK. Shared layout transforms also orient hover and activation particles. The inventory model shows the full apparatus. No GeckoLib dependency was added.

Vanilla 26.3's `BlockEntityRenderer` exposes `shouldRenderOffScreen` and distance selection, but no per-renderer frustum AABB hook. The renderer uses the global submission path to avoid controller-chunk culling, with distance measured from a finite complete-footprint AABB (two blocks, height 1.25). Its same common class also supplies NeoForge's `getRenderBoundingBox` hook with that AABB. No infinite box is used. Final art and shaders are deferred. Automated server tests do not visually verify the client renderer.

Fabric and NeoForge each register the workbench block/item, BE type, Divider item, nonpersistent selection component, and client renderer. All interaction, state, analysis, recording, binding, and rendering behavior is common. Recipes provide survival access to the workbench, Divider, blank page, and wand. There is no chest-style menu.

## Verification

Unit tests cover all four facings, part/controller addressing across positive/negative chunk edges, inverse world mapping for all 81 logical centers, continuous seam hits, finite bounds, grid center/corners/edges and side rejection; stable IDs and item ownership; occupancy; removal and dangling-arrow cleanup; boundary validation; revision and physical save/load; corruption repair and strict expression decoding; intrinsic/expression separation and profile changes; direction/role preservation; Monad, Dyad, straight Triad, spoke Tetrad; and feedback classifications.

Workbench GameTests cover item placement of both halves with one BE, blocked/replaceable targets and world-border rejection, canonical semantics and cross-half Divider/recovery in all four facings, survival/creative removal from each part with exact item/material/page/recovery counts, both explosion callback orders, mismatched-pair teardown, server-side placement and recovery; occupied-slot rejection; full-inventory drops; block removal contents; directed creation and erasure; reverse roles; explicit unstable enclosure; distance/self/duplicate/cross-table validation and selection cancellation; recording guards; and the full physical-state → analysis → page → page retrieval → wand binding → Heat manifestation path. Persistence is tested separately:

- **Serialization:** a placed bench's complete saved data is decoded into a detached BE, without destroying/replacing a block. Assertions cover all loci, stable IDs, positions, ItemStacks/components, expressions, resolved/unresolved flags, arrows, membership, recorded page, recovery, next ID after deletion, revision, and recomputed analysis. Blank pages, wrong items, incompatible components, oversized page counts, and partial recorded expressions have separate coverage.
- **Minecraft chunk lifecycle:** two halves crossing a chunk boundary outside GameTest's structure tickets are populated through real item placement and interactions. `ServerChunkCache.save(true)` flushes chunk/region storage; the temporary loading ticket expires naturally. The test waits for the old BE to be removed and the chunk to be absent, then loads them normally in two tests: controller-first and structural-half-first. Validation and saving while only one chunk is loaded must preserve the table without loading its counterpart. Both halves must resolve the new controller after full restoration. Tests require distinct chunk/BE instances and a new session, verifies the physical facts/recording/revision/analysis, waits for entity storage to load, checks that unloading created no item drops, and checks exact drops on subsequent destruction. No block destruction or manual `loadWithComponents` occurs in the reload path.
- **Not covered:** process/server restart, client reconnect/synchronization rendering, or visual client inspection. The lifecycle test is same-process disk-backed chunk unload/reload, not restart coverage.

The 26.3 implementation paths inspected were `ServerChunkCache.save` → `ChunkMap.saveAllChunks`, `ChunkMap.scheduleUnload` → `ServerLevel.unload` → `LevelChunk.clearAllBlockEntities` (marks BEs removed without draining), and normal chunk deserialization/BE promotion. Actual block replacement instead calls `LevelChunk.setBlockState` → `preRemoveSideEffects`, which drains contents. These are deliberately distinct test paths. The preexisting runtime GameTests now explicitly initialize their test-only fixtures.

Run:

```bash
./gradlew :domain:test
./gradlew :common:test
./gradlew :common:compileJava
./gradlew :fabric:build
./gradlew :neoforge:build
./gradlew :neoforge:runGameTestServer -PgameTests
```

Build without `-PgameTests` for distribution. The tests exercise serialization and actual chunk save/unload/reload, but do not simulate a process restart or visually inspect either client's renderer. No domain semantics, multi-Form isolation UX, mediation rules, sustained fields, progression, notebook, or advanced vessels are introduced by this slice.

## Architectural reference

The Ars Nouveau [TableBlock](https://github.com/baileyholl/Ars-Nouveau/blob/1.21.x/src/main/java/com/hollingsworth/arsnouveau/common/block/TableBlock.java), [ScribesBlock](https://github.com/baileyholl/Ars-Nouveau/blob/1.21.x/src/main/java/com/hollingsworth/arsnouveau/common/block/ScribesBlock.java), [ScribesRenderer](https://github.com/baileyholl/Ars-Nouveau/blob/1.21.x/src/main/java/com/hollingsworth/arsnouveau/client/renderer/tile/ScribesRenderer.java) and [table geometry](https://github.com/baileyholl/Ars-Nouveau/blob/1.21.x/src/main/resources/assets/ars_nouveau/geo/scribes_table.geo.json) were inspected as structural references: paired facing-aware blocks, interaction forwarding, and furniture spanning the footprint. Spellcraft implements these independently with one BE, native static models, current 26.3 lifecycle APIs and finite bounds, rather than copying their code, GeckoLib rendering or visual style.
