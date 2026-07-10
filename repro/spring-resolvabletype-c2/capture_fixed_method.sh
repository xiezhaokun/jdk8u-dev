#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)
JAVA_BIN=${REPRO_JAVA:-/Users/xiezhaokun/workspace/jdk8u-dev/build/codex-fastdebug-x86_64/jdk/bin/java}
JAVA_HOME=$(cd "$(dirname "$JAVA_BIN")/.." && pwd)
JCMD_BIN=${REPRO_JCMD:-"$JAVA_HOME/bin/jcmd"}
JSTACK_BIN=${REPRO_JSTACK:-"$JAVA_HOME/bin/jstack"}
TARGET_NAME=${REPRO_TARGET:-pureZipSpringWideOsr}
TARGET_METHOD=${TARGET_METHOD:-org/springframework/core/ResolvableType forField}
MATCH_ANY_FIXED=${MATCH_ANY_FIXED:-0}
ITERATIONS=${REPRO_ITERATIONS:-1}
DEOPT_AFTER=${REPRO_DEOPT_AFTER:-2}
MAX_SAMPLES=${MAX_SAMPLES:-200200000}
SAMPLE_INTERVAL=${SAMPLE_INTERVAL:-0.2}
SAME_THRESHOLD=${SAME_THRESHOLD:-8}
C2_CSV_METHOD_VALUE=${C2_CSV_METHOD_VALUE:-ResolvableType}
C2_CSV_LOG_LIMIT_VALUE=${C2_CSV_LOG_LIMIT_VALUE:-300000}
EXTRA_JVM_ARGS_VALUE=${EXTRA_JVM_ARGS:-}
STAMP=$(date +%Y%m%d-%H%M%S)
CAPDIR=${CAPDIR:-"$ROOT_DIR/target/captures"}
BASE="$CAPDIR/${TARGET_NAME}-capture-$STAMP"
CP=target/classes:target/test-classes:target/dependency/spring-core-5.3.31.jar:target/dependency/spring-jcl-5.3.31.jar

mkdir -p "$CAPDIR"
cd "$ROOT_DIR"

extra_jvm_args=()
if [[ -n "$EXTRA_JVM_ARGS_VALUE" ]]; then
  read -r -a extra_jvm_args <<<"$EXTRA_JVM_ARGS_VALUE"
fi

java_args=(
  -server
  -Xbatch
  -XX:CompileThreshold=12000
  -XX:BackEdgeThreshold=12000
  -XX:-TieredCompilation
  -XX:CICompilerCount=1
  -XX:+UnlockDiagnosticVMOptions
  -XX:+StressGCM
  -XX:+StressLCM
  -XX:+StressReflectiveCode
  -XX:+AlwaysIncrementalInline
  -XX:MaxInlineLevel=15
  -XX:MaxInlineSize=500
  -XX:FreqInlineSize=1000
  -XX:InlineSmallCode=10000
  -XX:MaxNodeLimit=300000
  -XX:CompileCommand=quiet
  -XX:CompileCommand=inline,repro/ResolvableTypeC2Repro.hotWrapperThree
  -XX:CompileCommand=inline,repro/ResolvableTypeC2Repro.finalCpuLiteStep
  -XX:CompileCommand=inline,repro/ResolvableTypeC2Repro.consume
  -XX:CompileCommand=inline,repro/ResolvableTypeC2Repro\$PseudoZip.loopPhiResolvableClose
)

