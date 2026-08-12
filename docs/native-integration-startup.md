# Native integration startup and threading

## Scope

`BacktraceClient.enableNativeIntegration()` installs Crashpad for NDK/JNI crashes. Managed Java
exception handling and Backtrace ANR monitoring are separate integrations.

## Recommended initialization order

1. Create `BacktraceDatabase` with a writable application-private directory.
2. Create `BacktraceClient`.
3. Configure initial attributes, attachments, breadcrumbs, metrics, and the managed exception
   handler.
4. Call `enableNativeIntegration()` once, as early as practical.

Java:

```java
BacktraceDatabaseSettings settings =
        new BacktraceDatabaseSettings(new File(context.getFilesDir(), "backtrace").getAbsolutePath());
BacktraceDatabase database = new BacktraceDatabase(context, settings);
BacktraceClient client = new BacktraceClient(context, credentials, database, attributes, attachments);

BacktraceExceptionHandler.enable(client);
client.enableNativeIntegration();
```

Kotlin:

```kotlin
val settings = BacktraceDatabaseSettings(File(filesDir, "backtrace").absolutePath)
val database = BacktraceDatabase(applicationContext, settings)
val client = BacktraceClient(applicationContext, credentials, database, attributes, attachments)

BacktraceExceptionHandler.enable(client)
client.enableNativeIntegration()
```

## Main-thread behavior

Native initialization is synchronous because crash coverage begins only after Crashpad is
installed. Backtrace does not read the base APK or split APK ZIP central directory during this
call; Android's own linker may still access the installed APK when loading an unextracted native
library.
It obtains the exact `libbacktrace-native.so` path from Android's already-loaded module metadata and
uses path-only application metadata as a fallback.

The resolver performs O(number of split metadata entries) filesystem metadata checks — private and
public split arrays are scanned before deduplication, and none of the work scales with APK ZIP
central-directory size — and it does not open or parse APK ZIP contents. The remaining work consists of Crashpad database setup, native
attribute/attachment transfer, and Crashpad handler registration.

## Background-thread use

Calling `enableNativeIntegration()` from a single application-owned worker thread is supported. Do
not invoke it concurrently from multiple threads or race it with `disableNativeIntegration()`.

Asynchronous initialization creates a coverage window: a native fault before initialization
finishes cannot be handled by Backtrace. Managed exceptions remain independently covered when
`BacktraceExceptionHandler.enable(client)` has already been registered.

## Split APK and AAB installs

Resolution order is:

1. Exact filesystem or APK-backed path reported by Android's native linker.
2. Extracted `nativeLibraryDir/libbacktrace-native.so`, when present.
3. An ABI split identified by `splitSourceDirs`, `splitPublicSourceDirs`, and `splitNames`.
4. The historical base-APK path shape as a compatibility fallback.

The linker-reported path is authoritative and is validated structurally — it is accepted whenever it
names `lib/<abi>/libbacktrace-native.so` inside an existing container, and is deliberately *not*
compared against an ABI inferred in Java. Android has already resolved which module the process
loaded, so rejecting it on an ABI guess would break 32-bit processes on 64-bit devices and
native-bridge translation.

An inferred ABI is used only to construct steps 3 and 4 — the linker path and an existing extracted
library are both directly loadable without ABI inference, so the handler initializes from either
even when process ABI metadata is malformed or unavailable. That inferred value comes from `Build.CPU_ABI`, which
Android adjusts for the current process bitness, rather than `Build.SUPPORTED_ABIS[0]`, which
describes the device. On API 23 and later the defensive fallback for an empty `CPU_ABI` preserves
process bitness via `Process.is64Bit()`; API 21-22 can only fall back to the device-ordered list.

No fallback opens an APK. Split candidates from `splitSourceDirs` and `splitPublicSourceDirs` are
compared globally: selection prefers an exact `config.<abi>` split name, then an exact
`split_config.<abi>.apk` filename. A low-confidence filename token match is accepted only when
split-name metadata is globally unavailable (pre-API-26 installs) and the match is unique; two
equal-confidence candidates fall back to the base APK instead of being guessed by array order. ABI
matching treats `x86` and `x86_64` as distinct tokens and ignores language and density splits.

## Failure behavior

- Crash-handler registration is optional and failure-contained. The packaged Backtrace native
  library is currently loaded when `BacktraceBase` is initialized.
- `BacktraceClient.enableNativeIntegration()` returns normally and logs a contained failure; use
  `tryEnableNativeIntegration()` to observe the result, or
  `BacktraceDatabase.setupNativeIntegration()`, which returns `false`. Managed crash reporting
  stays operational either way.
- `dumpWithoutCrash()` is a safe no-op while native integration is uninitialized or disabled.
- The known-unsupported `x86` native backend remains skipped.
- An ABI that cannot be determined is not treated as known-unsupported, because a valid
  linker-reported path and an extracted library do not require ABI inference.
- In the crash-handler process, a library-load failure exits nonzero with a logged cause instead of
  appearing successful; that exit status is diagnostic and is not currently consumed by Crashpad.

## Validation

For release qualification, verify:

- monolithic APK with extracted native libraries;
- AAB/Google Play install with compressed native libraries in an ABI split, including the
  crash-handler process loading the split-backed path and a correlated report reaching Backtrace;
- cold-start main-thread trace contains no `ZipFile$Source.initCEN` under Backtrace initialization;
- native crash generated immediately after initialization is uploaded and symbolicated;
- API 21-22 devices (class loading, resolver construction, environment construction);
- a real 32-bit process on a 64-bit-capable device;
- Android 15/16 devices using 16 KB pages;
- all supported ABIs other than the intentionally unsupported `x86` native crash backend.

These qualification gates are independent of the source-level fix: the resolver performs bounded
filesystem metadata checks and no host APK ZIP central-directory parsing.
