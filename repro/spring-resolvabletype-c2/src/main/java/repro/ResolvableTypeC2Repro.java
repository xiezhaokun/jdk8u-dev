package repro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.ConcurrentReferenceHashMap;

import sun.misc.Unsafe;

public final class ResolvableTypeC2Repro {
    private static final Field[] FIELDS;
    private static final MethodParameter[] METHOD_PARAMETERS;
    private static final Class<?>[] IMPLEMENTATIONS = {
            StringRepository.class,
            IntegerRepository.class,
            DeepStringRepository.class,
            DeepIntegerRepository.class,
            RecursiveStringRepository.class,
            RecursiveIntegerRepository.class,
            HolderRepository.class,
            MutualStringRepository.class,
            MutualIntegerRepository.class,
            BarrierStringRepository.class,
            BarrierIntegerRepository.class
    };
    private static final TypeDriver[] TYPE_DRIVERS = {
            new StableStringDriver(),
            new StableIntegerDriver(),
            new RareMutualDriver()
    };

    private static volatile Object sink;
    private static volatile int intSink;
    private static volatile Object publishedObject;
    private static volatile int volatileGuard;
    private static volatile boolean osrFlip;
    private static final Object LOCK_ONE = new Object();
    private static final Object LOCK_TWO = new Object();
    private static final ConcurrentReferenceHashMap<Object, Object> SHARED_CACHE =
            new ConcurrentReferenceHashMap<Object, Object>(256, 0.75f, 16,
                    ConcurrentReferenceHashMap.ReferenceType.WEAK);
    private static final HotReferenceMap SEGMENT_STORM_CACHE = new HotReferenceMap();
    private static final CollisionKey[] COLLISION_KEYS = createCollisionKeys();
    private static final SpreadKey[] RESIZE_KEYS = createResizeKeys();
    private static final BarrierHolder<Object>[] SHARED_RING = createSharedRing();
    private static final Unsafe UNSAFE = findUnsafe();
    private static final long FINAL_CPU_LEFT_OFFSET = objectFieldOffset(FinalCpuBox.class, "left");
    private static final long FINAL_CPU_MIRROR_OFFSET = objectFieldOffset(FinalCpuBox.class, "mirror");
    private static final long FINAL_CPU_LITE_LEFT_OFFSET = objectFieldOffset(FinalCpuLite.class, "left");
    private static final long FINAL_CPU_LITE_RIGHT_OFFSET = objectFieldOffset(FinalCpuLite.class, "right");
    private static final long FINAL_CPU_LITE_THIRD_OFFSET = objectFieldOffset(FinalCpuLite.class, "third");
    private static final long FINAL_CPU_LITE_MIRROR_OFFSET = objectFieldOffset(FinalCpuLite.class, "mirror");
    private static final long FINAL_CPU_LITE_SPARE_OFFSET = objectFieldOffset(FinalCpuLiteBase.class, "spare");
    private static final long FINAL_CPU_LITE_SHADOW_OFFSET = objectFieldOffset(FinalCpuLiteBase.class, "shadow");
    private static final long FINAL_CPU_LITE_GHOST_OFFSET = objectFieldOffset(FinalCpuLiteBase.class, "ghost");
    private static final long FINAL_CPU_LITE_RELAY_OFFSET = objectFieldOffset(FinalCpuLiteBase.class, "relay");
    private static final long VOLATILE_CELL_VALUE_OFFSET = objectFieldOffset(VolatileCell.class, "value");
    private static final VolatileCell FINAL_CPU_CELL = new VolatileCell();
    private static final Map<Object, Object> LIBRARY_SHAPE_MAP =
            Collections.synchronizedMap(new WeakHashMap<Object, Object>());
    private static final URL[] URLS = createUrls();
    private static final File ZIP_STORM_FILE = createZipStormFile();
    private static final PseudoZip PSEUDO_ZIP = new PseudoZip();