if [[ ${#extra_jvm_args[@]} -gt 0 ]]; then
  java_args+=("${extra_jvm_args[@]}")
fi

java_args+=(
  -Drepro.iterations="$ITERATIONS"
  -Drepro.deoptAfter="$DEOPT_AFTER"
  -Drepro.target="$TARGET_NAME"
  -cp "$CP"
  repro.ResolvableTypeC2Repro
)

env \
  C2_CSV_LOG=1 \
  C2_CSV_TRACE_SKIP=1 \
  C2_CSV_LOG_LIMIT="$C2_CSV_LOG_LIMIT_VALUE" \
  C2_CSV_METHOD="$C2_CSV_METHOD_VALUE" \
  "$JAVA_BIN" \
    "${java_args[@]}" >"$BASE.vm.log" 2>&1 &
PID=$!

echo "pid=$PID" | tee "$BASE.meta"
echo "target=$TARGET_NAME" | tee -a "$BASE.meta"
echo "target_method=$TARGET_METHOD" | tee -a "$BASE.meta"
echo "match_any_fixed=$MATCH_ANY_FIXED" | tee -a "$BASE.meta"

prev=""
same=0
attach_fail=0

cleanup_child() {
  if kill -0 "$PID" 2>/dev/null; then
    kill "$PID" 2>/dev/null || true
    sleep 1
  fi
  if kill -0 "$PID" 2>/dev/null; then
    kill -9 "$PID" 2>/dev/null || true
  fi
}

trap cleanup_child EXIT

for ((i = 1; i <= MAX_SAMPLES; i++)); do
  if ! kill -0 "$PID" 2>/dev/null; then
    break
  fi

  out=$("$JCMD_BIN" "$PID" PerfCounter.print 2>/dev/null || true)
  if [[ -z "$out" ]]; then
    attach_fail=$((attach_fail + 1))
    printf 'sample=%03d method="" attach_fail=1\n' "$i" >>"$BASE.perf"
    sleep "$SAMPLE_INTERVAL"
    continue
  fi

  method=$(printf '%s\n' "$out" | sed -n 's/.*sun.ci.compilerThread.0.method="\([^"]*\)".*/\1/p' | tail -n 1)
  compiles=$(printf '%s\n' "$out" | sed -n 's/.*sun.ci.compilerThread.0.compiles=\([0-9][0-9]*\).*/\1/p' | tail -n 1)
  total=$(printf '%s\n' "$out" | sed -n 's/.*sun.ci.totalCompiles=\([0-9][0-9]*\).*/\1/p' | tail -n 1)
  osr=$(printf '%s\n' "$out" | sed -n 's/.*sun.ci.osrCompiles=\([0-9][0-9]*\).*/\1/p' | tail -n 1)
  timev=$(printf '%s\n' "$out" | sed -n 's/.*sun.ci.compilerThread.0.time=\([0-9][0-9]*\).*/\1/p' | tail -n 1)

  printf 'sample=%03d method="%s" compiles=%s total=%s osr=%s time=%s\n' \
    "$i" "$method" "${compiles:-}" "${total:-}" "${osr:-}" "${timev:-}" >>"$BASE.perf"

  matched=0
  if [[ "$MATCH_ANY_FIXED" == "1" ]]; then
    if [[ -n "$method" ]]; then
      matched=1
    fi
  elif [[ "$method" == "$TARGET_METHOD" ]]; then
    matched=1
  fi

  if [[ "$matched" == "1" && "$method" == "$prev" ]]; then
    same=$((same + 1))
  elif [[ "$matched" == "1" ]]; then
    prev="$method"
    same=1
  else
    prev="$method"
    same=0
  fi

  if [[ "$same" -ge "$SAME_THRESHOLD" ]]; then
    echo "trigger sample=$i method=$method same=$same attach_fail=$attach_fail" | tee -a "$BASE.meta"
    printf '%s\n' "$out" >"$BASE.trigger.perfcounter.txt"
    "$JSTACK_BIN" -l "$PID" >"$BASE.jstack.txt" 2>&1 || true
    lldb -p "$PID" -o 'thread list' -o 'thread backtrace all' -o 'detach' -o 'quit' >"$BASE.lldb.txt" 2>&1 || true
    break
  fi

  sleep "$SAMPLE_INTERVAL"
done

cleanup_child
trap - EXIT

echo "base=$BASE"
echo "meta:"
cat "$BASE.meta"
echo "perf-tail:"
tail -n 20 "$BASE.perf" || true
echo "step-dist:"
rg -o 'C2_CSV step .* step=[0-9]+' "$BASE.vm.log" | sed -E 's/.* step=([0-9]+)/\1/' | sort -n | uniq -c || true
echo "enter-probe-step:"
printf 'enter=%s probe=%s step=%s\n' \
  "$(rg -c 'C2_CSV enter' "$BASE.vm.log" || true)" \
  "$(rg -c 'C2_CSV probe' "$BASE.vm.log" || true)" \
  "$(rg -c 'C2_CSV step compile_id=' "$BASE.vm.log" || true)"
