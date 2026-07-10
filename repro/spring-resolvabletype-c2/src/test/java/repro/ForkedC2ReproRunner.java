package repro;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ForkedC2ReproRunner {
    private ForkedC2ReproRunner() {
    }

    public static void main(String[] args) throws Exception {
        String java = System.getProperty("repro.java", defaultJava());
        int timeoutSeconds = Integer.getInteger("repro.timeoutSeconds", 90);
        int forks = Integer.getInteger("repro.forks", 1);

        for (int fork = 1; fork <= forks; fork++) {
            runFork(java, timeoutSeconds, fork, forks);
        }
    }

    private static void runFork(String java, int timeoutSeconds, int fork, int forks) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(java);
        command.add("-server");
        if (Boolean.parseBoolean(System.getProperty("repro.xcomp", "true"))) {
            command.add("-Xcomp");
        } else {
            command.add("-Xbatch");
            command.add("-XX:CompileThreshold=" + Integer.getInteger("repro.compileThreshold", 100));
            command.add("-XX:BackEdgeThreshold=" + Integer.getInteger("repro.backEdgeThreshold", 1000));
        }
        boolean tiered = Boolean.parseBoolean(System.getProperty("repro.tiered", "false"));
        command.add(tiered ? "-XX:+TieredCompilation" : "-XX:-TieredCompilation");
        int defaultCompilerCount = tiered ? 2 : 1;
        command.add("-XX:CICompilerCount=" + Integer.getInteger("repro.ciCompilerCount", defaultCompilerCount));
        command.add("-XX:+UnlockDiagnosticVMOptions");
        if (Boolean.parseBoolean(System.getProperty("repro.whiteBox", "false"))
                || "whiteBoxCompileStage".equals(System.getProperty("repro.target", ""))) {
            command.add("-XX:+WhiteBoxAPI");
            command.add("-Xbootclasspath/a:" + new File("target/test-classes").getAbsolutePath());
        }
        command.add("-XX:CompileCommand=quiet");
        if (Boolean.parseBoolean(System.getProperty("repro.printCompilation", "true"))) {
            command.add("-XX:+PrintCompilation");
        }
        if (Boolean.parseBoolean(System.getProperty("repro.compileOnly", "true"))) {
            for (String compileTarget : compileTargets()) {
                command.add("-XX:CompileCommand=compileonly," + compileTarget);
            }
        }
        command.addAll(extraJvmArgs());
        command.add("-Drepro.iterations=" + Integer.getInteger("repro.iterations", 2_000_000));
        command.add("-Drepro.seconds=" + Long.getLong("repro.seconds", 0L));
        command.add("-Drepro.clearCacheEvery=" + Integer.getInteger("repro.clearCacheEvery", 0));
        command.add("-Drepro.deoptAfter=" + Integer.getInteger("repro.deoptAfter",
                Integer.getInteger("repro.iterations", 2_000_000) + 1));
        command.add("-Drepro.target=" + System.getProperty("repro.target", "field"));
        command.add("-cp");
        command.add(childClasspath());
        command.add(ResolvableTypeC2Repro.class.getName());

        System.out.println("Launching child JVM " + fork + "/" + forks + ":");
        System.out.println(join(command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        StreamCollector collector = new StreamCollector(process.getInputStream());
        collector.start();
        PerfCounterSampler sampler = PerfCounterSampler.start(java, process);

        boolean exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!exited) {
            if (sampler != null) {
                sampler.shutdown();
            }
            String diagnostic = diagnostics(java, process);
            process.destroyForcibly();
            collector.join(TimeUnit.SECONDS.toMillis(5));
            throw new AssertionError("child JVM did not finish within " + timeoutSeconds
                    + "s; likely C2 hang while compiling "
                    + Arrays.toString(compileTargets()) + ". Output:\n"
                    + collector.output() + "\nDiagnostics:\n" + diagnostic);
        }

        if (sampler != null) {
            sampler.shutdown();
        }
        collector.join(TimeUnit.SECONDS.toMillis(5));
        String output = collector.output();
        if (sampler != null && Boolean.parseBoolean(System.getProperty("repro.printPerfSummaryOnExit", "false"))) {
            System.out.println(sampler.summary("exit"));
        }
        if (Boolean.parseBoolean(System.getProperty("repro.printSuccessOutput", "true"))) {
            System.out.print(output);
        }
        if (process.exitValue() != 0) {
            throw new AssertionError("child JVM exited with " + process.exitValue() + ". Output:\n" + output);
        }
    }

    private static String defaultJava() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

    private static String childClasspath() {
        StringBuilder cp = new StringBuilder();
        cp.append(new File("target/classes").getAbsolutePath());
        File testClasses = new File("target/test-classes");
        if (testClasses.isDirectory()) {
            cp.append(File.pathSeparator).append(testClasses.getAbsolutePath());
        }
        File dependencyDir = new File("target/dependency");
        File[] jars = dependencyDir.listFiles();
        if (jars != null) {
            Arrays.sort(jars);
            for (File jar : jars) {
                if (jar.getName().endsWith(".jar")) {
                    cp.append(File.pathSeparator).append(jar.getAbsolutePath());
                }
            }
        }
        return cp.toString();
    }

    private static List<String> extraJvmArgs() {
        String value = System.getProperty("repro.jvmArgs", "").trim();
        if (value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split("\\s+"));
    }

    private static String[] compileTargets() {
        String explicit = System.getProperty("repro.compileTargets", "").trim();
        if (!explicit.isEmpty()) {
            return explicit.split("\\s*,\\s*");
        }
        String target = System.getProperty("repro.target", "field");
        if ("methodParameter".equals(target)) {
            return new String[] { "org/springframework/core/ResolvableType.forMethodParameter" };
        }
        if ("methodOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forMethodParameter",
                    "repro/ResolvableTypeC2Repro.exerciseMethodParameterOsrMemBarLoop",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("mixedOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/core/ResolvableType.forMethodParameter",
                    "repro/ResolvableTypeC2Repro.exerciseOsrMemBarLoop",
                    "repro/ResolvableTypeC2Repro.exerciseMethodParameterOsrMemBarLoop",
                    "repro/ResolvableTypeC2Repro.exerciseNestedReleaseChain",
                    "repro/ResolvableTypeC2Repro.exerciseConvergedRelease",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("shortOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/core/ResolvableType.forMethodParameter",
                    "repro/ResolvableTypeC2Repro.exerciseShortOsrDriver",
                    "repro/ResolvableTypeC2Repro.exerciseShortOsrVariantA",
                    "repro/ResolvableTypeC2Repro.exerciseShortOsrVariantB",
                    "repro/ResolvableTypeC2Repro.exerciseShortOsrVariantC",
                    "repro/ResolvableTypeC2Repro.exerciseShortOsrVariantD",
                    "repro/ResolvableTypeC2Repro.exerciseConvergedRelease",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("cacheOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/core/ResolvableType.forMethodParameter",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseSpringCachePurgeOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("sharedCacheOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/core/ResolvableType.forMethodParameter",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseSharedCacheRingOsr",
                    "repro/ResolvableTypeC2Repro.exerciseNestedReleaseChain",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("fenceOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseUnsafeFenceOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("irreducibleLockOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseIrreducibleLockOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("segmentStormOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseSegmentStormOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$HotReferenceMap.*",
                    "repro/ResolvableTypeC2Repro$CollisionKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("segmentPhiOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseSegmentPhiOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$HotReferenceMap.*",
                    "repro/ResolvableTypeC2Repro$CollisionKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("resizeStormOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseResizeStormOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$HotReferenceMap.*",
                    "repro/ResolvableTypeC2Repro$SpreadKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("monitorResizeOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseMonitorResizeOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$HotReferenceMap.*",
                    "repro/ResolvableTypeC2Repro$SpreadKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("branchFenceOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseBranchFenceOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("clearResizeOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseClearResizeOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$HotReferenceMap.*",
                    "repro/ResolvableTypeC2Repro$SpreadKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("allocationChainOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseAllocationChainOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$FinalPair.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("libraryShapeOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "sun/nio/cs/FastCharsetProvider.lookup",
                    "sun/nio/cs/StandardCharsets.*",
                    "java/nio/DirectByteBuffer.*",
                    "java/util/WeakHashMap.*",
                    "java/util/Collections$SynchronizedMap.*",
                    "java/net/URL.*",
                    "repro/ResolvableTypeC2Repro.exerciseLibraryShapeOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("weakQueueStormOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "java/util/WeakHashMap.*",
                    "java/util/WeakHashMap$*.*",
                    "java/util/Collections$SynchronizedMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "java/util/Vector.*",
                    "java/util/Vector$*.*",
                    "java/util/zip/ZipFile.*",
                    "java/util/zip/ZipFile$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseWeakQueueStormOsr",
                    "repro/ResolvableTypeC2Repro.exerciseVectorChain",
                    "repro/ResolvableTypeC2Repro.exerciseZipCloseChain",
                    "repro/ResolvableTypeC2Repro.exercisePseudoZipChain",
                    "repro/ResolvableTypeC2Repro.compactMemBarChain",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$LockBox.*",
                    "repro/ResolvableTypeC2Repro$PseudoZip.*",
                    "repro/ResolvableTypeC2Repro$PseudoZip$*.*",
                    "repro/ResolvableTypeC2Repro$FinalPair.*",
                    "repro/ResolvableTypeC2Repro$WeakStormKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("loopPhiSelfOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/core/ResolvableType.forMethodParameter",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseLoopPhiSelfOsr",
                    "repro/ResolvableTypeC2Repro.compactMemBarChain",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$LockBox.*",
                    "repro/ResolvableTypeC2Repro$FinalPair.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("pureZipSpringWideOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/core/ResolvableType.forType",
                    "org/springframework/core/ResolvableType.getNested",
                    "org/springframework/core/ResolvableType.as",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exercisePureZipSpringWideOsr",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.finalCpuLiteStep",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$PseudoZip.loopPhiResolvableClose",
                    "repro/ResolvableTypeC2Repro$PseudoZip.*",
                    "repro/ResolvableTypeC2Repro$PseudoZip$*.*",
                    "repro/ResolvableTypeC2Repro$FinalCpuLite.*",
                    "repro/ResolvableTypeC2Repro$FinalPair.*",
                    "repro/ResolvableTypeC2Repro$HotReferenceMap.*",
                    "repro/ResolvableTypeC2Repro$SpreadKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("advanced".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "repro/ResolvableTypeC2Repro.*",
                    "repro/ResolvableTypeC2Repro$*.*"
            };
        }
        if ("fieldObservePinned".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField"
            };
        }
        if ("fieldObserveNeedlePinned".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField"
            };
        }
        if ("compilerMethodWindowOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/core/ResolvableType.forType",
                    "org/springframework/core/ResolvableType.getNested",
                    "org/springframework/core/ResolvableType.as",
                    "org/springframework/core/SerializableTypeWrapper.forTypeProvider",
                    "org/springframework/core/SerializableTypeWrapper$FieldTypeProvider.<init>",
                    "org/springframework/core/SerializableTypeWrapper$FieldTypeProvider.getType",
                    "org/springframework/util/Assert.notNull",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseCompilerMethodWindowOsr",
                    "repro/ResolvableTypeC2Repro.fatResolvableMix",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.compactMemBarChain",
                    "repro/ResolvableTypeC2Repro.finalCpuLiteStep",
                    "repro/ResolvableTypeC2Repro.selectFinalCpuLiteSeed",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$FinalCpuLite.*",
                    "repro/ResolvableTypeC2Repro$FinalPair.*",
                    "repro/ResolvableTypeC2Repro$HotReferenceMap.*",
                    "repro/ResolvableTypeC2Repro$SpreadKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("compilerMethodFocusedOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/core/ResolvableType.forType",
                    "org/springframework/core/ResolvableType.getNested",
                    "org/springframework/core/ResolvableType.as",
                    "org/springframework/core/SerializableTypeWrapper.forTypeProvider",
                    "org/springframework/core/SerializableTypeWrapper$FieldTypeProvider.<init>",
                    "org/springframework/core/SerializableTypeWrapper$FieldTypeProvider.getType",
                    "org/springframework/util/Assert.notNull",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseCompilerMethodFocusedOsr",
                    "repro/ResolvableTypeC2Repro.hotFocusedResolvableStage",
                    "repro/ResolvableTypeC2Repro.fatResolvableMix",
                    "repro/ResolvableTypeC2Repro.finalCpuLiteStep",
                    "repro/ResolvableTypeC2Repro.compactMemBarChain",
                    "repro/ResolvableTypeC2Repro.selectFinalCpuLiteSeed",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$FinalCpuLite.*",
                    "repro/ResolvableTypeC2Repro$FinalPair.*",
                    "repro/ResolvableTypeC2Repro$HotReferenceMap.*",
                    "repro/ResolvableTypeC2Repro$SpreadKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("compilerMethodStagedOsr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "org/springframework/core/ResolvableType.forType",
                    "org/springframework/core/ResolvableType.getNested",
                    "org/springframework/core/ResolvableType.as",
                    "org/springframework/core/SerializableTypeWrapper.forTypeProvider",
                    "org/springframework/core/SerializableTypeWrapper$FieldTypeProvider.<init>",
                    "org/springframework/core/SerializableTypeWrapper$FieldTypeProvider.getType",
                    "org/springframework/util/Assert.notNull",
                    "org/springframework/util/ConcurrentReferenceHashMap.*",
                    "org/springframework/util/ConcurrentReferenceHashMap$*.*",
                    "repro/ResolvableTypeC2Repro.exerciseCompilerMethodStagedOsr",
                    "repro/ResolvableTypeC2Repro.lateTargetResolvableStage",
                    "repro/ResolvableTypeC2Repro.hotFocusedResolvableStage",
                    "repro/ResolvableTypeC2Repro.fatResolvableMix",
                    "repro/ResolvableTypeC2Repro.finalCpuLiteStep",
                    "repro/ResolvableTypeC2Repro.compactMemBarChain",
                    "repro/ResolvableTypeC2Repro.selectFinalCpuLiteSeed",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$FinalCpuLite.*",
                    "repro/ResolvableTypeC2Repro$FinalPair.*",
                    "repro/ResolvableTypeC2Repro$HotReferenceMap.*",
                    "repro/ResolvableTypeC2Repro$SpreadKey.*",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        if ("osr".equals(target)) {
            return new String[] {
                    "org/springframework/core/ResolvableType.forField",
                    "repro/ResolvableTypeC2Repro.exerciseOsrMemBarLoop",
                    "repro/ResolvableTypeC2Repro.hotWrapperThree",
                    "repro/ResolvableTypeC2Repro.consume",
                    "repro/ResolvableTypeC2Repro$BarrierHolder.*"
            };
        }
        return new String[] { "org/springframework/core/ResolvableType.forField" };
    }

    private static String diagnostics(String java, Process process) {
        StringBuilder out = new StringBuilder();
        long pid = pid(process);
        out.append("pid=").append(pid).append('\n');
        PerfCounterSampler.appendSummary(out, java, pid);
        File javaBin = new File(java);
        File jstack = new File(javaBin.getParentFile(), "jstack");
        if (jstack.isFile() && pid > 0L) {
            runDiagnostic(out, Arrays.asList(jstack.getAbsolutePath(), "-l", Long.toString(pid)));
        } else {
            out.append("jstack not found next to ").append(java).append('\n');
        }
        return out.toString();
    }

    private static long pid(Process process) {
        try {
            Method method = Process.class.getMethod("pid");
            return ((Number) method.invoke(process)).longValue();
        } catch (Exception ignore) {
            // Fall through to the legacy field lookup on older JDKs.
        }
        try {
            Field field = process.getClass().getDeclaredField("pid");
            field.setAccessible(true);
            return ((Number) field.get(process)).longValue();
        } catch (Exception e) {
            return -1L;
        }
    }

    private static void runDiagnostic(StringBuilder out, List<String> command) {
        out.append("$ ").append(join(command)).append('\n');
        try {
            Process diagnostic = new ProcessBuilder(command).redirectErrorStream(true).start();
            StreamCollector collector = new StreamCollector(diagnostic.getInputStream());
            collector.start();
            boolean exited = diagnostic.waitFor(15, TimeUnit.SECONDS);
            if (!exited) {
                diagnostic.destroyForcibly();
                out.append("diagnostic command timed out\n");
            }
            collector.join(TimeUnit.SECONDS.toMillis(2));
            out.append(collector.output());
        } catch (Exception e) {
            out.append(e).append('\n');
        }
    }

    private static String join(List<String> command) {
        StringBuilder builder = new StringBuilder();
        for (String part : command) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private static final class PerfCounterSampler extends Thread {
        private static final long SAMPLE_MILLIS = Long.getLong("repro.perfSampleMillis", 250L);
        private static final int MAX_SNAPSHOTS = Integer.getInteger("repro.perfSampleLimit", 256);

        private final List<String> command;
        private final List<PerfSnapshot> snapshots = new LinkedList<PerfSnapshot>();
        private volatile boolean running = true;

        private PerfCounterSampler(List<String> command) {
            super("perf-counter-sampler");
            this.command = command;
            setDaemon(true);
        }

        static PerfCounterSampler start(String java, Process process) {
            long pid = pid(process);
            if (pid <= 0L) {
                return null;
            }
            File javaBin = new File(java);
            File jcmd = new File(javaBin.getParentFile(), "jcmd");
            if (!jcmd.isFile()) {
                return null;
            }
            PerfCounterSampler sampler = new PerfCounterSampler(Arrays.asList(
                    jcmd.getAbsolutePath(), Long.toString(pid), "PerfCounter.print"));
            sampler.start();
            return sampler;
        }

        static void appendSummary(StringBuilder out, String java, long pid) {
            if (pid <= 0L) {
                return;
            }
            File javaBin = new File(java);
            File jcmd = new File(javaBin.getParentFile(), "jcmd");
            if (!jcmd.isFile()) {
                out.append("jcmd not found next to ").append(java).append('\n');
                return;
            }
            List<PerfSnapshot> snapshots = new ArrayList<PerfSnapshot>();
            for (int i = 0; i < 5; i++) {
                PerfSnapshot snapshot = capture(Arrays.asList(
                        jcmd.getAbsolutePath(), Long.toString(pid), "PerfCounter.print"));
                if (snapshot != null) {
                    snapshots.add(snapshot);
                }
                if (i < 4) {
                    try {
                        Thread.sleep(5_000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            if (snapshots.isEmpty()) {
                out.append("perf-counter snapshots unavailable\n");
                return;
            }
            out.append("perf-counter snapshots:\n");
            for (PerfSnapshot snapshot : snapshots) {
                out.append(snapshot).append('\n');
            }
            MethodStreak repeatedMethod = repeatedNonEmptyMethod(snapshots);
            if (repeatedMethod != null) {
                out.append("compilerThread.0.method stayed non-empty and fixed; treat as reproduced: ")
                        .append(repeatedMethod.method)
                        .append(" samples=").append(repeatedMethod.samples)
                        .append(" durationMillis=").append(repeatedMethod.durationMillis)
                        .append('\n');
            }
        }

        private static MethodStreak repeatedNonEmptyMethod(List<PerfSnapshot> snapshots) {
            String current = null;
            int streak = 0;
            long streakStart = 0L;
            MethodStreak best = null;
            for (PerfSnapshot snapshot : snapshots) {
                if (snapshot.compilerMethod.length() == 0) {
                    current = null;
                    streak = 0;
                    streakStart = 0L;
                    continue;
                }
                if (snapshot.compilerMethod.equals(current)) {
                    streak++;
                } else {
                    current = snapshot.compilerMethod;
                    streak = 1;
                    streakStart = snapshot.captureMillis;
                }
                if (streak >= 3) {
                    long durationMillis = snapshot.captureMillis - streakStart;
                    if (best == null || streak > best.samples) {
                        best = new MethodStreak(current, streak, durationMillis);
                    }
                }
            }
            return best;
        }

        private static PerfSnapshot capture(List<String> command) {
            try {
                Process diagnostic = new ProcessBuilder(command).redirectErrorStream(true).start();
                StreamCollector collector = new StreamCollector(diagnostic.getInputStream());
                collector.start();
                boolean exited = diagnostic.waitFor(10, TimeUnit.SECONDS);
                if (!exited) {
                    diagnostic.destroyForcibly();
                    return null;
                }
                collector.join(TimeUnit.SECONDS.toMillis(2));
                return PerfSnapshot.parse(collector.output());
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public void run() {
            while (running) {
                PerfSnapshot snapshot = capture(command);
                if (snapshot != null) {
                    remember(snapshot);
                }
                try {
                    Thread.sleep(SAMPLE_MILLIS);
                } catch (InterruptedException e) {
                    interrupt();
                    return;
                }
            }
        }

        void shutdown() {
            running = false;
            interrupt();
            try {
                join(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private synchronized void remember(PerfSnapshot snapshot) {
            snapshots.add(snapshot);
            while (snapshots.size() > MAX_SNAPSHOTS) {
                snapshots.remove(0);
            }
        }

        synchronized String summary(String label) {
            StringBuilder out = new StringBuilder();
            out.append("perf-counter ").append(label).append(" snapshots:\n");
            if (snapshots.isEmpty()) {
                out.append("none\n");
                return out.toString();
            }
            for (PerfSnapshot snapshot : snapshots) {
                out.append(snapshot).append('\n');
            }
            MethodStreak repeatedMethod = repeatedNonEmptyMethod(snapshots);
            if (repeatedMethod != null) {
                out.append("compilerThread.0.method stayed non-empty and fixed; treat as reproduced: ")
                        .append(repeatedMethod.method)
                        .append(" samples=").append(repeatedMethod.samples)
                        .append(" durationMillis=").append(repeatedMethod.durationMillis)
                        .append('\n');
            } else if (allCompilerMethodsEmpty(snapshots)) {
                out.append("compilerThread.0.method stayed empty throughout sampled history\n");
            }
            return out.toString();
        }

        private static boolean allCompilerMethodsEmpty(List<PerfSnapshot> snapshots) {
            for (PerfSnapshot snapshot : snapshots) {
                if (snapshot.compilerMethod.length() != 0) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class MethodStreak {
        private final String method;
        private final int samples;
        private final long durationMillis;

        private MethodStreak(String method, int samples, long durationMillis) {
            this.method = method;
            this.samples = samples;
            this.durationMillis = durationMillis;
        }
    }

    private static final class PerfSnapshot {
        private final long captureMillis;
        private final String compilerMethod;
        private final String compilerCompiles;
        private final String compilerTime;
        private final String lastMethod;
        private final String totalCompiles;
        private final String osrCompiles;
        private final String totalTime;

        private PerfSnapshot(long captureMillis, String compilerMethod, String compilerCompiles,
                String compilerTime, String lastMethod, String totalCompiles,
                String osrCompiles, String totalTime) {
            this.captureMillis = captureMillis;
            this.compilerMethod = compilerMethod;
            this.compilerCompiles = compilerCompiles;
            this.compilerTime = compilerTime;
            this.lastMethod = lastMethod;
            this.totalCompiles = totalCompiles;
            this.osrCompiles = osrCompiles;
            this.totalTime = totalTime;
        }

        static PerfSnapshot parse(String output) {
            return new PerfSnapshot(
                    System.currentTimeMillis(),
                    valueOf(output, "sun.ci.compilerThread.0.method"),
                    valueOf(output, "sun.ci.compilerThread.0.compiles"),
                    valueOf(output, "sun.ci.compilerThread.0.time"),
                    valueOf(output, "sun.ci.lastMethod"),
                    valueOf(output, "sun.ci.totalCompiles"),
                    valueOf(output, "sun.ci.osrCompiles"),
                    valueOf(output, "java.ci.totalTime"));
        }

        private static String valueOf(String output, String key) {
            String prefix = key + "=";
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(prefix)) {
                        return line.substring(prefix.length()).replace("\"", "");
                    }
                }
            } catch (IOException e) {
                return "";
            }
            return "";
        }

        @Override
        public String toString() {
            return "t=" + captureMillis
                    + " method=" + compilerMethod
                    + " compiles=" + compilerCompiles
                    + " cthread_time=" + compilerTime
                    + " totalCompiles=" + totalCompiles
                    + " osrCompiles=" + osrCompiles
                    + " totalTime=" + totalTime;
        }
    }

    private static final class StreamCollector extends Thread {
        private final InputStream input;
        private final StringBuilder output = new StringBuilder();

        private StreamCollector(InputStream input) {
            super("child-jvm-output");
            this.input = input;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            } catch (IOException e) {
                output.append(e).append('\n');
            }
        }

        private String output() {
            return output.toString();
        }
    }
}
