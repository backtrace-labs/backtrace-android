# Native integration startup and threading

## Scope

`BacktraceClient.enableNativeIntegration()` installs Crashpad for NDK/JNI crashes and requires a
writable `BacktraceDatabase`. Managed Java exception handling and Backtrace ANR monitoring operate
independently of native crash-handler registration.

## Recommended initialization order

1. Create `BacktraceDatabase` with a writable application-private directory.
2. Create `BacktraceClient` with its credentials.
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

## Synchronous startup behavior

Native initialization is synchronous because crash coverage begins only after Crashpad is
installed. `enableNativeIntegration()` preserves its existing `void` API and returns normally when
an optional native setup failure is contained. Use `tryEnableNativeIntegration()`,
`tryEnableNativeIntegration(boolean)`, or
`tryEnableNativeIntegration(boolean, UnwindingMode)` when the application needs to know whether
registration succeeded.

Backtrace does not read or parse the base APK or split APK ZIP central directory during
initialization. The SDK obtains the exact `libbacktrace-native.so` path from Android's already-loaded
module metadata and uses path-only application metadata as a fallback. Android's linker may still
access an installed APK when it loads an unextracted native library.

Path resolution performs a bounded number of filesystem metadata checks based on the installed
split entries. Its work does not scale with the number of files in an APK.

## Background-thread use

Calling `enableNativeIntegration()` from one application-controlled worker thread is supported. Do
not invoke it concurrently from multiple threads or race it with `disableNativeIntegration()`.

Background initialization creates a coverage window: a native fault before initialization
finishes cannot be handled by Backtrace. Managed exceptions remain independently covered when
`BacktraceExceptionHandler.enable(client)` has already been registered.

## Split APK and Android App Bundle installs

Crash-handler library resolution uses this order:

1. The exact filesystem or APK-backed path reported by Android's native linker.
2. An existing extracted `nativeLibraryDir/libbacktrace-native.so`.
3. An ABI split identified by installed split metadata.
4. The base-APK path shape as a compatibility fallback.

The linker-reported path is authoritative and is not compared against an ABI inferred in Java.
Android has already selected the module loaded by the current process, so this behavior supports
32-bit processes on 64-bit devices and native-bridge translation.

ABI inference is used only to construct split and base-APK fallback paths. It prefers
`Build.CPU_ABI`, which Android adjusts for the current process. On API 23 and later, an empty or
malformed primary ABI falls back according to the process bitness reported by `Process.is64Bit()`.
API 21 and 22 use the device-ordered ABI list when the primary value is unavailable.

No fallback opens an APK. Split matching keeps `x86` and `x86_64` distinct and does not select an
ambiguous split candidate.

## Failure behavior

- Native crash-handler registration is optional. A contained registration failure leaves managed
  crash reporting operational.
- `enableNativeIntegration()` returns normally after a contained failure. Use
  `tryEnableNativeIntegration()` to observe the registration result.
- `BacktraceDatabase.setupNativeIntegration()` returns `false` when native registration does not
  succeed.
- `dumpWithoutCrash()` is a safe no-op while native integration is unavailable or disabled.
- A crash-handler process that cannot load or invoke the handler exits with a nonzero status.

## Retry and process lifecycle

Native handler initialization is process-scoped. Applications should configure native attributes,
attachments, breadcrumbs, and credentials before initialization and make at most one initial
registration attempt per process.

A Java-side validation or preparation failure is contained and returns `false` from
`tryEnableNativeIntegration()`. A native initialization attempt is protected by a process-global
once-only initialization boundary, so applications should not repeatedly retry native
initialization in a loop. Continue using managed reporting and correct the configuration before the
next application process start.

After a successful initial registration, `disableNativeIntegration()` disables native uploads for
the current process. A later enable call restarts the Crashpad upload thread.

## Compatibility and limitations

- Android API 21 is the minimum supported API level.
- Monolithic APK and Android App Bundle/split APK installations are supported.
- The native-linker path is authoritative when available.
- An existing extracted native library can be used without ABI inference.
- The `x86` native crash backend is unsupported.
- `x86_64`, `armeabi-v7a`, and `arm64-v8a` native crash backends are supported.
- Managed exception reporting and ANR monitoring remain available independently of native
  crash-handler registration.
- `arm64-v8a` and `x86_64` support flexible 16 KB page sizes.

## Native integration diagnostic codes

Optional native failures omit exception messages and stack traces because they may contain
credentials, application-provided data, or filesystem paths. Log entries use a stable diagnostic
code and, where an exception was caught, its class name.

| Code | Meaning |
| --- | --- |
| `BT_NATIVE_PREPARE_FAILURE` | Native configuration or environment preparation failed. |
| `BT_NATIVE_BRIDGE_FAILURE` | The JNI/native initialization bridge failed. |
| `BT_NATIVE_BREADCRUMB_HOOK_FAILURE` | The breadcrumb synchronization hook could not be installed. Native integration remains enabled. |
| `BT_NATIVE_DISABLE_FAILURE` | The native disable bridge failed. Java-side enabled state is still cleared. |
| `BT_NATIVE_DUMP_UNAVAILABLE` | A dump was requested while native integration was unavailable. |
| `BT_HANDLER_ENV_UNAVAILABLE` | The crash-handler process environment was unavailable. |
| `BT_HANDLER_PATH_UNAVAILABLE` | The crash-handler native-library path was unavailable. |
| `BT_HANDLER_LOAD_FAILURE` | The crash-handler native library could not be loaded. |
| `BT_HANDLER_DISPATCH_FAILURE` | Dispatch to the native crash handler failed. |
| `BT_HANDLER_RETURNED_FAILURE` | The native crash handler returned a failure result. |
