# Common Minecraft Project

`common/` is a real Gradle subproject and owns the shared Minecraft-facing source tree.

It exists as a first-class project so IntelliJ and JDTLS have a proper Minecraft 26.3 classpath for files under `common/src/main/java`.

Rules:

- vanilla `net.minecraft.*` imports are allowed;
- Fabric and NeoForge imports are not allowed;
- loader-independent Minecraft gameplay belongs here;
- loader registration/event/network glue belongs in `fabric/` or `neoforge/`.

The loader projects consume the exported `commonJava` and `commonResources` configurations and compile those sources directly into their own distributable jars.

Useful checks from the repository root:

```bash
./gradlew :common:compileJava
./gradlew :common:genSources
```

Do not install the common jar into Minecraft.
