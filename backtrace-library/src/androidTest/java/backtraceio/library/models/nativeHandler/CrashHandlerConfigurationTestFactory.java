package backtraceio.library.models.nativeHandler;

/**
 * Test-only factories for {@link CrashHandlerConfiguration} instances whose package-private
 * provider constructor is not visible outside this package.
 */
public final class CrashHandlerConfigurationTestFactory {
    private CrashHandlerConfigurationTestFactory() {}

    /** Configuration whose linker lookup returns {@code loadedLibraryPath} and whose ABI provider throws. */
    public static CrashHandlerConfiguration withThrowingAbiProvider(final String loadedLibraryPath) {
        return new CrashHandlerConfiguration(() -> loadedLibraryPath, () -> {
            throw new IllegalStateException("Unable to determine the current process ABI");
        });
    }

    /** Configuration with no linker metadata and a fixed process ABI. */
    public static CrashHandlerConfiguration withFixedAbi(final String abi) {
        return new CrashHandlerConfiguration(() -> null, () -> abi);
    }

    /** Configuration with no linker metadata and a throwing ABI provider. */
    public static CrashHandlerConfiguration withoutLinkerPathAndThrowingAbiProvider() {
        return withThrowingAbiProvider(null);
    }
}
