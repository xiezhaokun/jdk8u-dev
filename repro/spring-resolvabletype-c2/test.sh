C2_CSV_LOG=1 \
C2_CSV_METHOD=finalCpuLiteStep \
C2_CSV_LOG_LIMIT=1500 \
C2_CSV_FATAL_STEP_LIMIT=64 \
mvn -o -q verify \
  -Drepro.java=/Users/xiezhaokun/workspace/jdk8u-dev/build/codex-fastdebug-x86_64/jdk/bin/java \
  -Drepro.target=finalCpuLiteSpringCascadeOsr \
  -Drepro.iterations=22000 \
  -Drepro.deoptAfter=80 \
  -Drepro.timeoutSeconds=3000 \
  -Drepro.compileThreshold=3 \
  -Drepro.backEdgeThreshold=4 \
  -Drepro.xcomp=false \
  -Drepro.printCompilation=false \
  -Drepro.printSuccessOutput=true \
  -Drepro.compileTargets=org/springframework/core/ResolvableType.forField,repro/ResolvableTypeC2Repro.exerciseFinalCpuLiteSpringCascadeOsr,repro/ResolvableTypeC2Repro.finalCpuLiteStep,repro/ResolvableTypeC2Repro.hotWrapperThree,repro/ResolvableTypeC2Repro.consume,repro/ResolvableTypeC2Repro.readFinalCpuLiteShadow,repro/ResolvableTypeC2Repro.readFinalCpuLiteGhost,repro/ResolvableTypeC2Repro.readFinalCpuLiteRelay,repro/ResolvableTypeC2Repro.foldFinalCpuLiteObserved,repro/ResolvableTypeC2Repro.selectFinalCpuLiteSeed,repro/ResolvableTypeC2Repro.compactMemBarChain \
  -Drepro.inlineTargets=repro/ResolvableTypeC2Repro.finalCpuLiteStep,repro/ResolvableTypeC2Repro.readFinalCpuLiteShadow,repro/ResolvableTypeC2Repro.readFinalCpuLiteGhost,repro/ResolvableTypeC2Repro.readFinalCpuLiteRelay,repro/ResolvableTypeC2Repro.foldFinalCpuLiteObserved,repro/ResolvableTypeC2Repro.selectFinalCpuLiteSeed,repro/ResolvableTypeC2Repro.compactMemBarChain \
  -Drepro.jvmArgs="-Xbatch -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressLCM -XX:+StressReflectiveCode -XX:-LoopUnswitching -XX:-PartialPeelLoop -XX:LoopOptsCount=18"
