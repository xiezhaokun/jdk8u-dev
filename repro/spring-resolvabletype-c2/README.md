# spring-resolvabletype-c2

Spring 5.3.31 + JDK8 C2 repro for `ResolvableType::forField(Field, nestingLevel, implementationClass)`.

Current strongest repro target for fixed
`sun.ci.compilerThread.0.method="org/springframework/core/ResolvableType forField"`:

- `repro.ResolvableTypeC2Repro.exerciseFieldObserveNeedlePinned`
- narrower than `pureZipSpringWideOsr`
- repeatedly hits only `ResolvableType.forField(field, nestingLevel, implementationClass)`
  from a deep generic field subset, with periodic `ResolvableType.clearCache()`

Current strongest repro target for raw `MemNode::can_see_stored_value` traffic:

- `repro.ResolvableTypeC2Repro.exercisePureZipSpringWideOsr`
- hot helper: `repro.ResolvableTypeC2Repro$PseudoZip.loopPhiResolvableClose`
- mixes Spring `ResolvableType.forField(...)`, nested monitor edges, `FinalCpuLite`,
  and `ConcurrentReferenceHashMap` updates in one compile window

## Repro command

Run from this directory.

To reproduce the stable fixed-method symptom on `forField`:

```bash
mvn -o -q test-compile

REPRO_TARGET=fieldObserveNeedlePinned \
TARGET_METHOD='org/springframework/core/ResolvableType forField' \
bash ./capture_fixed_method.sh
```

To drive the heavier `MemNode` traffic target directly:

```bash
mvn -o -q test-compile

/Users/xiezhaokun/workspace/jdk8u-dev/build/codex-fastdebug-x86_64/jdk/bin/java \
  -server \
  -Xbatch \
  -XX:CompileThreshold=12000 \
  -XX:BackEdgeThreshold=12000 \
  -XX:-TieredCompilation \
  -XX:CICompilerCount=1 \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+StressGCM \
  -XX:+StressLCM \
  -XX:+StressReflectiveCode \
  -XX:+AlwaysIncrementalInline \
  -XX:MaxInlineLevel=15 \
  -XX:MaxInlineSize=500 \
  -XX:FreqInlineSize=1000 \
  -XX:InlineSmallCode=10000 \
  -XX:MaxNodeLimit=300000 \
  -XX:CompileCommand=quiet \
  -XX:CompileCommand=inline,repro/ResolvableTypeC2Repro.hotWrapperThree \
  -XX:CompileCommand=inline,repro/ResolvableTypeC2Repro.finalCpuLiteStep \
  -XX:CompileCommand=inline,repro/ResolvableTypeC2Repro.consume \
  -XX:CompileCommand=inline,repro/ResolvableTypeC2Repro\$PseudoZip.loopPhiResolvableClose \
  -Drepro.iterations=1 \
  -Drepro.deoptAfter=2 \
  -Drepro.target=pureZipSpringWideOsr \
  -cp target/classes:target/test-classes:target/dependency/spring-core-5.3.31.jar:target/dependency/spring-jcl-5.3.31.jar \
  repro.ResolvableTypeC2Repro
```

## Expected result

With the current fastdebug JDK8 tree, this configuration reproducibly triggers
a pathological C2 compile where:

- `sun.ci.compilerThread.0.method` stays non-empty and fixed for many samples
- `sun.ci.compilerThread.0.compiles`, `sun.ci.totalCompiles`, and often
  `sun.ci.osrCompiles` stop moving
- the child JVM keeps running and CPU stays high

Observed fixed methods include:

- `org/springframework/core/ResolvableType as`
- `org/springframework/core/ResolvableType forType`
- `org/springframework/core/ResolvableType forField`
- `org/springframework/core/ResolvableType getNested`
- `org/springframework/core/ResolvableType resolveClass`

## Repro criterion

Use `PerfCounter.print` as the primary discriminator.

- if `sun.ci.compilerThread.0.method` stays non-empty and fixed across samples,
  treat that run as reproduced
- if compile counters stop moving at the same time, that is a stronger signal

Representative captures from this workspace:

- `target/captures/pureZipSpringWideOsr-inline-20260710-092814.perf`
- `target/captures/pureZipSpringWideOsr-inline-20260710-092814.jstack.txt`
- `target/captures/pureZipSpringWideOsr-inline-20260710-092814.lldb.txt`

Additional fixed-method captures observed on July 10, 2026:

- external `PerfCounter.print` captured
  `sun.ci.compilerThread.0.method="org/springframework/core/ResolvableType forField"`
- long-running captures also pinned
  `org/springframework/core/ResolvableType getSuperType`
- compact needle capture pinned
  `org/springframework/core/ResolvableType forField` for samples `249..256`
  in `target/captures/fieldObserveNeedlePinned-capture-20260710-110406.perf`
- the same compact target also showed earlier long fixed windows on
  `org/springframework/core/ResolvableType getNested` and
  `org/springframework/core/ResolvableType resolveClass`

## Capture helper

Use the helper script when you want one command that:

- starts the strongest repro target
- samples `PerfCounter.print`
- waits for a fixed compiler thread method
- captures `PerfCounter`, `jstack`, `lldb`, and full `C2_CSV` output

Default behavior waits for
`org/springframework/core/ResolvableType forField`:

```bash
mvn -o -q test-compile

bash ./capture_fixed_method.sh
```

To treat any fixed non-empty `sun.ci.compilerThread.0.method` as success:

```bash
MATCH_ANY_FIXED=1 TARGET_METHOD=ANY bash ./capture_fixed_method.sh
```

To run the compact `forField`-focused target instead of the wider zip-based one:

```bash
REPRO_TARGET=fieldObserveNeedlePinned bash ./capture_fixed_method.sh
```

Artifacts are written under `target/captures/`.

## Current MemNode Observation

On the current fastdebug JDK8 tree, the same repro run can show both:

- a fixed non-empty `sun.ci.compilerThread.0.method`
- heavy `MemNode::can_see_stored_value` traffic in `C2_CSV`

Observed `C2_CSV` counts in one `fieldObserveNeedlePinned` run that pinned
`org/springframework/core/ResolvableType forField`:

- `enter=58462`
- `probe=28184`
- `step=2257`

Observed `C2_CSV` counts in one `pureZipSpringWideOsr` run:

- `enter=40758`
- `probe=20592`
- `step=1641`

So far the recorded `can_see_stored_value` loop depth is still shallow:

- in the compact `fieldObserveNeedlePinned` capture, all observed `step` records
  were still `step=1`
- in the wider `pureZipSpringWideOsr` capture, `probe step=2` was observed for
  methods such as `ResolvableType::getGenerics`, but the corresponding `step`
  records still stopped at `1`
- no `C2_CSV repeat`
- no `C2_CSV step-limit`

That means the repro is already good at pinning the compiler thread on Spring
methods and driving `can_see_stored_value` heavily, but it has not yet shown a
single-call deep repeat in that loop body.
