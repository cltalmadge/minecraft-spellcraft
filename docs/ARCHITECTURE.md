# Architecture Notes

```text
                         ┌───────────────────────┐
                         │        domain         │
                         │ pure Java rules/tests │
                         └───────────┬───────────┘
                                     │ sources compiled into both jars
                                     ▼
                         ┌───────────────────────┐
                         │        common         │
                         │ shared Minecraft code │
                         └───────────┬───────────┘
                                     │
                         ┌───────────┴───────────┐
                         ▼                       ▼
                 ┌──────────────┐        ┌──────────────┐
                 │    fabric    │        │   neoforge   │
                 │ thin adapter │        │ thin adapter │
                 └──────────────┘        └──────────────┘
```

The `domain` and `common` directories are source-owned once but compiled independently by each loader build. This is intentional. It avoids making the cross-loader source architecture depend on Fabric Loom and NeoForge ModDevGradle agreeing on one wrapper/plugin environment.

Once the project grows, you can graduate to a published internal common artifact if that actually improves the build. Do not introduce that complexity before it solves a real problem.