    static {
        try {
            FIELDS = new Field[] {
                    GenericRepository.class.getDeclaredField("value"),
                    GenericRepository.class.getDeclaredField("values"),
                    GenericRepository.class.getDeclaredField("map"),
                    GenericRepository.class.getDeclaredField("nested"),
                    GenericRepository.class.getDeclaredField("wildcard"),
                    GenericRepository.class.getDeclaredField("array"),
                    GenericRepository.class.getDeclaredField("optional"),
                    GenericRepository.class.getDeclaredField("entry"),
                    GenericRepository.class.getDeclaredField("queue"),
                    DeepRepository.class.getDeclaredField("deepValue"),
                    DeepRepository.class.getDeclaredField("deepValues"),
                    DeepRepository.class.getDeclaredField("deepMap"),
                    DeepRepository.class.getDeclaredField("deepArray"),
                    DeepRepository.class.getDeclaredField("deepOptional"),
                    RecursiveRepository.class.getDeclaredField("recursiveValue"),
                    RecursiveRepository.class.getDeclaredField("recursiveValues"),
                    RecursiveRepository.class.getDeclaredField("recursiveMap"),
                    HolderRepository.class.getDeclaredField("holders"),
                    HolderRepository.class.getDeclaredField("holderMap"),
                    HolderRepository.class.getDeclaredField("holderArray"),
                    MutualA.class.getDeclaredField("peer"),
                    MutualA.class.getDeclaredField("peers"),
                    MutualA.class.getDeclaredField("indexedPeers"),
                    MutualB.class.getDeclaredField("owner"),
                    MutualB.class.getDeclaredField("owners"),
                    MutualB.class.getDeclaredField("reverseIndex"),
                    MutualRepository.class.getDeclaredField("root"),
                    MutualRepository.class.getDeclaredField("roots"),
                    MutualRepository.class.getDeclaredField("graph"),
                    BarrierRepository.class.getDeclaredField("holder"),
                    BarrierRepository.class.getDeclaredField("holders"),
                    BarrierRepository.class.getDeclaredField("barrierMap"),
                    BarrierRepository.class.getDeclaredField("boxedValue"),
                    BarrierRepository.class.getDeclaredField("boxedValues")
            };
            METHOD_PARAMETERS = new MethodParameter[] {
                    new MethodParameter(GenericRepository.class.getDeclaredMethod("consume", Object.class, List.class, Map.class), 0),
                    new MethodParameter(GenericRepository.class.getDeclaredMethod("consume", Object.class, List.class, Map.class), 1),
                    new MethodParameter(GenericRepository.class.getDeclaredMethod("consume", Object.class, List.class, Map.class), 2),
                    new MethodParameter(DeepRepository.class.getDeclaredMethod("consumeDeep", Object.class, List.class, Map.class), 0),
                    new MethodParameter(DeepRepository.class.getDeclaredMethod("consumeDeep", Object.class, List.class, Map.class), 1),
                    new MethodParameter(DeepRepository.class.getDeclaredMethod("consumeDeep", Object.class, List.class, Map.class), 2)
            };
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ResolvableTypeC2Repro() {
    }

    public static void main(String[] args) {
        int iterations = Integer.getInteger("repro.iterations", 2_000_000);
        String target = System.getProperty("repro.target", "field");
        int clearCacheEvery = Integer.getInteger("repro.clearCacheEvery", 0);
        int deoptAfter = Integer.getInteger("repro.deoptAfter", iterations + 1);
        long deadlineNanos = System.nanoTime() + secondsToNanos(Long.getLong("repro.seconds", 0L));
        long started = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            if (clearCacheEvery > 0 && (i % clearCacheEvery) == 0) {
                ResolvableType.clearCache();
            }
            if ("advanced".equals(target)) {
                exerciseAdvanced(i, deoptAfter);
            } else if ("osr".equals(target)) {
                exerciseOsrMemBarLoop(i, deoptAfter);
            } else if ("methodOsr".equals(target)) {
                exerciseMethodParameterOsrMemBarLoop(i, deoptAfter);
            } else if ("mixedOsr".equals(target)) {
                exerciseOsrMemBarLoop(i, deoptAfter);
                exerciseMethodParameterOsrMemBarLoop(i, deoptAfter);
                exerciseShortOsrVariants(i, deoptAfter);
            } else if ("shortOsr".equals(target)) {
                exerciseShortOsrDriver(i, deoptAfter);
            } else if ("cacheOsr".equals(target)) {
                exerciseSpringCachePurgeOsr(i, deoptAfter);
            } else if ("sharedCacheOsr".equals(target)) {
                exerciseSharedCacheRingOsr(i, deoptAfter);
            } else if ("fenceOsr".equals(target)) {
                exerciseUnsafeFenceOsr(i, deoptAfter);
            } else if ("irreducibleLockOsr".equals(target)) {
                exerciseIrreducibleLockOsr(i, deoptAfter);
            } else if ("segmentStormOsr".equals(target)) {
                exerciseSegmentStormOsr(i, deoptAfter);
            } else if ("segmentPhiOsr".equals(target)) {
                exerciseSegmentPhiOsr(i, deoptAfter);
            } else if ("resizeStormOsr".equals(target)) {
                exerciseResizeStormOsr(i, deoptAfter);
            } else if ("monitorResizeOsr".equals(target)) {
                exerciseMonitorResizeOsr(i, deoptAfter);
            } else if ("branchFenceOsr".equals(target)) {
                exerciseBranchFenceOsr(i, deoptAfter);
            } else if ("clearResizeOsr".equals(target)) {
                exerciseClearResizeOsr(i, deoptAfter);
            } else if ("allocationChainOsr".equals(target)) {
                exerciseAllocationChainOsr(i, deoptAfter);
            } else if ("libraryShapeOsr".equals(target)) {
                exerciseLibraryShapeOsr(i, deoptAfter);
            } else if ("weakQueueStormOsr".equals(target)) {
                exerciseWeakQueueStormOsr(i, deoptAfter);
            } else if ("loopPhiSelfOsr".equals(target)) {
                exerciseLoopPhiSelfOsr(i, deoptAfter);
            } else if ("purePhiOsr".equals(target)) {
                exercisePurePhiOsr(i, deoptAfter);
            } else if ("pureZipLoopOsr".equals(target)) {
                exercisePureZipLoopOsr(i, deoptAfter);
            } else if ("lastFieldLoopOsr".equals(target)) {
                exerciseLastFieldLoopOsr(i, deoptAfter);
            } else if ("finalCpuOrderOsr".equals(target)) {
                exerciseFinalCpuOrderOsr(i, deoptAfter);
            } else if ("finalCpuLiteOsr".equals(target)) {
                exerciseFinalCpuLiteOsr(i, deoptAfter);
            } else if ("finalCpuLiteHelperOsr".equals(target)) {
                exerciseFinalCpuLiteHelperOsr(i, deoptAfter);
            } else if ("finalCpuLiteCascadeOsr".equals(target)) {
                exerciseFinalCpuLiteCascadeOsr(i, deoptAfter);
            } else if ("finalCpuLiteSpringCascadeOsr".equals(target)) {
                exerciseFinalCpuLiteSpringCascadeOsr(i, deoptAfter);
            } else if ("fieldMatrix".equals(target)) {
                exerciseFieldMatrix(i);
            } else if ("methodParameter".equals(target)) {
                exerciseMethodParameter(i);
            } else if ("both".equals(target)) {
                exerciseFieldMatrix(i);
                exerciseMethodParameter(i);
            } else {
                exerciseField(i);
            }
            if (deadlineNanos != 0L && System.nanoTime() >= deadlineNanos) {
                break;
            }
        }

        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        System.out.println("completed elapsedMillis=" + elapsedMillis + " sink=" + intSink);
    }

    private static long secondsToNanos(long seconds) {
        if (seconds <= 0L) {
            return 0L;
        }
        return seconds * 1_000_000_000L;
    }

    @SuppressWarnings("unchecked")
    private static BarrierHolder<Object>[] createSharedRing() {
        BarrierHolder<Object>[] holders = new BarrierHolder[8];
        for (int i = 0; i < holders.length; i++) {
            holders[i] = new BarrierHolder<Object>("shared-" + i, i + 1);
        }
        for (int i = 0; i < holders.length; i++) {
            holders[i].next = holders[(i + 1) & (holders.length - 1)];
        }
        return holders;
    }

    private static Unsafe findUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static long objectFieldOffset(Class<?> type, String fieldName) {
        try {
            return UNSAFE.objectFieldOffset(type.getDeclaredField(fieldName));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static URL[] createUrls() {
        try {
            return new URL[] {
                    new URL("file", "", "/tmp/spring-c2-a"),
                    new URL("file", "", "/tmp/spring-c2-b"),
                    new URL("jar", "", "file:/tmp/spring-c2.jar!/a")
            };
        } catch (MalformedURLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static File createZipStormFile() {
        try {
            File file = File.createTempFile("spring-c2-zip-storm", ".zip");
            file.deleteOnExit();
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
            try {
                out.putNextEntry(new ZipEntry("entry.txt"));
                out.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
                out.closeEntry();
            } finally {
                out.close();
            }
            return file;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static CollisionKey[] createCollisionKeys() {
        CollisionKey[] keys = new CollisionKey[256];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = new CollisionKey(i);
        }
        return keys;
    }

    private static SpreadKey[] createResizeKeys() {
        SpreadKey[] keys = new SpreadKey[4096];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = new SpreadKey(i);
        }
        return keys;
    }

    private static void exerciseField(int iteration) {
        Field field = FIELDS[iteration % FIELDS.length];
        Class<?> implementationClass = IMPLEMENTATIONS[(iteration >>> 3) % IMPLEMENTATIONS.length];
        int nestingLevel = 1 + ((iteration >>> 5) & 3);

        ResolvableType type = ResolvableType.forField(field, nestingLevel, implementationClass);
        consume(type, nestingLevel);
    }

    private static void exerciseFieldMatrix(int iteration) {
        Field field = FIELDS[iteration % FIELDS.length];
        Class<?> implementationClass = IMPLEMENTATIONS[(iteration >>> 3) % IMPLEMENTATIONS.length];
        int nestingLevel = 1 + ((iteration >>> 5) & 3);

        ResolvableType owner = ResolvableType.forClass(implementationClass);
        ResolvableType typedOwner = ResolvableType.forClassWithGenerics(ComplexPair.class,
                ResolvableType.forClass(String.class), ResolvableType.forClass(Integer.class));

        consume(ResolvableType.forField(field), nestingLevel);
        consume(ResolvableType.forField(field, implementationClass), nestingLevel);
        consume(ResolvableType.forField(field, owner), nestingLevel);
        consume(ResolvableType.forField(field, typedOwner), nestingLevel);
        consume(ResolvableType.forField(field, nestingLevel), nestingLevel);
        consume(ResolvableType.forField(field, nestingLevel, implementationClass), nestingLevel);
    }

    private static void exerciseAdvanced(int iteration, int deoptAfter) {
        Field field = FIELDS[shuffle(iteration) % FIELDS.length];
        TypeDriver driver = TYPE_DRIVERS[driverIndex(iteration, deoptAfter)];
        Class<?> implementationClass = driver.implementation(iteration);
        int nestingLevel = 1 + ((iteration >>> 4) & 4);

        ResolvableType type = hotWrapperOne(field, nestingLevel, implementationClass, iteration);
        consumeDeep(type, nestingLevel, iteration);
        exerciseBarrierBoxing(iteration, deoptAfter);
        exerciseLoopedBarriers(iteration, deoptAfter);
        if ((iteration & 15) == 0) {
            exerciseOsrMemBarLoop(iteration, deoptAfter);
        }
        triggerUncommonTrap(iteration, deoptAfter);
    }

    private static ResolvableType hotWrapperOne(Field field, int nestingLevel,
            Class<?> implementationClass, int iteration) {
        ResolvableType owner = hotWrapperTwo(implementationClass, iteration);
        if ((iteration & 3) == 0) {
            consume(ResolvableType.forField(field, owner), nestingLevel);
        }
        return hotWrapperThree(field, nestingLevel, implementationClass);
    }

    private static ResolvableType hotWrapperTwo(Class<?> implementationClass, int iteration) {
        if ((iteration & 7) == 0) {
            return ResolvableType.forClassWithGenerics(ComplexPair.class,
                    ResolvableType.forClass(String.class), ResolvableType.forClass(Integer.class));
        }
        return ResolvableType.forClass(implementationClass);
    }

    private static ResolvableType hotWrapperThree(Field field, int nestingLevel,
            Class<?> implementationClass) {
        return ResolvableType.forField(field, nestingLevel, implementationClass);
    }

    private static void consumeDeep(ResolvableType type, int nestingLevel, int iteration) {
        consume(type, nestingLevel);
        ResolvableType generic0 = type.getGeneric(0);
        ResolvableType generic1 = type.getGeneric(1);
        ResolvableType nested = type.getNested(1 + ((iteration >>> 2) & 5));
        ResolvableType deeper = nested.asMap().getGeneric(1).getNested(2);

        sink = deeper;
        intSink ^= generic0.hashCode();
        intSink += generic1.resolve(Object.class).getName().length();
    }

    private static int shuffle(int value) {
        int x = value * 1103515245 + 12345;
        x ^= x >>> 16;
        return x & 0x7fffffff;
    }

    private static int driverIndex(int iteration, int deoptAfter) {
        if (iteration < deoptAfter) {
            return 0;
        }
        return 1 + (iteration & 1);
    }

    private static void triggerUncommonTrap(int iteration, int deoptAfter) {
        Object value = iteration < deoptAfter ? "mostly-string" : Integer.valueOf(iteration);
        try {
            intSink += ((String) value).length();
        } catch (ClassCastException expectedAfterWarmup) {
            intSink ^= expectedAfterWarmup.getClass().getName().length();
        }
    }

    private static void exerciseBarrierBoxing(int iteration, int deoptAfter) {
        Field field = FIELDS[(FIELDS.length - 1) - (shuffle(iteration) % 5)];
        Class<?> implementationClass = (iteration & 1) == 0
                ? BarrierStringRepository.class
                : BarrierIntegerRepository.class;
        int nestingLevel = 1 + ((iteration >>> 3) & 3);

        BarrierHolder<?> holder = new BarrierHolder<Object>(
                iteration < deoptAfter ? "payload" : Integer.valueOf(iteration),
                iteration);
        Object loaded;
        synchronized (LOCK_ONE) {
            holder.sideEffect = Integer.valueOf(iteration + 17);
            publishedObject = holder;
            volatileGuard = iteration;
        }

        synchronized (LOCK_TWO) {
            loaded = publishedObject;
            if (loaded instanceof BarrierHolder) {
                BarrierHolder<?> lockedSeen = (BarrierHolder<?>) loaded;
                intSink ^= lockedSeen.boxedInt.intValue();
                intSink += (int) (lockedSeen.boxedLong.longValue() & 7L);
                sink = lockedSeen.value;
            }
            if ((volatileGuard & 15) == 7) {
                publishedObject = holder.next;
            }
        }

        if (loaded instanceof BarrierHolder) {
            BarrierHolder<?> seen = (BarrierHolder<?>) loaded;
            Integer boxed = Integer.valueOf(iteration & 127);
            Long longBoxed = Long.valueOf(iteration + seen.boxedLong.longValue());
            intSink += boxed.intValue();
            intSink ^= seen.boxedInt.intValue();
            intSink += (int) (longBoxed.longValue() & 31L);
            sink = seen.values[(iteration >>> 1) & 1];
        }

        consume(hotWrapperThree(field, nestingLevel, implementationClass), nestingLevel);
    }

    private static void exerciseLoopedBarriers(int iteration, int deoptAfter) {
        BarrierHolder<?> carried = new BarrierHolder<Object>("loop-start", iteration);
        int rounds = 2 + (iteration & 3);
        for (int round = 0; round < rounds; round++) {
            Object candidate;
            synchronized (LOCK_ONE) {
                candidate = publishedObject;
                if (!(candidate instanceof BarrierHolder)) {
                    candidate = carried;
                    publishedObject = candidate;
                }
            }
            try {
                synchronized (LOCK_TWO) {
                    BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                    intSink += seen.boxedInt.intValue();
                    intSink ^= (int) (seen.boxedLong.longValue() & 127L);
                    if (((iteration + round) & 31) == 3) {
                        publishedObject = new BarrierHolder<Object>(seen.value, iteration + round);
                    }
                    if (iteration >= deoptAfter && round == (rounds - 1)) {
                        candidate = "late-type";
                    }
                    carried = seen.next == null ? seen : seen.next;
                }
            } catch (ClassCastException expectedAfterWarmup) {
                intSink ^= expectedAfterWarmup.getClass().getName().length();
                carried = new BarrierHolder<Object>("recovered", iteration ^ round);
            }
        }
        sink = carried.value;
    }

    private static void exerciseOsrMemBarLoop(int iteration, int deoptAfter) {
        BarrierHolder<?> left = new BarrierHolder<Object>("osr-left", iteration);
        BarrierHolder<?> right = new BarrierHolder<Object>("osr-right", iteration + 1);
        Object candidate = left;
        Field field = FIELDS[(iteration + 29) % FIELDS.length];
        Class<?> implementationClass = (iteration & 2) == 0
                ? BarrierStringRepository.class
                : BarrierIntegerRepository.class;
        int nestingLevel = 1 + ((iteration >>> 2) & 3);
        int local = intSink;

        for (int round = 0; round < 1200; round++) {
            BarrierHolder<?> selected;
            synchronized (LOCK_ONE) {
                selected = ((round + iteration) & 1) == 0 ? left : right;
                selected.sideEffect = Integer.valueOf(round + iteration);
                publishedObject = selected;
                volatileGuard = round;
            }
            local += selected.boxedInt.intValue();
            local ^= (int) (selected.boxedLong.longValue() & 511L);
            sink = selected.value;

            try {
                synchronized (LOCK_TWO) {
                    Object seenObject = osrFlip ? candidate : publishedObject;
                    BarrierHolder<?> seen = (BarrierHolder<?>) seenObject;
                    local += seen.boxedInt.intValue();
                    local ^= (int) (seen.boxedLong.longValue() & 255L);
                    local += seen.values[(round >>> 3) & 1].hashCode();

                    if ((round & 63) == 0) {
                        consume(hotWrapperThree(field, nestingLevel, implementationClass), nestingLevel);
                    }
                    if (round > 900 && iteration >= deoptAfter) {
                        osrFlip = true;
                        candidate = "late-osr-type";
                    } else if ((round & 7) == 0) {
                        candidate = seen.next == null ? seen : seen.next;
                    }
                }
                if (candidate instanceof BarrierHolder) {
                    BarrierHolder<?> afterUnlock = (BarrierHolder<?>) candidate;
                    local += afterUnlock.boxedInt.intValue();
                    local ^= (int) (afterUnlock.boxedLong.longValue() & 1023L);
                    sink = afterUnlock.values[round & 1];
                }
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                osrFlip = false;
                candidate = new BarrierHolder<Object>("osr-recovered", iteration ^ round);
            }

            if ((local & 1023) == 17) {
                ResolvableType.clearCache();
            }
            if ((round & 15) == 5) {
                local = exerciseNestedReleaseChain(iteration, round, selected, local);
            }
            if ((round & 15) == 9) {
                local = exerciseConvergedRelease(iteration, round, selected, local);
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseShortOsrVariants(int iteration, int deoptAfter) {
        switch (iteration & 3) {
            case 0:
                exerciseShortOsrVariantA(iteration, deoptAfter);
                break;
            case 1:
                exerciseShortOsrVariantB(iteration, deoptAfter);
                break;
            case 2:
                exerciseShortOsrVariantC(iteration, deoptAfter);
                break;
            default:
                exerciseShortOsrVariantD(iteration, deoptAfter);
                break;
        }
    }

    private static void exerciseShortOsrDriver(int iteration, int deoptAfter) {
        BarrierHolder<?> left = new BarrierHolder<Object>("driver-left", iteration);
        BarrierHolder<?> right = new BarrierHolder<Object>("driver-right", iteration + 1);
        Object candidate = left;
        Field field = FIELDS[(iteration + 23) % FIELDS.length];
        MethodParameter parameter = METHOD_PARAMETERS[iteration % METHOD_PARAMETERS.length]
                .withContainingClass((iteration & 1) == 0 ? DeepStringRepository.class : DeepIntegerRepository.class);
        int local = intSink;

        for (int round = 0; round < 2400; round++) {
            BarrierHolder<?> selected = ((round ^ iteration) & 1) == 0 ? left : right;
            synchronized (LOCK_ONE) {
                selected.sideEffect = Integer.valueOf(local + round);
                publishedObject = selected;
                volatileGuard = local;
            }

            if ((round & 3) == 0) {
                consume(hotWrapperThree(field, 1 + ((round >>> 2) & 3),
                        (round & 8) == 0 ? BarrierStringRepository.class : BarrierIntegerRepository.class), 1);
            } else if ((round & 3) == 1) {
                consume(ResolvableType.forMethodParameter(parameter), 1 + (round & 3));
            } else if ((round & 3) == 2) {
                local = exerciseConvergedRelease(iteration, round, selected, local);
            } else {
                synchronized (LOCK_TWO) {
                    candidate = iteration >= deoptAfter && round > 1600
                            ? Integer.valueOf(round)
                            : publishedObject;
                }
            }

            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                local += seen.boxedInt.intValue();
                local ^= (int) (seen.boxedLong.longValue() & 16383L);
                sink = seen.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().length();
                candidate = selected;
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseSpringCachePurgeOsr(int iteration, int deoptAfter) {
        ConcurrentReferenceHashMap<Object, Object> map =
                new ConcurrentReferenceHashMap<Object, Object>(128, ConcurrentReferenceHashMap.ReferenceType.WEAK);
        BarrierHolder<?> holder = new BarrierHolder<Object>("cache", iteration);
        Object candidate = holder;
        int local = intSink;

        for (int round = 0; round < 2600; round++) {
            Field field = FIELDS[(iteration + round) % FIELDS.length];
            MethodParameter parameter = METHOD_PARAMETERS[(iteration + round) % METHOD_PARAMETERS.length]
                    .withContainingClass((round & 1) == 0 ? DeepStringRepository.class : DeepIntegerRepository.class);
            ResolvableType type = ((round & 1) == 0)
                    ? hotWrapperThree(field, 1 + ((round >>> 3) & 3), IMPLEMENTATIONS[(round >>> 2) % IMPLEMENTATIONS.length])
                    : ResolvableType.forMethodParameter(parameter);

            synchronized (LOCK_ONE) {
                holder.sideEffect = type;
                publishedObject = holder;
                map.put(field, type);
                map.put(parameter, ResolvableType.forClass(holder.getClass()));
            }

            if ((round & 7) == 3) {
                map.remove(field);
            }
            if ((round & 15) == 7) {
                map.purgeUnreferencedEntries();
            }
            if ((round & 63) == 11) {
                ResolvableType.clearCache();
            }

            synchronized (LOCK_TWO) {
                candidate = iteration >= deoptAfter && round > 1700
                        ? Long.valueOf(round)
                        : publishedObject;
            }

            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                local += seen.boxedInt.intValue();
                local ^= map.size();
                sink = seen.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = holder;
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseSharedCacheRingOsr(int iteration, int deoptAfter) {
        BarrierHolder<Object> holder = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        Object candidate = holder;
        int local = intSink;

        for (int round = 0; round < 3600; round++) {
            Field field = FIELDS[(iteration + (round * 3)) % FIELDS.length];
            MethodParameter parameter = METHOD_PARAMETERS[(iteration + round) % METHOD_PARAMETERS.length]
                    .withContainingClass((round & 2) == 0 ? DeepStringRepository.class : DeepIntegerRepository.class);
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + (round >>> 2)) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((round >>> 3) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(round + iteration) & (SHARED_RING.length - 1)];
            BarrierHolder<Object> next = selected.next;
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);
            ResolvableType parameterType = ResolvableType.forMethodParameter(parameter);
            ResolvableType pairType = ResolvableType.forClassWithGenerics(ComplexPair.class,
                    ResolvableType.forClass(String.class), ResolvableType.forClass(Integer.class));

            synchronized (LOCK_ONE) {
                selected.sideEffect = type;
                next.sideEffect = parameterType;
                publishedObject = selected;
                volatileGuard = local + round;

                synchronized (LOCK_TWO) {
                    next.sideEffect = pairType;
                    publishedObject = ((round & 1) == 0) ? next : selected;
                    next.sideEffect = selected;
                    publishedObject = next;
                    volatileGuard = local ^ round;
                }
                selected.sideEffect = next;
                publishedObject = selected;
                volatileGuard = local - round;
            }

            local += selected.boxedInt.intValue();
            local ^= next.boxedInt.intValue();
            local += (int) ((selected.boxedLong.longValue() + next.boxedLong.longValue()) & 4095L);
            consume(type, nestingLevel);
            SHARED_CACHE.put(field, type);
            SHARED_CACHE.put(selected, parameterType);
            SHARED_CACHE.put(parameter, pairType);

            if ((round & 7) == 1) {
                SHARED_CACHE.remove(field);
            }
            if ((round & 15) == 5) {
                SHARED_CACHE.purgeUnreferencedEntries();
            }
            if ((round & 63) == 17) {
                ResolvableType.clearCache();
            }

            synchronized (LOCK_TWO) {
                Object seen = ((round & 3) == 0) ? selected.next : publishedObject;
                candidate = iteration >= deoptAfter && round > 2300
                        ? Integer.valueOf(round)
                        : seen;
            }

            try {
                BarrierHolder<?> cast = (BarrierHolder<?>) candidate;
                local += cast.boxedInt.intValue();
                local ^= SHARED_CACHE.size();
                sink = cast.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = selected;
                osrFlip = !osrFlip;
            }

            if (osrFlip && (round & 31) == 9) {
                local = exerciseNestedReleaseChain(iteration, round, next, local);
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseUnsafeFenceOsr(int iteration, int deoptAfter) {
        BarrierHolder<Object> holder = SHARED_RING[(iteration + 3) & (SHARED_RING.length - 1)];
        Object candidate = holder;
        int local = intSink;

        for (int round = 0; round < 4200; round++) {
            Field field = FIELDS[(iteration + (round * 5)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + (round >>> 1)) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((round >>> 4) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];
            BarrierHolder<Object> next = selected.next;
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);

            selected.sideEffect = type;
            publishedObject = selected;
            volatileGuard = local + round;
            UNSAFE.storeFence();

            local += selected.boxedInt.intValue();
            local ^= (int) (selected.boxedLong.longValue() & 8191L);
            local += next.boxedInt.intValue();
            consume(type, nestingLevel);

            if ((round & 3) == 1) {
                next.sideEffect = ResolvableType.forClassWithGenerics(ComplexPair.class,
                        ResolvableType.forClass(String.class), ResolvableType.forClass(Integer.class));
                publishedObject = next;
                volatileGuard = local ^ round;
                UNSAFE.fullFence();
            } else {
                UNSAFE.loadFence();
            }

            if ((round & 7) == 2) {
                SHARED_CACHE.put(field, type);
                SHARED_CACHE.put(selected, next.sideEffect);
            }
            if ((round & 15) == 6) {
                SHARED_CACHE.purgeUnreferencedEntries();
            }
            if ((round & 63) == 21) {
                ResolvableType.clearCache();
            }

            candidate = iteration >= deoptAfter && round > 2600
                    ? Long.valueOf(round)
                    : ((round & 1) == 0 ? publishedObject : selected.next);

            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                UNSAFE.loadFence();
                local += seen.boxedInt.intValue();
                local ^= (int) (seen.boxedLong.longValue() & 16383L);
                sink = seen.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = selected;
                osrFlip = !osrFlip;
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseIrreducibleLockOsr(int iteration, int deoptAfter) {
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        outer:
        for (int round = 0; round < 3600; round++) {
            Field field = FIELDS[(iteration + (round * 7)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + round) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((round >>> 2) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(round + iteration) & (SHARED_RING.length - 1)];
            BarrierHolder<Object> next = selected.next;

            try {
                switch ((round ^ iteration) & 7) {
                    case 0:
                    case 3:
                        synchronized (LOCK_ONE) {
                            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);
                            selected.sideEffect = type;
                            publishedObject = selected;
                            volatileGuard = local + round;
                            local += selected.boxedInt.intValue();
                            consume(type, nestingLevel);
                            if ((round & 31) == 0) {
                                continue outer;
                            }
                        }
                        break;
                    case 1:
                    case 6:
                        synchronized (LOCK_TWO) {
                            candidate = iteration >= deoptAfter && round > 2400
                                    ? "irreducible-late"
                                    : publishedObject;
                            BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                            local += seen.boxedInt.intValue();
                            local ^= (int) (seen.boxedLong.longValue() & 32767L);
                            if ((round & 15) == 1) {
                                break;
                            }
                            synchronized (LOCK_ONE) {
                                next.sideEffect = seen;
                                publishedObject = next;
                                volatileGuard = local ^ round;
                            }
                        }
                        break;
                    default:
                        synchronized (LOCK_ONE) {
                            selected.sideEffect = next;
                            publishedObject = selected;
                            volatileGuard = round;
                            synchronized (LOCK_TWO) {
                                ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);
                                next.sideEffect = type;
                                candidate = next;
                                local ^= type.hashCode();
                            }
                        }
                        break;
                }
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = selected;
            } finally {
                if ((round & 63) == 5) {
                    ResolvableType.clearCache();
                }
                if ((round & 15) == 9) {
                    SHARED_CACHE.put(field, candidate);
                }
            }

            if (candidate instanceof BarrierHolder) {
                BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                local += seen.boxedInt.intValue();
                sink = seen.values[round & 1];
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseSegmentStormOsr(int iteration, int deoptAfter) {
        HotReferenceMap map = SEGMENT_STORM_CACHE;
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        for (int round = 0; round < 4200; round++) {
            CollisionKey key = COLLISION_KEYS[(iteration + round) & (COLLISION_KEYS.length - 1)];
            CollisionKey removeKey = COLLISION_KEYS[(iteration + round + 127) & (COLLISION_KEYS.length - 1)];
            Field field = FIELDS[(iteration + (round * 11)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + (round >>> 2)) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((round >>> 3) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);

            map.segmentLock();
            try {
                selected.sideEffect = type;
                publishedObject = selected;
                volatileGuard = local + round;
                map.put(key, type);
                if ((round & 1) == 0) {
                    map.put(COLLISION_KEYS[(round * 13) & (COLLISION_KEYS.length - 1)], selected);
                }
                local += selected.boxedInt.intValue();
                local ^= (int) (selected.boxedLong.longValue() & 65535L);
            } finally {
                map.segmentUnlock();
            }

            consume(type, nestingLevel);
            Object value = map.get(key);
            if ((round & 3) == 1) {
                map.remove(removeKey);
            }
            if ((round & 7) == 3) {
                map.purgeUnreferencedEntries();
            }
            if ((round & 63) == 19) {
                ResolvableType.clearCache();
            }

            candidate = iteration >= deoptAfter && round > 2800
                    ? Integer.valueOf(round)
                    : (value == null ? selected : value);
            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                local += seen.boxedInt.intValue();
                sink = seen.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = selected;
            }

            local ^= map.segmentCount();
            local += map.segmentSize();
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseSegmentPhiOsr(int iteration, int deoptAfter) {
        HotReferenceMap map = SEGMENT_STORM_CACHE;
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        for (int round = 0; round < 5200; round++) {
            CollisionKey key = COLLISION_KEYS[(iteration + (round * 3)) & (COLLISION_KEYS.length - 1)];
            CollisionKey neighbor = COLLISION_KEYS[(iteration + round + 1) & (COLLISION_KEYS.length - 1)];
            Field field = FIELDS[(iteration + (round * 17)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + round) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((round + iteration) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];
            BarrierHolder<Object> next = selected.next;
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);

            map.put(key, type);
            if ((round & 1) == 0) {
                map.put(neighbor, selected);
            } else {
                map.remove(neighbor);
            }
            if ((round & 3) == 2) {
                map.purgeUnreferencedEntries();
            }

            map.segmentLock();
            try {
                selected.sideEffect = type;
                next.sideEffect = candidate;
                publishedObject = selected;
                volatileGuard = local ^ round;
                Object value = map.get((round & 1) == 0 ? key : neighbor);
                candidate = value == null ? selected : value;
                local += selected.boxedInt.intValue();
                local ^= (int) (next.boxedLong.longValue() & 65535L);
            } finally {
                map.segmentUnlock();
            }

            local += map.segmentCount();
            local ^= map.segmentSize();
            consume(type, nestingLevel);

            if ((round & 31) == 7) {
                synchronized (LOCK_ONE) {
                    SHARED_CACHE.put(field, type);
                    SHARED_CACHE.put(selected, candidate);
                    publishedObject = next;
                    volatileGuard = local + round;
                }
            }
            if ((round & 63) == 11) {
                ResolvableType.clearCache();
            }

            if (iteration >= deoptAfter && round > 3600) {
                candidate = "segment-phi-deopt";
            }
            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                local += seen.boxedInt.intValue();
                local ^= (int) (seen.boxedLong.longValue() & 4095L);
                sink = seen.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = selected;
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseResizeStormOsr(int iteration, int deoptAfter) {
        HotReferenceMap map = new HotReferenceMap();
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        for (int round = 0; round < 3600; round++) {
            SpreadKey key = RESIZE_KEYS[(iteration + round) & (RESIZE_KEYS.length - 1)];
            SpreadKey removeKey = RESIZE_KEYS[(iteration + round - 257) & (RESIZE_KEYS.length - 1)];
            Field field = FIELDS[(iteration + (round * 19)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + (round >>> 1)) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((round >>> 2) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);

            map.put(key, type);
            if ((round & 3) == 0) {
                map.put(RESIZE_KEYS[(round * 17) & (RESIZE_KEYS.length - 1)], selected);
            }
            if ((round & 15) == 5) {
                map.remove(removeKey);
            }
            if ((round & 31) == 13) {
                map.purgeUnreferencedEntries();
            }

            map.segmentLock();
            try {
                selected.sideEffect = candidate;
                publishedObject = selected;
                volatileGuard = local + round;
                candidate = map.get(key);
                local += selected.boxedInt.intValue();
                local ^= (int) (selected.boxedLong.longValue() & 65535L);
            } finally {
                map.segmentUnlock();
            }

            consume(type, nestingLevel);
            local ^= map.segmentCount();
            local += map.segmentSize();

            if (iteration >= deoptAfter && round > 2400) {
                candidate = Integer.valueOf(round);
            }
            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) (candidate == null ? selected : candidate);
                local += seen.boxedInt.intValue();
                sink = seen.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = selected;
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseMonitorResizeOsr(int iteration, int deoptAfter) {
        HotReferenceMap map = new HotReferenceMap();
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        for (int round = 0; round < 4200; round++) {
            SpreadKey key = RESIZE_KEYS[(iteration + round) & (RESIZE_KEYS.length - 1)];
            SpreadKey secondary = RESIZE_KEYS[(iteration + (round * 29)) & (RESIZE_KEYS.length - 1)];
            Field field = FIELDS[(iteration + (round * 23)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + round) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((round + iteration) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];
            BarrierHolder<Object> next = selected.next;
            ResolvableType type;

            synchronized (LOCK_ONE) {
                type = hotWrapperThree(field, nestingLevel, implementationClass);
                selected.sideEffect = candidate;
                publishedObject = selected;
                volatileGuard = local ^ round;
                map.put(key, type);
                local += selected.boxedInt.intValue();
            }

            synchronized (LOCK_TWO) {
                map.put(secondary, selected);
                if ((round & 7) == 3) {
                    map.purgeUnreferencedEntries();
                }
                candidate = map.get((round & 1) == 0 ? key : secondary);
                next.sideEffect = candidate;
                local ^= (int) (next.boxedLong.longValue() & 65535L);
            }

            synchronized (LOCK_ONE) {
                if ((round & 15) == 9) {
                    map.remove(RESIZE_KEYS[(iteration + round - 513) & (RESIZE_KEYS.length - 1)]);
                }
                SHARED_CACHE.put(field, type);
                publishedObject = next;
                volatileGuard = local + map.segmentCount();
                local += map.segmentSize();
            }

            consume(type, nestingLevel);
            if (iteration >= deoptAfter && round > 3000) {
                candidate = Long.valueOf(round);
            }
            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) (candidate == null ? selected : candidate);
                local += seen.boxedInt.intValue();
                sink = seen.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = selected;
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseBranchFenceOsr(int iteration, int deoptAfter) {
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        for (int round = 0; round < 5200; round++) {
            Field field = FIELDS[(iteration + (round * 31)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + round) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((iteration + round) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];
            BarrierHolder<Object> next = selected.next;
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);

            if (((round ^ iteration) & 3) == 0) {
                selected.sideEffect = type;
                publishedObject = selected;
                volatileGuard = local + round;
                UNSAFE.storeFence();
                candidate = selected;
                UNSAFE.loadFence();
            } else if (((round ^ iteration) & 3) == 1) {
                synchronized (LOCK_ONE) {
                    next.sideEffect = candidate;
                    publishedObject = next;
                    volatileGuard = local ^ round;
                    candidate = next;
                }
                UNSAFE.storeFence();
            } else if (((round ^ iteration) & 3) == 2) {
                synchronized (LOCK_TWO) {
                    selected.sideEffect = next;
                    SHARED_CACHE.put(field, type);
                    publishedObject = selected;
                    volatileGuard = round;
                }
                candidate = iteration >= deoptAfter && round > 3600 ? "branch-fence" : selected;
                UNSAFE.loadFence();
            } else {
                selected.sideEffect = SHARED_CACHE.get(field);
                publishedObject = selected;
                volatileGuard = local - round;
                UNSAFE.storeFence();
                UNSAFE.loadFence();
                candidate = selected;
            }

            BarrierHolder<?> seen;
            try {
                seen = (BarrierHolder<?>) candidate;
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                seen = selected;
                candidate = selected;
            }
            local += seen.boxedInt.intValue();
            local ^= (int) (seen.boxedLong.longValue() & 65535L);
            sink = seen.values[round & 1];
            consume(type, nestingLevel);

            if ((round & 63) == 17) {
                ResolvableType.clearCache();
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseClearResizeOsr(int iteration, int deoptAfter) {
        HotReferenceMap map = SEGMENT_STORM_CACHE;
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        for (int round = 0; round < 4800; round++) {
            if ((round & 127) == 0) {
                map.clear();
            }
            Field field = FIELDS[(iteration + (round * 37)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + (round >>> 1)) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((round + iteration) & 3);
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];

            for (int burst = 0; burst < 6; burst++) {
                SpreadKey key = RESIZE_KEYS[(iteration + round * 17 + burst) & (RESIZE_KEYS.length - 1)];
                Object value = (burst & 1) == 0 ? type : selected;
                map.put(key, value);
                candidate = map.get(key);
            }

            if ((round & 7) == 3) {
                map.purgeUnreferencedEntries();
            }
            if ((round & 31) == 11) {
                map.remove(RESIZE_KEYS[(iteration + round - 97) & (RESIZE_KEYS.length - 1)]);
            }

            map.segmentLock();
            try {
                selected.sideEffect = candidate;
                publishedObject = selected;
                volatileGuard = local ^ round;
                local += selected.boxedInt.intValue();
                local ^= (int) (selected.boxedLong.longValue() & 65535L);
            } finally {
                map.segmentUnlock();
            }

            if (iteration >= deoptAfter && round > 3400) {
                candidate = Float.valueOf(round);
            }
            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) (candidate instanceof BarrierHolder ? candidate : selected);
                local += seen.boxedInt.intValue();
                sink = seen.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = selected;
            }
            consume(type, nestingLevel);
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseAllocationChainOsr(int iteration, int deoptAfter) {
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        for (int round = 0; round < 7000; round++) {
            Field field = FIELDS[(iteration + (round * 41)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + (round >>> 1)) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((iteration + round) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];
            BarrierHolder<Object> next = selected.next;
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);

            FinalPair first = new FinalPair(selected, type, local + round);
            FinalPair second = new FinalPair(first, next, local ^ round);
            FinalPair third;
            if ((round & 1) == 0) {
                UNSAFE.storeFence();
                third = new FinalPair(second, candidate, local - round);
            } else {
                synchronized (LOCK_ONE) {
                    publishedObject = second;
                    volatileGuard = local + round;
                    third = new FinalPair(next, second, local + selected.boxedInt.intValue());
                }
            }

            Object left = third.left;
            Object right = third.right;
            Object nested = ((FinalPair) second.left).left;
            local += third.marker.intValue();
            local ^= second.marker.intValue();
            if (left instanceof BarrierHolder) {
                BarrierHolder<?> seen = (BarrierHolder<?>) left;
                local += seen.boxedInt.intValue();
                sink = seen.values[round & 1];
            } else if (right instanceof BarrierHolder) {
                BarrierHolder<?> seen = (BarrierHolder<?>) right;
                local ^= seen.boxedInt.intValue();
                sink = seen.values[round & 1];
            } else if (nested instanceof BarrierHolder) {
                BarrierHolder<?> seen = (BarrierHolder<?>) nested;
                local += (int) (seen.boxedLong.longValue() & 65535L);
                sink = seen.values[round & 1];
            }

            SHARED_CACHE.put(field, type);
            if ((round & 31) == 15) {
                SHARED_CACHE.purgeUnreferencedEntries();
            }
            if (iteration >= deoptAfter && round > 5200) {
                candidate = "allocation-chain";
            } else {
                candidate = third;
            }
            consume(type, nestingLevel);
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseLibraryShapeOsr(int iteration, int deoptAfter) {
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        for (int round = 0; round < 6400; round++) {
            Field field = FIELDS[(iteration + (round * 43)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + (round >>> 2)) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((iteration + round) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);

            Charset charset = Charset.forName((round & 1) == 0 ? "UTF-8" : "ISO-8859-1");
            ByteBuffer buffer = ByteBuffer.allocateDirect(96 + ((round & 3) << 3));
            buffer.putInt(0, local + round);
            LongBuffer longs = buffer.asLongBuffer();
            longs.put(0, ((long) local << 32) ^ round);
            URL url = URLS[round % URLS.length];

            synchronized (LOCK_ONE) {
                selected.sideEffect = type;
                publishedObject = selected;
                volatileGuard = charset.name().length() + round;
                LIBRARY_SHAPE_MAP.put(field, type);
                LIBRARY_SHAPE_MAP.put(charset, selected);
                LIBRARY_SHAPE_MAP.put(url, buffer);
            }

            Object mapValue = LIBRARY_SHAPE_MAP.get((round & 1) == 0 ? field : charset);
            if ((round & 7) == 3) {
                LIBRARY_SHAPE_MAP.remove(URLS[(round >>> 3) % URLS.length]);
            }
            if ((round & 31) == 11) {
                ResolvableType.clearCache();
                Charset.availableCharsets().get("UTF-8");
            }

            if ((round & 3) == 0) {
                candidate = type;
            } else if ((round & 3) == 1) {
                candidate = mapValue == null ? type : mapValue;
            } else if (iteration >= deoptAfter && round > 4600) {
                candidate = url;
            } else {
                candidate = selected;
            }

            try {
                ResolvableType seenType = (ResolvableType) candidate;
                consume(seenType, nestingLevel);
                local ^= seenType.hashCode();
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = type;
            }

            if (candidate instanceof BarrierHolder) {
                BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                local += seen.boxedInt.intValue();
                sink = seen.values[round & 1];
            }

            local += charset.hashCode();
            local ^= (int) longs.get(0);
            local += url.getProtocol().length();
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseWeakQueueStormOsr(int iteration, int deoptAfter) {
        WeakHashMap<Object, Object> weakMap = new WeakHashMap<Object, Object>(512);
        Vector<Object> vector = new Vector<Object>(32);
        Object anchor = new Object();
        Object candidate = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        int local = intSink;

        weakMap.put(anchor, candidate);
        vector.add(anchor);
        vector.add(candidate);
        for (int round = 0; round < 7600; round++) {
            Field field = FIELDS[(iteration + (round * 47)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + (round >>> 1)) % IMPLEMENTATIONS.length];
            int nestingLevel = 1 + ((iteration ^ round) & 3);
            BarrierHolder<Object> selected = SHARED_RING[(iteration + round) & (SHARED_RING.length - 1)];
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);
            MethodParameter parameter = METHOD_PARAMETERS[(iteration + round) % METHOD_PARAMETERS.length]
                    .withContainingClass((round & 1) == 0 ? DeepStringRepository.class : DeepIntegerRepository.class);
            ResolvableType parameterType = ResolvableType.forMethodParameter(parameter, field.getGenericType());
            consume(parameterType, nestingLevel);
            FinalPair barrierPair = new FinalPair(selected, type, local + round);

            Object weakKey = new WeakStormKey(round, local);
            synchronized (LOCK_ONE) {
                selected.sideEffect = barrierPair;
                publishedObject = barrierPair;
                volatileGuard = local + round;
            }
            synchronized (weakMap) {
                weakMap.put(weakKey, type);
                weakMap.put(new WeakStormKey(round + 17, local ^ round), selected);
                if ((round & 1) == 0) {
                    weakMap.put(new WeakStormKey(round + 257, local + selected.boxedInt.intValue()), field);
                }
                candidate = weakMap.get((round & 7) == 0 ? anchor : weakKey);
                if ((round & 3) == 1) {
                    weakMap.remove(weakKey);
                }
                local ^= weakMap.size();
            }
            synchronized (LOCK_TWO) {
                Object published = publishedObject;
                if (published instanceof FinalPair) {
                    FinalPair seenPair = (FinalPair) published;
                    local += seenPair.marker.intValue();
                    selected.sideEffect = seenPair.right;
                }
                volatileGuard = local ^ round;
            }
            Object pairLeft = barrierPair.left;
            Object pairRight = barrierPair.right;
            local += barrierPair.marker.intValue();
            if (pairLeft instanceof BarrierHolder) {
                BarrierHolder<?> seen = (BarrierHolder<?>) pairLeft;
                local ^= seen.boxedInt.intValue();
            }
            if (pairRight instanceof ResolvableType) {
                consume((ResolvableType) pairRight, nestingLevel);
            }
            local = compactMemBarChain(barrierPair, local, round);
            if ((round & 1) == 1) {
                local = exerciseZipCloseChain(local, round);
            }
            if ((round & 3) == 0) {
                local = exercisePseudoZipChain(local, round, barrierPair);
            }
            if ((round & 3) != 2) {
                local = exerciseVectorChain(vector, barrierPair, type, local, round);
            }

            if ((round & 63) == 7) {
                System.gc();
            }
            if ((round & 31) == 13) {
                synchronized (LOCK_ONE) {
                    publishedObject = selected;
                    volatileGuard = local + round;
                    LIBRARY_SHAPE_MAP.put(new FinalPair(field, type, round), selected);
                    local += LIBRARY_SHAPE_MAP.size();
                }
            }
            if ((round & 63) == 19) {
                ResolvableType.clearCache();
            }

            if ((round & 3) == 0) {
                candidate = type;
            } else if (candidate == null) {
                candidate = selected;
            } else if (iteration >= deoptAfter && round > 5400) {
                candidate = Integer.valueOf(round);
            }

            try {
                ResolvableType seenType = (ResolvableType) candidate;
                consume(seenType, nestingLevel);
                local += seenType.hashCode();
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = type;
            }

            local += selected.boxedInt.intValue();
            sink = selected.values[round & 1];
        }

        intSink = local;
        sink = candidate;
    }

    private static void exercisePurePhiOsr(int iteration, int deoptAfter) {
        BarrierHolder<Object> selected = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        Field field = FIELDS[iteration % FIELDS.length];
        PseudoZip.PseudoInput first = PSEUDO_ZIP.getInput(selected);
        PseudoZip.PseudoInput second = PSEUDO_ZIP.getInput(field);
        PseudoZip.PseudoInput current = first;
        PseudoZip.PseudoInput alternate = second;
        Object candidate = current;
        int local = intSink;

        for (int round = 0; round < 9800; round++) {
            int nestingLevel = 1 + ((round ^ iteration) & 3);
            Field activeField = FIELDS[(iteration + (round * 53)) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + (round >>> 1)) % IMPLEMENTATIONS.length];
            ResolvableType type = hotWrapperThree(activeField, nestingLevel, implementationClass);
            FinalPair pair = new FinalPair(current, type, local + round);

            synchronized (LOCK_ONE) {
                selected.sideEffect = pair;
                publishedObject = current;
                volatileGuard = local + round;
                PSEUDO_ZIP.pureClose(current);
            }

            if ((round & 1) == 0) {
                synchronized (LOCK_TWO) {
                    candidate = publishedObject;
                    PSEUDO_ZIP.pureClose(alternate);
                    current = alternate;
                    alternate = candidate instanceof PseudoZip.PseudoInput
                            ? (PseudoZip.PseudoInput) candidate
                            : first;
                }
            } else {
                try {
                    PSEUDO_ZIP.pureClose(current);
                    if ((round & 7) == 3) {
                        current.close();
                    }
                } finally {
                    PseudoZip.PseudoInput swap = current;
                    current = alternate;
                    alternate = swap;
                }
            }

            if ((round & 3) == 0) {
                PSEUDO_ZIP.crossClose(current, alternate, round);
            }
            if ((round & 15) == 5) {
                PSEUDO_ZIP.afterClose(current);
            }
            if ((round & 63) == 17) {
                ResolvableType.clearCache();
            }

            try {
                PseudoZip.PseudoInput seen = (PseudoZip.PseudoInput) candidate;
                local += seen.read();
                local ^= pair.marker.intValue();
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                candidate = iteration >= deoptAfter && round > 7200
                        ? Integer.valueOf(round)
                        : current;
            }
            consume(type, nestingLevel);
        }

        intSink = local;
        sink = candidate;
    }

    private static void exercisePureZipLoopOsr(int iteration, int deoptAfter) {
        BarrierHolder<Object> selected = SHARED_RING[(iteration + 3) & (SHARED_RING.length - 1)];
        PseudoZip.PseudoInput first = PSEUDO_ZIP.getInput(selected);
        PseudoZip.PseudoInput second = PSEUDO_ZIP.getInput(FIELDS[iteration % FIELDS.length]);
        Object candidate = first;
        int local = intSink;

        for (int outer = 0; outer < 6; outer++) {
            Field field = FIELDS[(iteration + outer * 7) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + outer) % IMPLEMENTATIONS.length];
            ResolvableType type = hotWrapperThree(field, 1 + ((iteration + outer) & 3), implementationClass);
            consume(type, 1 + (outer & 3));
            local = PSEUDO_ZIP.loopPhiClose(first, second, type, local + outer, deoptAfter);
            if ((outer & 1) == 0) {
                candidate = second;
                second = PSEUDO_ZIP.getInput(type);
            } else {
                candidate = first;
                first = PSEUDO_ZIP.getInput(field);
            }
            if (iteration >= deoptAfter && outer > 3) {
                candidate = Integer.valueOf(local);
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseLastFieldLoopOsr(int iteration, int deoptAfter) {
        PseudoZip.PseudoInput first = PSEUDO_ZIP.getInput(SHARED_RING[iteration & (SHARED_RING.length - 1)]);
        PseudoZip.PseudoInput second = PSEUDO_ZIP.getInput(FIELDS[(iteration + 11) % FIELDS.length]);
        Object candidate = first;
        int local = intSink;

        for (int outer = 0; outer < 8; outer++) {
            Field field = FIELDS[(iteration + outer * 3) % FIELDS.length];
            Class<?> implementationClass = IMPLEMENTATIONS[(iteration + outer * 5) % IMPLEMENTATIONS.length];
            ResolvableType type = hotWrapperThree(field, 1 + ((iteration + outer) & 3), implementationClass);
            local = PSEUDO_ZIP.lastFieldLoop(first, second, type, local + outer, deoptAfter);
            consume(type, 1 + (outer & 3));

            if ((outer & 1) == 0) {
                candidate = second;
                second = PSEUDO_ZIP.getInput(type);
            } else {
                candidate = first;
                first = PSEUDO_ZIP.getInput(field);
            }
            if (iteration >= deoptAfter && outer > 4) {
                candidate = Integer.valueOf(local);
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static void exerciseFinalCpuOrderOsr(int iteration, int deoptAfter) {
        Field field = FIELDS[(iteration * 7 + 3) % FIELDS.length];
        Class<?> implementationClass = IMPLEMENTATIONS[(iteration * 5 + 1) % IMPLEMENTATIONS.length];
        Object carried = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        Object alternate = FIELDS[(iteration + 17) % FIELDS.length];
        int nestingLevel = 1 + (iteration & 3);
        int local = intSink;

        for (int round = 0; round < 11200; round++) {
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);
            FinalCpuBox box = new FinalCpuBox(carried, type, local + round);
            Object finalLoad = box.left;
            Object unsafeFinalLoad = UNSAFE.getObjectVolatile(box, FINAL_CPU_LEFT_OFFSET);

            UNSAFE.putOrderedObject(box, FINAL_CPU_MIRROR_OFFSET, unsafeFinalLoad == null ? alternate : unsafeFinalLoad);
            Object mirror = UNSAFE.getObjectVolatile(box, FINAL_CPU_MIRROR_OFFSET);
            UNSAFE.putOrderedObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET, box);
            Object cellSeen = UNSAFE.getObjectVolatile(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET);

            if ((round & 3) == 1) {
                if (UNSAFE.compareAndSwapObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET, cellSeen, mirror)) {
                    carried = finalLoad == null ? box : finalLoad;
                } else {
                    carried = cellSeen;
                }
            } else if ((round & 3) == 2) {
                FinalCpuBox nested = new FinalCpuBox(box, unsafeFinalLoad, local ^ round);
                Object nestedFinal = nested.right;
                UNSAFE.putOrderedObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET, nested);
                carried = nestedFinal == null ? nested : nestedFinal;
            } else {
                carried = mirror == null ? box : mirror;
            }

            if ((round & 15) == 7) {
                alternate = new FinalCpuBox(carried, alternate, local + box.marker.intValue());
            }
            if ((round & 63) == 11) {
                consume(type, nestingLevel);
            }
            if (round > 8400 && iteration >= deoptAfter) {
                carried = Integer.valueOf(round);
            }
            local ^= box.marker.intValue();
            local += carried == null ? 1 : carried.hashCode();
        }

        intSink = local;
        sink = carried;
    }

    private static void exerciseFinalCpuLiteOsr(int iteration, int deoptAfter) {
        Field field = FIELDS[(iteration * 11 + 5) % FIELDS.length];
        Class<?> implementationClass = IMPLEMENTATIONS[(iteration * 3 + 2) % IMPLEMENTATIONS.length];
        Object carried = SHARED_RING[(iteration + 1) & (SHARED_RING.length - 1)];
        Object alternate = FIELDS[(iteration + 19) % FIELDS.length];
        int nestingLevel = 1 + ((iteration >>> 1) & 3);
        int local = intSink;

        for (int round = 0; round < 12400; round++) {
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);
            FinalCpuLite box = finalCpuLiteStep(carried, type, alternate, local + round);
            Object left = box.left;
            Object right = box.right;
            Object mirror = box.mirror;

            if ((round & 3) == 0) {
                carried = left == null ? box : left;
            } else if ((round & 3) == 1) {
                carried = mirror == null ? right : mirror;
            } else if ((round & 3) == 2) {
                FinalCpuLite nested = new FinalCpuLite(box, mirror, carried, local ^ round);
                Object nestedLeft = nested.left;
                carried = nestedLeft == null ? nested : nestedLeft;
            } else {
                Object seen = UNSAFE.getObjectVolatile(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET);
                carried = seen == null ? box : seen;
            }

            if ((round & 31) == 7) {
                UNSAFE.putOrderedObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET, box);
            }
            if ((round & 127) == 13) {
                consume(type, nestingLevel);
            }
            if (round > 9000 && iteration >= deoptAfter) {
                carried = Integer.valueOf(round);
            }
            local ^= box.marker;
            local += carried == null ? 3 : 7;
        }

        intSink = local;
        sink = carried;
    }

    private static void exerciseFinalCpuLiteHelperOsr(int iteration, int deoptAfter) {
        Field field = FIELDS[(iteration * 5 + 7) % FIELDS.length];
        Class<?> implementationClass = IMPLEMENTATIONS[(iteration * 7 + 1) % IMPLEMENTATIONS.length];
        Object carried = SHARED_RING[(iteration + 2) & (SHARED_RING.length - 1)];
        Object alternate = FIELDS[(iteration + 23) % FIELDS.length];
        int nestingLevel = 1 + ((iteration >>> 2) & 3);
        int local = intSink;

        for (int round = 0; round < 12800; round++) {
            ResolvableType type = hotWrapperThree(field, nestingLevel, implementationClass);
            FinalCpuLite box = finalCpuLiteStep(carried, type, alternate, local + round);

            if ((round & 1) == 0) {
                carried = box.left == null ? box : box.left;
            } else {
                carried = box.mirror == null ? box.right : box.mirror;
            }
            if ((round & 15) == 5) {
                alternate = finalCpuLiteStep(box, carried, alternate, local ^ round);
            }
            if ((round & 127) == 9) {
                consume(type, nestingLevel);
            }
            if (round > 9600 && iteration >= deoptAfter) {
                carried = Integer.valueOf(round);
            }
            local ^= box.marker;
            local += carried == null ? 5 : 11;
        }

        intSink = local;
        sink = carried;
    }

    private static void exerciseFinalCpuLiteCascadeOsr(int iteration, int deoptAfter) {
        Field field = FIELDS[(iteration * 9 + 3) % FIELDS.length];
        Class<?> implementationClass = IMPLEMENTATIONS[(iteration * 11 + 1) % IMPLEMENTATIONS.length];
        Object carried = SHARED_RING[(iteration + 5) & (SHARED_RING.length - 1)];
        Object alternate = FIELDS[(iteration + 29) % FIELDS.length];
        Object thirdSeed = IMPLEMENTATIONS[(iteration + 7) % IMPLEMENTATIONS.length];
        int nestingLevel = 1 + ((iteration >>> 1) & 3);
        int local = intSink;

        for (int round = 0; round < 13200; round++) {
            ResolvableType type = hotWrapperThree(field, 1 + ((nestingLevel + round) & 3),
                    implementationClass);
            FinalCpuLite first = finalCpuLiteStep(carried, type, alternate, local + round);
            FinalCpuLite second = finalCpuLiteStep(first, thirdSeed, carried, local ^ round);
            FinalCpuLite third = ((round & 3) == 0)
                    ? finalCpuLiteStep(second, alternate, first, local - round)
                    : finalCpuLiteStep(type, second, thirdSeed, local + (round << 1));

            if ((round & 7) == 0) {
                carried = third.mirror == null ? second : third.mirror;
            } else if ((round & 7) == 1) {
                carried = second.left == null ? first : second.left;
            } else if ((round & 7) == 2) {
                carried = first.right == null ? third : first.right;
            } else if ((round & 7) == 3) {
                Object fromCell = UNSAFE.getObjectVolatile(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET);
                carried = fromCell == null ? third.relay : fromCell;
            } else if ((round & 7) == 4) {
                carried = third.ghost == null ? second.relay : third.ghost;
            } else if ((round & 7) == 5) {
                carried = first == null ? thirdSeed : first;
            } else if ((round & 7) == 6) {
                carried = second.relay == null ? type : second.relay;
            } else {
                carried = third.shadow == null ? alternate : third.shadow;
            }

            if ((round & 15) == 6) {
                alternate = finalCpuLiteStep(third, carried, alternate, local ^ (round << 2));
            } else if ((round & 15) == 10) {
                alternate = third.relay == null ? first.ghost : third.relay;
            }
            if ((round & 31) == 11) {
                thirdSeed = Integer.valueOf(local + round);
            } else if ((round & 31) == 19) {
                thirdSeed = type;
            } else if ((round & 31) == 27) {
                thirdSeed = field;
            } else if ((round & 31) == 29) {
                thirdSeed = second.shadow == null ? third.ghost : second.shadow;
            }
            if ((round & 63) == 9) {
                UNSAFE.putOrderedObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET, third);
            } else if ((round & 63) == 41) {
                UNSAFE.putOrderedObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET,
                        third.relay == null ? second : third.relay);
            }
            if ((round & 127) == 21) {
                consume(type, nestingLevel);
            }
            if (round > 9800 && iteration >= deoptAfter) {
                carried = Integer.valueOf(round);
            }
            local ^= first.marker;
            local += second.marker;
            local ^= third.marker;
            local += carried == null ? 13 : 17;
        }

        intSink = local;
        sink = carried;
    }

    private static void exerciseFinalCpuLiteSpringCascadeOsr(int iteration, int deoptAfter) {
        Field field = FIELDS[(iteration * 9 + 1) % FIELDS.length];
        Field otherField = FIELDS[(iteration * 7 + 5) % FIELDS.length];
        Class<?> implementationClass = IMPLEMENTATIONS[(iteration * 11 + 3) % IMPLEMENTATIONS.length];
        Class<?> otherImplementation = IMPLEMENTATIONS[(iteration * 13 + 1) % IMPLEMENTATIONS.length];
        Object carried = SHARED_RING[(iteration + 9) & (SHARED_RING.length - 1)];
        Object alternate = FIELDS[(iteration + 31) % FIELDS.length];
        Object[] scratch = new Object[8];
        HashMap<Object, Object> localMap = new HashMap<Object, Object>();
        int nestingLevel = 1 + ((iteration >>> 1) & 3);
        int local = intSink;

        for (int round = 0; round < 11800; round++) {
            ResolvableType typeA = hotWrapperThree(field, 1 + ((nestingLevel + round) & 3),
                    implementationClass);
            ResolvableType typeB = hotWrapperThree(otherField, 1 + ((nestingLevel + round + 1) & 3),
                    otherImplementation);
            FinalCpuLite first = finalCpuLiteStep(carried, typeA, alternate, local + round);
            FinalCpuLite second = finalCpuLiteStep(typeB, first, carried, local ^ round);
            FinalCpuLite third = ((round & 1) == 0)
                    ? finalCpuLiteStep(typeA, second, typeB, local - round)
                    : finalCpuLiteStep(first, typeB, typeA, local + (round << 1));
            Object observedFirst = foldFinalCpuLiteObserved(first, carried, local + round);
            Object observedSecond = foldFinalCpuLiteObserved(second, observedFirst, local ^ round);
            Object observedThird = foldFinalCpuLiteObserved(third, observedSecond, local - round);
            scratch[round & 7] = observedThird;
            scratch[(round + 3) & 7] = third;
            localMap.put(first, observedSecond);
            localMap.put(second, observedThird);
            if ((round & 7) == 2) {
                publishedObject = scratch[(round + 5) & 7];
                volatileGuard = local ^ round;
            }

            if ((round & 7) == 0) {
                carried = third.mirror == null ? observedThird : third.mirror;
            } else if ((round & 7) == 1) {
                carried = second.left == null ? observedFirst : second.left;
            } else if ((round & 7) == 2) {
                carried = first.right == null ? observedSecond : first.right;
            } else if ((round & 7) == 3) {
                carried = third.shadow == null ? typeB : third.shadow;
            } else if ((round & 7) == 4) {
                carried = third.ghost == null ? second.relay : third.ghost;
            } else if ((round & 7) == 5) {
                carried = first.relay == null ? observedThird : first.relay;
            } else if ((round & 7) == 6) {
                carried = second.relay == null ? observedSecond : second.relay;
            } else {
                Object mapped = localMap.get(first);
                carried = third.shadow == null ? (mapped == null ? alternate : mapped) : third.shadow;
            }

            if ((round & 15) == 6) {
                alternate = finalCpuLiteStep(third, carried, typeA, local ^ (round << 2));
            } else if ((round & 15) == 10) {
                alternate = third.relay == null ? second.ghost : third.relay;
            } else if ((round & 15) == 14) {
                alternate = selectFinalCpuLiteSeed(third, observedThird, observedFirst, local + round);
            }
            if ((round & 31) == 11) {
                local = compactMemBarChain(new FinalPair(first, third, local + round), local, round);
            } else if ((round & 31) == 19) {
                Object seen = publishedObject;
                if (seen instanceof FinalCpuLite) {
                    FinalCpuLite seenBox = (FinalCpuLite) seen;
                    carried = seenBox.relay == null ? observedSecond : seenBox.relay;
                } else if (seen != null) {
                    carried = seen;
                }
            }
            if ((round & 63) == 9) {
                UNSAFE.putOrderedObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET, third);
            } else if ((round & 63) == 41) {
                UNSAFE.putOrderedObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET,
                        third.relay == null ? second : third.relay);
            } else if ((round & 63) == 57) {
                UNSAFE.putOrderedObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET,
                        observedThird == null ? carried : observedThird);
            }
            if ((round & 127) == 21) {
                consume(typeA, nestingLevel);
                consume(typeB, nestingLevel + 1);
            } else if ((round & 127) == 53) {
                consume(typeA, third.marker);
            } else if ((round & 127) == 85) {
                consume(typeB, second.marker);
            }
            if (round > 9000 && iteration >= deoptAfter) {
                carried = Integer.valueOf(round);
            }
            if ((round & 63) == 27) {
                local += localMap.size();
            }
            local ^= first.marker;
            local += second.marker;
            local ^= third.marker;
            local += carried == null ? 19 : 23;
        }

        intSink = local;
        sink = carried;
    }

    private static Object readFinalCpuLiteShadow(FinalCpuLite box, Object fallback) {
        Object volatileShadow = UNSAFE.getObjectVolatile(box, FINAL_CPU_LITE_SHADOW_OFFSET);
        if (volatileShadow != null) {
            return volatileShadow;
        }
        Object plainShadow = box.shadow;
        return plainShadow == null ? fallback : plainShadow;
    }

    private static Object readFinalCpuLiteGhost(FinalCpuLite box, Object fallback) {
        Object volatileGhost = UNSAFE.getObjectVolatile(box, FINAL_CPU_LITE_GHOST_OFFSET);
        if (volatileGhost != null) {
            return volatileGhost;
        }
        Object plainGhost = box.ghost;
        return plainGhost == null ? fallback : plainGhost;
    }

    private static Object readFinalCpuLiteRelay(FinalCpuLite box, Object fallback) {
        Object volatileRelay = UNSAFE.getObjectVolatile(box, FINAL_CPU_LITE_RELAY_OFFSET);
        if (volatileRelay != null) {
            return volatileRelay;
        }
        Object plainRelay = box.relay;
        return plainRelay == null ? fallback : plainRelay;
    }

    private static Object foldFinalCpuLiteObserved(FinalCpuLite box, Object fallback, int marker) {
        Object candidate = fallback;
        if ((marker & 3) == 0) {
            UNSAFE.storeFence();
            candidate = readFinalCpuLiteShadow(box, candidate);
        } else if ((marker & 3) == 1) {
            UNSAFE.loadFence();
            candidate = readFinalCpuLiteGhost(box, candidate);
        } else if ((marker & 3) == 2) {
            candidate = readFinalCpuLiteRelay(box, candidate);
        } else {
            Object cellSeen = UNSAFE.getObjectVolatile(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET);
            candidate = cellSeen == null ? box.mirror : cellSeen;
        }
        if ((marker & 15) == 6) {
            candidate = box.spare == null ? candidate : box.spare;
        } else if ((marker & 15) == 10) {
            candidate = box.shadow == null ? candidate : box.shadow;
        } else if ((marker & 15) == 12) {
            candidate = box.ghost == null ? candidate : box.ghost;
        }
        return candidate == null ? fallback : candidate;
    }

    private static Object selectFinalCpuLiteSeed(FinalCpuLite box, Object primary, Object alternate, int marker) {
        Object observed = foldFinalCpuLiteObserved(box, primary, marker);
        if ((marker & 7) == 1) {
            return observed == null ? alternate : observed;
        }
        if ((marker & 7) == 3) {
            return alternate == null ? observed : alternate;
        }
        if ((marker & 7) == 5) {
            return box.relay == null ? observed : box.relay;
        }
        return observed == null ? primary : observed;
    }

    private static FinalCpuLite finalCpuLiteStep(Object leftSeed, Object rightSeed,
            Object alternateSeed, int marker) {
        FinalCpuLite box = new FinalCpuLite(leftSeed, rightSeed, alternateSeed, marker);
        Object left = box.left;
        Object right = box.right;
        Object unsafeLeft = UNSAFE.getObjectVolatile(box, FINAL_CPU_LITE_LEFT_OFFSET);
        Object unsafeRight = UNSAFE.getObjectVolatile(box, FINAL_CPU_LITE_RIGHT_OFFSET);
        Object earlySpare = box.spare;
        Object earlyShadow = box.shadow;
        UNSAFE.putOrderedObject(box, FINAL_CPU_LITE_MIRROR_OFFSET, unsafeLeft == null ? right : unsafeLeft);
        Object mirror = UNSAFE.getObjectVolatile(box, FINAL_CPU_LITE_MIRROR_OFFSET);
        Object unsafeThird = UNSAFE.getObjectVolatile(box, FINAL_CPU_LITE_THIRD_OFFSET);
        if ((marker & 3) == 0) {
            UNSAFE.putOrderedObject(box, FINAL_CPU_LITE_MIRROR_OFFSET,
                    unsafeRight == null ? unsafeThird : unsafeRight);
            mirror = box.mirror;
        }
        Object selectedMirror = mirror;
        if ((marker & 31) == 3) {
            selectedMirror = left == null ? alternateSeed : left;
        } else if ((marker & 31) == 5) {
            selectedMirror = mirror == null ? right : mirror;
        } else if ((marker & 31) == 6) {
            selectedMirror = unsafeThird == null ? box : unsafeThird;
        } else if ((marker & 31) == 2 && earlySpare != null) {
            selectedMirror = earlySpare;
        } else if ((marker & 31) == 4 && earlyShadow != null) {
            selectedMirror = earlyShadow;
        }
        UNSAFE.putOrderedObject(box, FINAL_CPU_LITE_RELAY_OFFSET, selectedMirror);
        if ((marker & 15) == 2) {
            UNSAFE.putOrderedObject(FINAL_CPU_CELL, VOLATILE_CELL_VALUE_OFFSET, box);
        }
        Object trailingSpare = UNSAFE.getObjectVolatile(box, FINAL_CPU_LITE_SPARE_OFFSET);
        Object plainSpare = box.spare;
        Object shadowValue = readFinalCpuLiteShadow(box, mirror);
        Object ghostValue = readFinalCpuLiteGhost(box, shadowValue);
        Object relayValue = readFinalCpuLiteRelay(box, ghostValue);
        if ((marker & 31) == 1) {
            Object selected = trailingSpare == null ? plainSpare : trailingSpare;
            selectedMirror = selected == null ? relayValue : selected;
        } else if ((marker & 63) == 9) {
            Object selected = trailingSpare == null ? plainSpare : trailingSpare;
            selectedMirror = selected == null ? relayValue : selected;
        } else if ((marker & 63) == 13) {
            selectedMirror = shadowValue;
        } else if ((marker & 31) == 21) {
            selectedMirror = ghostValue;
        } else if ((marker & 63) == 25) {
            selectedMirror = relayValue;
        }
        Object observed = selectFinalCpuLiteSeed(box, relayValue, alternateSeed, marker);
        if ((marker & 63) == 27) {
            selectedMirror = observed;
        } else if ((marker & 63) == 29) {
            selectedMirror = observed == null ? shadowValue : observed;
        } else if ((marker & 63) == 45) {
            selectedMirror = observed == null ? ghostValue : observed;
        }
        if ((marker & 127) == 37) {
            box.mirror = selectedMirror;
        } else {
            UNSAFE.putOrderedObject(box, FINAL_CPU_LITE_GHOST_OFFSET, selectedMirror);
            if ((marker & 63) == 41) {
                box.shadow = relayValue;
            } else if ((marker & 63) == 43) {
                box.shadow = observed;
            }
        }
        if ((marker & 31) == 17) {
            UNSAFE.putOrderedObject(box, FINAL_CPU_LITE_RELAY_OFFSET, observed == null ? selectedMirror : observed);
        } else if ((marker & 31) == 19) {
            UNSAFE.putOrderedObject(box, FINAL_CPU_LITE_GHOST_OFFSET, observed == null ? relayValue : observed);
        }
        return box;
    }

    private static void exerciseLoopPhiSelfOsr(int iteration, int deoptAfter) {
        Field field = FIELDS[(iteration * 13 + 5) % FIELDS.length];
        Class<?> implementationClass = IMPLEMENTATIONS[(iteration >>> 1) % IMPLEMENTATIONS.length];
        int nestingLevel = 1 + (iteration & 3);
        FinalPair left = new FinalPair(SHARED_RING[iteration & (SHARED_RING.length - 1)],
                hotWrapperThree(field, nestingLevel, implementationClass), iteration);
        FinalPair right = new FinalPair(left,
                ResolvableType.forField(field, nestingLevel, implementationClass), iteration + 1);
        BarrierHolder<Object> holder = SHARED_RING[iteration & (SHARED_RING.length - 1)];
        Object[] scratch = new Object[16];
        HashMap<Object, Object> localMap = new HashMap<Object, Object>();
        Object carried = left;
        Object previous = right;
        int local = intSink;

        for (int round = 0; round < 9200; round++) {
            ResolvableType type = hotWrapperThree(field, 1 + ((round ^ iteration) & 3),
                    IMPLEMENTATIONS[(round + iteration) % IMPLEMENTATIONS.length]);
            FinalPair fresh = new FinalPair(carried, type, local + round);

            synchronized (LOCK_ONE) {
                publishedObject = fresh;
                scratch[round & 15] = fresh;
                scratch[(round + 3) & 15] = type;
                holder.sideEffect = scratch[(round + 1) & 15];
                localMap.put(field, fresh);
                localMap.put(type, holder);
                volatileGuard = local + round;
                if ((round & 3) == 0) {
                    previous = carried;
                } else if ((round & 3) == 1) {
                    carried = fresh;
                }
            }

            synchronized (LOCK_TWO) {
                Object seen = publishedObject;
                if (seen instanceof FinalPair) {
                    FinalPair seenPair = (FinalPair) seen;
                    local += seenPair.marker.intValue();
                    scratch[(round + 7) & 15] = seenPair.left;
                    holder.sideEffect = seenPair.right;
                    carried = ((round & 1) == 0) ? seenPair : previous;
                }
                if ((round & 7) == 2) {
                    publishedObject = previous;
                }
                if ((round & 15) == 6) {
                    localMap.remove(field);
                }
                if ((round & 31) == 10) {
                    SHARED_CACHE.put(fresh, type);
                    SHARED_CACHE.put(field, holder);
                }
                volatileGuard = local ^ round;
            }

            try {
                FinalPair pair = (FinalPair) carried;
                local ^= pair.marker.intValue();
                Object next = ((round & 1) == 0) ? pair.left : pair.right;
                if (next instanceof ResolvableType) {
                    consume((ResolvableType) next, nestingLevel);
                } else if (next instanceof FinalPair) {
                    FinalPair nextPair = (FinalPair) next;
                    local += nextPair.marker.intValue();
                    carried = nextPair;
                }
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
                carried = fresh;
            }

            if ((round & 15) == 5) {
                local = compactMemBarChain(fresh, local, round);
            }
            if ((round & 31) == 17) {
                ResolvableType.clearCache();
            }
            if ((round & 63) == 23) {
                local += localMap.size();
                Object fromScratch = scratch[(round >>> 1) & 15];
                if (fromScratch instanceof FinalPair) {
                    carried = fromScratch;
                }
            }
            if (iteration >= deoptAfter && round > 6500 && (round & 63) == 7) {
                carried = Integer.valueOf(round);
            }
            previous = fresh;
            local += fresh.marker.intValue();
        }

        intSink = local;
        sink = carried;
    }

    private static int compactMemBarChain(FinalPair pair, int local, int round) {
        LockBox box = new LockBox(pair, local + round);
        synchronized (LOCK_ONE) {
            box.side = publishedObject;
            publishedObject = box;
            volatileGuard = local;
        }
        synchronized (LOCK_TWO) {
            Object seen = publishedObject;
            if (seen instanceof LockBox) {
                LockBox seenBox = (LockBox) seen;
                seenBox.side = pair;
                local += seenBox.marker.intValue();
            }
            volatileGuard = local ^ round;
        }
        UNSAFE.storeFence();
        local += box.marker.intValue();
        Object storedPair = box.pair;
        if (storedPair instanceof FinalPair) {
            FinalPair seenPair = (FinalPair) storedPair;
            local ^= seenPair.marker.intValue();
            sink = seenPair.left;
        }
        return local;
    }

    private static int exerciseZipCloseChain(int local, int round) {
        try {
            ZipFile zipFile = new ZipFile(ZIP_STORM_FILE);
            try {
                InputStream input = zipFile.getInputStream(zipFile.getEntry("entry.txt"));
                InputStream second = zipFile.getInputStream(zipFile.getEntry("entry.txt"));
                if (input != null) {
                    try {
                        local += input.read();
                        if (second != null && (round & 7) == 3) {
                            local += second.read();
                            second.close();
                        }
                    } finally {
                        try {
                            input.close();
                            if ((round & 3) == 1) {
                                input.close();
                            }
                        } finally {
                            if (second != null) {
                                second.close();
                                if ((round & 7) == 5) {
                                    second.close();
                                }
                            }
                        }
                    }
                }
            } finally {
                zipFile.close();
                if ((round & 7) == 5) {
                    zipFile.close();
                }
            }
        } catch (IOException e) {
            local ^= e.getClass().getName().hashCode();
        }
        return local ^ round;
    }

    private static int exercisePseudoZipChain(int local, int round, FinalPair pair) {
        PseudoZip.PseudoInput first = PSEUDO_ZIP.getInput(pair);
        PseudoZip.PseudoInput second = PSEUDO_ZIP.getInput(pair.right);
        try {
            local += first.read();
            if ((round & 7) == 0) {
                local += second.read();
                second.close();
            }
        } finally {
            try {
                first.close();
                if ((round & 3) == 0) {
                    first.close();
                }
            } finally {
                second.close();
                if ((round & 3) == 2) {
                    PSEUDO_ZIP.crossClose(first, second, round);
                }
                if ((round & 15) == 8) {
                    PSEUDO_ZIP.clearSome(round);
                }
            }
        }
        return local ^ first.marker.intValue();
    }

    private static int exerciseVectorChain(Vector<Object> vector, FinalPair pair,
            ResolvableType type, int local, int round) {
        synchronized (vector) {
            vector.add(pair);
            vector.add(type);
            if (vector.size() > 18) {
                vector.removeElementAt((round >>> 1) % vector.size());
            }
            if ((round & 7) == 3 && vector.size() > 2) {
                vector.removeElementAt(1);
            }
            if ((round & 15) == 11) {
                vector.trimToSize();
            }
            if ((round & 31) == 21) {
                vector.removeAllElements();
                vector.add(pair.left);
                vector.add(type);
            }
            local += vector.size();
        }
        if ((round & 7) == 5) {
            try {
                vector.removeElementAt(vector.size());
            } catch (ArrayIndexOutOfBoundsException expected) {
                local ^= expected.getClass().getName().length();
            }
        }
        Object first;
        synchronized (vector) {
            first = vector.isEmpty() ? pair : vector.firstElement();
            if ((round & 3) == 0) {
                vector.setElementAt(pair.right, 0);
            }
        }
        if (first instanceof ResolvableType) {
            consume((ResolvableType) first, 1 + (round & 3));
        }
        return local ^ System.identityHashCode(first);
    }

    private static void exerciseShortOsrVariantA(int iteration, int deoptAfter) {
        BarrierHolder<?> holder = new BarrierHolder<Object>("short-a", iteration);
        Object candidate = holder;
        Field field = FIELDS[(iteration + 7) % FIELDS.length];
        int local = intSink;
        for (int round = 0; round < 700; round++) {
            synchronized (LOCK_ONE) {
                holder.sideEffect = Integer.valueOf(local + round);
                publishedObject = holder;
            }
            if ((round & 15) == 0) {
                consume(hotWrapperThree(field, 1 + (round & 3), BarrierStringRepository.class), 1);
            }
            if (iteration >= deoptAfter && round > 500) {
                candidate = "short-a-flip";
            }
            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                local += seen.boxedInt.intValue();
                local ^= (int) (seen.boxedLong.longValue() & 255L);
            } catch (ClassCastException expectedAfterWarmup) {
                candidate = holder;
                local ^= expectedAfterWarmup.getClass().getName().length();
            }
        }
        intSink = local;
        sink = candidate;
    }

    private static void exerciseShortOsrVariantB(int iteration, int deoptAfter) {
        BarrierHolder<?> holder = new BarrierHolder<Object>("short-b", iteration);
        MethodParameter parameter = METHOD_PARAMETERS[iteration % METHOD_PARAMETERS.length]
                .withContainingClass(DeepStringRepository.class);
        Object candidate = holder;
        int local = intSink;
        for (int round = 0; round < 720; round++) {
            synchronized (LOCK_TWO) {
                publishedObject = holder;
                volatileGuard = round;
            }
            if ((round & 31) == 3) {
                consume(ResolvableType.forMethodParameter(parameter), 1 + (round & 3));
            }
            synchronized (LOCK_ONE) {
                candidate = iteration >= deoptAfter && round > 520
                        ? Integer.valueOf(round)
                        : publishedObject;
            }
            try {
                BarrierHolder<?> seen = (BarrierHolder<?>) candidate;
                local += seen.boxedInt.intValue();
                sink = seen.values[round & 1];
            } catch (ClassCastException expectedAfterWarmup) {
                candidate = holder;
                local ^= expectedAfterWarmup.getClass().getName().hashCode();
            }
        }
        intSink = local;
        sink = candidate;
    }

    private static void exerciseShortOsrVariantC(int iteration, int deoptAfter) {
        BarrierHolder<?> first = new BarrierHolder<Object>("short-c0", iteration);
        BarrierHolder<?> second = new BarrierHolder<Object>("short-c1", iteration + 1);
        Field field = FIELDS[(iteration + 17) % FIELDS.length];
        int local = intSink;
        for (int round = 0; round < 740; round++) {
            BarrierHolder<?> selected = (round & 1) == 0 ? first : second;
            synchronized (LOCK_ONE) {
                selected.sideEffect = Integer.valueOf(round);
                synchronized (LOCK_TWO) {
                    publishedObject = selected;
                    volatileGuard = local;
                }
            }
            local += selected.boxedInt.intValue();
            local ^= (int) (selected.boxedLong.longValue() & 1023L);
            if ((round & 63) == 11) {
                consume(hotWrapperThree(field, 1 + ((round >>> 1) & 3), BarrierIntegerRepository.class), 2);
            }
            if (iteration >= deoptAfter && round == 650) {
                triggerUncommonTrap(iteration + round, deoptAfter);
            }
        }
        intSink = local;
        sink = publishedObject;
    }

    private static void exerciseShortOsrVariantD(int iteration, int deoptAfter) {
        BarrierHolder<?> holder = new BarrierHolder<Object>("short-d", iteration);
        MethodParameter parameter = METHOD_PARAMETERS[(iteration + 1) % METHOD_PARAMETERS.length]
                .withContainingClass(DeepIntegerRepository.class);
        int local = intSink;
        for (int round = 0; round < 760; round++) {
            synchronized (LOCK_TWO) {
                holder.sideEffect = parameter;
                publishedObject = holder;
            }
            if ((round & 7) == 5) {
                local = exerciseConvergedRelease(iteration, round, holder, local);
            }
            if ((round & 63) == 19) {
                consume(ResolvableType.forMethodParameter(parameter), 2);
            }
            local += holder.boxedInt.intValue();
        }
        intSink = local;
        sink = holder.value;
    }

    private static int exerciseConvergedRelease(int iteration, int round,
            BarrierHolder<?> holder, int local) {
        BarrierHolder<?> selected;
        synchronized (LOCK_ONE) {
            selected = holder;
            selected.sideEffect = Integer.valueOf(iteration + round);
            publishedObject = selected;
        }

        if (((iteration + round) & 1) == 0) {
            local += round;
        } else {
            local ^= iteration;
        }

        local += selected.boxedInt.intValue();
        local ^= (int) (selected.boxedLong.longValue() & 8191L);
        sink = selected.value;
        return local;
    }

    private static void exerciseMethodParameterOsrMemBarLoop(int iteration, int deoptAfter) {
        BarrierHolder<?> left = new BarrierHolder<Object>("method-left", iteration);
        BarrierHolder<?> right = new BarrierHolder<Object>("method-right", iteration + 3);
        MethodParameter baseParameter = METHOD_PARAMETERS[iteration % METHOD_PARAMETERS.length];
        Class<?> implementationClass = (iteration & 1) == 0
                ? DeepStringRepository.class
                : DeepIntegerRepository.class;
        Object candidate = left;
        int local = intSink;

        for (int round = 0; round < 1500; round++) {
            MethodParameter parameter = baseParameter.withContainingClass(implementationClass);
            BarrierHolder<?> selected;
            synchronized (LOCK_ONE) {
                selected = ((round + iteration) & 2) == 0 ? left : right;
                selected.sideEffect = parameter;
                publishedObject = selected;
                volatileGuard = round + local;
            }

            try {
                synchronized (LOCK_TWO) {
                    Object seenObject = osrFlip ? candidate : publishedObject;
                    BarrierHolder<?> seen = (BarrierHolder<?>) seenObject;
                    local += seen.boxedInt.intValue();
                    local ^= (int) (seen.boxedLong.longValue() & 4095L);
                    if ((round & 31) == 7) {
                        consume(ResolvableType.forMethodParameter(parameter), 1 + (round & 3));
                    }
                    if (round > 1000 && iteration >= deoptAfter) {
                        osrFlip = true;
                        candidate = Integer.valueOf(round);
                    } else {
                        candidate = seen;
                    }
                }

                if (candidate instanceof BarrierHolder) {
                    BarrierHolder<?> afterUnlock = (BarrierHolder<?>) candidate;
                    local += afterUnlock.boxedInt.intValue();
                    sink = afterUnlock.values[round & 1];
                }
            } catch (ClassCastException expectedAfterWarmup) {
                local ^= expectedAfterWarmup.getClass().getName().length();
                osrFlip = false;
                candidate = new BarrierHolder<Object>("method-recovered", iteration ^ round);
            }
        }

        intSink = local;
        sink = candidate;
    }

    private static int exerciseNestedReleaseChain(int iteration, int round,
            BarrierHolder<?> holder, int local) {
        BarrierHolder<?> nested = holder;
        synchronized (LOCK_ONE) {
            nested.sideEffect = Integer.valueOf(local);
            synchronized (LOCK_TWO) {
                publishedObject = nested;
                volatileGuard = local + round;
                if (((iteration ^ round) & 31) == 11) {
                    nested = new BarrierHolder<Object>(nested.value, iteration + round);
                    publishedObject = nested;
                }
            }
            if ((volatileGuard & 1) == 0) {
                sink = publishedObject;
            }
        }

        local += nested.boxedInt.intValue();
        local ^= (int) (nested.boxedLong.longValue() & 2047L);
        sink = nested.values[(round >>> 1) & 1];
        return local;
    }

    private static void consume(ResolvableType type, int nestingLevel) {
        ResolvableType generic0 = type.getGeneric(0);
        ResolvableType nested = type.getNested(nestingLevel);

        sink = nested;
        intSink ^= type.hashCode();
        intSink += generic0.resolve(Object.class).getName().length();
    }

    private static void exerciseMethodParameter(int iteration) {
        MethodParameter parameter = METHOD_PARAMETERS[iteration % METHOD_PARAMETERS.length];
        parameter = parameter.withContainingClass(IMPLEMENTATIONS[(iteration >>> 3) % IMPLEMENTATIONS.length]);

        ResolvableType type = ResolvableType.forMethodParameter(parameter);
        consume(type, 1 + ((iteration >>> 5) & 3));
    }

    public static class GenericRepository<T, ID> {
        public T value;
        public List<T> values;
        public Map<ID, List<T>> map;
        public List<List<List<T>>> nested;
        public List<? extends T> wildcard;
        public T[] array;
        public Optional<T> optional;
        public Map.Entry<ID, List<T>> entry;
        public Queue<? super T> queue;

        public void consume(T value, List<T> values, Map<ID, List<T>> mappedValues) {
        }
    }

    public static class DeepRepository<T, ID> extends GenericRepository<List<T>, ID> {
        public T deepValue;
        public List<List<T>> deepValues;
        public Map<ID, Map<String, List<T>>> deepMap;
        public List<T>[] deepArray;
        public Optional<Map<ID, List<T>>> deepOptional;

        public void consumeDeep(T value, List<List<T>> values, Map<ID, Map<String, List<T>>> mappedValues) {
        }
    }

    public static class RecursiveRepository<N extends Number, T extends RecursiveNode<T>>
            extends GenericRepository<T, N> {
        public T recursiveValue;
        public List<? extends T> recursiveValues;
        public Map<N, Map<T, List<? super T>>> recursiveMap;
    }

    public static class HolderRepository extends GenericRepository<Holder, String> {
        public List<Holder> holders;
        public Map<String, List<Holder>> holderMap;
        public Holder[] holderArray;
    }

    public static class MutualA<A, B> {
        public B peer;
        public List<Map<A, B>> peers;
        public Map<A, List<MutualB<B, A>>> indexedPeers;
    }

    public static class MutualB<B, A> {
        public A owner;
        public List<Map<B, A>> owners;
        public Map<B, List<MutualA<A, B>>> reverseIndex;
    }

    public static class MutualRepository<A, B>
            extends GenericRepository<MutualA<A, B>, A> {
        public MutualA<A, B> root;
        public List<MutualA<A, B>> roots;
        public Map<A, Map<B, List<MutualA<A, B>>>> graph;
    }

    public static class BarrierRepository<T, ID>
            extends GenericRepository<BarrierHolder<T>, ID> {
        public BarrierHolder<T> holder;
        public List<BarrierHolder<T>> holders;
        public Map<ID, Map<String, List<BarrierHolder<T>>>> barrierMap;
        public Integer boxedValue;
        public List<Integer> boxedValues;
    }

    public static final class StringRepository extends GenericRepository<String, Long> {
    }

    public static final class IntegerRepository extends GenericRepository<Integer, AtomicInteger> {
    }

    public static final class DeepStringRepository extends DeepRepository<String, Long> {
    }

    public static final class DeepIntegerRepository extends DeepRepository<Integer, AtomicInteger> {
    }

    public static final class RecursiveStringRepository
            extends RecursiveRepository<Long, StringNode> {
    }

    public static final class RecursiveIntegerRepository
            extends RecursiveRepository<Integer, IntegerNode> {
    }

    public static final class Holder extends HashMap<String, ArrayList<String>> {
        private static final long serialVersionUID = 1L;
    }

    public static final class MutualString
            extends MutualB<MutualString, MutualA<String, MutualString>> {
    }

    public static final class MutualInteger
            extends MutualB<MutualInteger, MutualA<Integer, MutualInteger>> {
    }

    public static final class MutualStringRepository
            extends MutualRepository<String, MutualString> {
    }

    public static final class MutualIntegerRepository
            extends MutualRepository<Integer, MutualInteger> {
    }

    public static final class BarrierStringRepository
            extends BarrierRepository<String, Long> {
    }

    public static final class BarrierIntegerRepository
            extends BarrierRepository<Integer, AtomicInteger> {
    }

    public static final class BarrierHolder<T> {
        public final T value;
        public final Integer boxedInt;
        public final Long boxedLong;
        public final Object[] values;
        public BarrierHolder<T> next;
        public volatile Object sideEffect;

        BarrierHolder(T value, int iteration) {
            this.value = value;
            this.boxedInt = Integer.valueOf(iteration & 127);
            this.boxedLong = Long.valueOf(iteration);
            this.values = new Object[] { value, boxedInt };
            this.next = iteration == Integer.MIN_VALUE ? this : null;
        }
    }

    public static final class FinalPair {
        public final Object left;
        public final Object right;
        public final Integer marker;

        FinalPair(Object left, Object right, int marker) {
            this.left = left;
            this.right = right;
            this.marker = Integer.valueOf(marker & 32767);
        }
    }

    public static final class FinalCpuBox {
        public final Object left;
        public final Object right;
        public final Integer marker;
        public volatile Object mirror;

        FinalCpuBox(Object left, Object right, int marker) {
            this.left = left;
            this.right = right;
            this.marker = Integer.valueOf(marker & 65535);
            this.mirror = right;
        }
    }

    public static class FinalCpuLiteBase {
        public Object spare;
        public Object shadow;
        public Object ghost;
        public Object relay;
    }

    public static final class FinalCpuLite extends FinalCpuLiteBase {
        public final Object left;
        public final Object right;
        public final Object third;
        public final int marker;
        public Object mirror;

        FinalCpuLite(Object left, Object right, Object third, int marker) {
            this.left = left;
            this.right = right;
            this.third = third;
            this.marker = marker;
            this.mirror = third;
        }
    }

    public static final class VolatileCell {
        public volatile Object value;
    }

    public static final class WeakStormKey {
        private final int id;
        private final int hash;

        WeakStormKey(int id, int salt) {
            this.id = id;
            this.hash = (id * 65537) ^ salt;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof WeakStormKey && ((WeakStormKey) other).id == id;
        }
    }

    public static final class LockBox {
        public final Object pair;
        public final Integer marker;
        public volatile Object side;

        LockBox(Object pair, int marker) {
            this.pair = pair;
            this.marker = Integer.valueOf(marker & 65535);
        }
    }

    public static final class PseudoZip {
        private final Map<PseudoInput, Object> streams = new WeakHashMap<PseudoInput, Object>();
        private final Deque<Object> cache = new ArrayDeque<Object>();
        private final Object[] slots = new Object[16];
        private final Object gate = new Object();
        private boolean closed;
        private int mutations;
        private Object last;

        PseudoInput getInput(Object entry) {
            synchronized (this) {
                ensureOpen();
                PseudoInput input = new PseudoInput(this, entry, cache.pollFirst());
                synchronized (streams) {
                    streams.put(input, entry);
                    slots[mutations & 15] = input;
                }
                mutations++;
                last = input;
                return input;
            }
        }

        void release(PseudoInput input) {
            Object value;
            synchronized (streams) {
                value = streams.remove(input);
                slots[input.marker.intValue() & 15] = value;
                if ((input.marker.intValue() & 7) == 3) {
                    streams.put(input, input.entry);
                    value = streams.remove(input);
                }
                mutations++;
            }
            synchronized (this) {
                if (value != null) {
                    cache.addLast(value);
                }
                slots[(mutations + 5) & 15] = input.cached == null ? input.entry : input.cached;
                if ((mutations & 3) == 1) {
                    last = slots[(mutations + 1) & 15];
                } else {
                    last = value;
                }
                mutations++;
                if ((input.marker.intValue() & 15) == 9) {
                    cache.addLast(input);
                    slots[(mutations + 7) & 15] = cache.peekFirst();
                }
            }
        }

        void pureClose(PseudoInput input) {
            Object value;
            synchronized (this) {
                value = last;
                slots[mutations & 15] = input;
                synchronized (gate) {
                    slots[(mutations + 1) & 15] = value == null ? input.entry : value;
                    last = input.entry;
                    mutations++;
                }
            }
            synchronized (gate) {
                value = slots[(mutations + input.marker.intValue()) & 15];
                slots[(mutations + 2) & 15] = input.cached == null ? input : input.cached;
                synchronized (this) {
                    slots[(mutations + 3) & 15] = value == null ? input.entry : value;
                    last = value == null ? input : value;
                    mutations++;
                }
            }
        }

        int loopPhiClose(PseudoInput first, PseudoInput second, Object payload, int seed, int deoptAfter) {
            PseudoInput current = first;
            PseudoInput alternate = second;
            Object value = payload;
            int local = seed;

            for (int round = 0; round < 9200; round++) {
                synchronized (this) {
                    value = last == null ? value : last;
                    slots[(mutations + round) & 15] = current;
                    synchronized (gate) {
                        slots[(mutations + 1) & 15] = value == null ? alternate.entry : value;
                        last = current.entry;
                        mutations++;
                    }
                }
                synchronized (gate) {
                    value = slots[(mutations + current.marker.intValue()) & 15];
                    slots[(mutations + 2) & 15] = current.cached == null ? alternate : current.cached;
                    synchronized (this) {
                        Object seen = value == null ? current.entry : value;
                        slots[(mutations + 3) & 15] = seen;
                        last = seen;
                        mutations++;
                    }
                }

                if ((round & 1) == 0) {
                    PseudoInput swap = current;
                    current = alternate;
                    alternate = swap;
                } else {
                    try {
                        synchronized (this) {
                            slots[(mutations + 5) & 15] = alternate.entry;
                            last = current;
                        }
                    } finally {
                        synchronized (gate) {
                            slots[(mutations + 7) & 15] = current.cached == null ? current : current.cached;
                            local ^= current.marker.intValue();
                        }
                    }
                }

                if ((round & 15) == 7) {
                    value = new FinalPair(current, value, local + round);
                }
                synchronized (this) {
                    last = current;
                    slots[(mutations + 11) & 15] = current;
                    synchronized (gate) {
                        value = slots[(mutations + 11) & 15];
                        slots[(mutations + 13) & 15] = value == null ? alternate : value;
                        mutations++;
                    }
                }
                synchronized (gate) {
                    slots[(mutations + 11) & 15] = last;
                    synchronized (this) {
                        Object carried = slots[(mutations + 13) & 15];
                        last = carried == null ? current : carried;
                        mutations++;
                    }
                }
                if (round > 7000 && seed >= deoptAfter) {
                    value = Integer.valueOf(round);
                }
                local += current.marker.intValue() ^ alternate.marker.intValue();
            }

            last = value;
            return local;
        }

        int lastFieldLoop(PseudoInput first, PseudoInput second, Object payload, int seed, int deoptAfter) {
            PseudoInput current = first;
            PseudoInput alternate = second;
            Object value = payload;
            int local = seed;

            for (int round = 0; round < 9800; round++) {
                synchronized (this) {
                    Object seen = last;
                    last = seen == null ? current : seen;
                    mutations++;
                }

                synchronized (this) {
                    value = last;
                    last = current.entry == null ? alternate : current.entry;
                    mutations++;
                }

                if ((round & 3) == 1) {
                    synchronized (this) {
                        Object seen = last;
                        last = seen == null ? value : seen;
                        local ^= mutations;
                    }
                } else if ((round & 3) == 2) {
                    try {
                        synchronized (this) {
                            value = last;
                            last = alternate.cached == null ? current : alternate.cached;
                            local += current.marker.intValue();
                        }
                    } finally {
                        synchronized (this) {
                            Object carried = last;
                            last = carried == null ? alternate : carried;
                            mutations++;
                        }
                    }
                } else {
                    synchronized (this) {
                        Object carried = last;
                        last = carried == null ? payload : carried;
                        local += alternate.marker.intValue();
                    }
                }

                if ((round & 7) == 3) {
                    PseudoInput swap = current;
                    current = alternate;
                    alternate = swap;
                }
                if ((round & 31) == 9) {
                    value = new FinalPair(current, value, local + round);
                    synchronized (this) {
                        last = value;
                        mutations++;
                    }
                }
                if (round > 7600 && seed >= deoptAfter) {
                    value = Integer.valueOf(round);
                }
                local ^= current.marker.intValue() + round;
            }

            synchronized (this) {
                last = value;
                mutations++;
            }
            return local;
        }

        void afterClose(PseudoInput input) {
            Object value;
            synchronized (this) {
                value = last;
                slots[(input.marker.intValue() + mutations) & 15] = input.entry;
                last = input.cached == null ? input : input.cached;
                mutations++;
                synchronized (streams) {
                    streams.put(input, value == null ? input.entry : value);
                    slots[(mutations + 3) & 15] = streams.remove(input);
                }
            }
            synchronized (streams) {
                streams.put(input, value == null ? input : value);
                synchronized (this) {
                    Object removed = streams.remove(input);
                    slots[(mutations + 9) & 15] = removed == null ? input.cached : removed;
                    last = removed;
                    mutations++;
                }
            }
        }

        void afterRepeatedClose(PseudoInput input) {
            synchronized (streams) {
                slots[(input.marker.intValue() + 11) & 15] = streams.get(input);
                synchronized (this) {
                    last = slots[(mutations + 11) & 15];
                    mutations++;
                }
            }
        }

        void crossClose(PseudoInput first, PseudoInput second, int round) {
            try {
                synchronized (this) {
                    slots[round & 15] = first.entry;
                    try {
                        first.close();
                    } finally {
                        synchronized (streams) {
                            streams.put(second, first.cached == null ? first.entry : first.cached);
                            slots[(round + 1) & 15] = second;
                        }
                    }
                }
            } finally {
                synchronized (streams) {
                    streams.put(first, second.entry);
                    try {
                        second.close();
                    } finally {
                        synchronized (this) {
                            Object removed = streams.remove(first);
                            last = removed == null ? second : removed;
                            slots[(round + 2) & 15] = last;
                            mutations++;
                        }
                    }
                }
            }
        }

        void clearSome(int round) {
            synchronized (this) {
                if ((round & 31) == 8) {
                    cache.clear();
                } else if (!cache.isEmpty()) {
                    cache.removeFirst();
                }
                closed = false;
            }
        }

        private void ensureOpen() {
            if (closed) {
                throw new IllegalStateException("closed");
            }
        }

        public static final class PseudoInput {
            private final PseudoZip owner;
            private final Object entry;
            private final Object cached;
            private final Integer marker;
            private boolean closed;

            PseudoInput(PseudoZip owner, Object entry, Object cached) {
                this.owner = owner;
                this.entry = entry;
                this.cached = cached;
                this.marker = Integer.valueOf(System.identityHashCode(entry) & 65535);
            }

            int read() {
                if (closed) {
                    return -1;
                }
                int value = marker.intValue();
                if (entry != null) {
                    value ^= entry.hashCode();
                }
                if (cached != null) {
                    value += cached.hashCode();
                }
                return value & 255;
            }

            void close() {
                if (closed) {
                    owner.afterRepeatedClose(this);
                    return;
                }
                try {
                    closed = true;
                    owner.release(this);
                } finally {
                    owner.pureClose(this);
                    if ((marker.intValue() & 3) == 2) {
                        owner.afterClose(this);
                    }
                }
            }
        }
    }

    public static final class HotReferenceMap extends ConcurrentReferenceHashMap<Object, Object> {
        HotReferenceMap() {
            super(2, 0.75f, 1, ConcurrentReferenceHashMap.ReferenceType.WEAK);
        }

        void segmentLock() {
            getSegment(0).lock();
        }

        void segmentUnlock() {
            getSegment(0).unlock();
        }

        int segmentSize() {
            return getSegment(0).getSize();
        }

        int segmentCount() {
            return getSegment(0).getCount();
        }
    }

    public static final class CollisionKey {
        private final int id;

        CollisionKey(int id) {
            this.id = id;
        }

        public int hashCode() {
            return 17;
        }

        public boolean equals(Object other) {
            return this == other || (other instanceof CollisionKey && this.id == ((CollisionKey) other).id);
        }
    }

    public static final class SpreadKey {
        private final int id;

        SpreadKey(int id) {
            this.id = id;
        }

        public int hashCode() {
            return id * 0x9e3779b9;
        }

        public boolean equals(Object other) {
            return this == other || (other instanceof SpreadKey && this.id == ((SpreadKey) other).id);
        }
    }

    public static class ComplexPair<A, B> extends LinkedHashMap<A, List<B>> {
        private static final long serialVersionUID = 1L;
    }

    public interface RecursiveNode<T extends RecursiveNode<T>> {
        Set<T> children();
    }

    public static final class StringNode implements RecursiveNode<StringNode> {
        public Set<StringNode> children() {
            return new HashSet<StringNode>();
        }
    }

    public static final class IntegerNode implements RecursiveNode<IntegerNode> {
        public Set<IntegerNode> children() {
            return new HashSet<IntegerNode>();
        }
    }

    private interface TypeDriver {
        Class<?> implementation(int iteration);
    }

    private static final class StableStringDriver implements TypeDriver {
        public Class<?> implementation(int iteration) {
            return StringRepository.class;
        }
    }

    private static final class StableIntegerDriver implements TypeDriver {
        public Class<?> implementation(int iteration) {
            return IntegerRepository.class;
        }
    }

    private static final class RareMutualDriver implements TypeDriver {
        public Class<?> implementation(int iteration) {
            return (iteration & 2) == 0 ? MutualStringRepository.class : MutualIntegerRepository.class;
        }
    }
}
