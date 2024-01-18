package dev.mccue.pagesize;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.IntSupplier;

public final class PageSize {
    private PageSize() {}

    private static final IntSupplier GETTER
            = System.getProperty("os.name")
                .toLowerCase()
                .contains("win")
            ? new Win()
            : new Nix();

    static final class Win implements IntSupplier {
        /*
         * https://learn.microsoft.com/en-us/windows/win32/api/sysinfoapi/ns-sysinfoapi-system_info
         */
        static final MemoryLayout SYSTEM_INFO_LAYOUT = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("dwOemId"),
                ValueLayout.JAVA_INT.withName("dwPageSize"),
                ValueLayout.ADDRESS.withName("lpMinimumApplicationAddress"),
                ValueLayout.ADDRESS.withName("lpMaximumApplicationAddress"),
                ValueLayout.ADDRESS.withName("dwActiveProcessorMask"),
                ValueLayout.JAVA_INT.withName("dwNumberOfProcessors"),
                ValueLayout.JAVA_INT.withName("dwProcessorType"),
                ValueLayout.JAVA_INT.withName("dwAllocationGranularity"),
                ValueLayout.JAVA_SHORT.withName("wProcessorLevel"),
                ValueLayout.JAVA_SHORT.withName("wProcessorRevision")
        );

        static final MethodHandle GetSystemInfo_MH;
        static final VarHandle dwPageSize_VH;

        static {
            var linker = Linker.nativeLinker();
            var lookup = linker.defaultLookup();
            var GetSystemInfo = lookup.find("GetSystemInfo")
                    .orElseThrow();
            GetSystemInfo_MH = MethodHandles.insertArguments(
                    linker.downcallHandle(
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
                    ),
                    0,
                    GetSystemInfo
            );

            dwPageSize_VH = SYSTEM_INFO_LAYOUT.varHandle(
                    MemoryLayout.PathElement.groupElement("dwPageSize")
            );
        }

        @Override
        public int getAsInt() {
            try (var arena = Arena.ofConfined()) {
                var systemInfo = arena.allocate(SYSTEM_INFO_LAYOUT);
                try {
                    GetSystemInfo_MH.invoke(systemInfo);
                } catch (Throwable t) {
                    throw new IllegalStateException(t);
                }

                return (int) dwPageSize_VH.get(systemInfo);
            }
        }
    }

    static final class Nix implements IntSupplier {
        static final MethodHandle getpagesize_MH;
        static {
            var linker = Linker.nativeLinker();
            var lookup = linker.defaultLookup();
            var getpagesize = lookup.find("getpagesize")
                    .orElseThrow();
            getpagesize_MH = MethodHandles.insertArguments(
                    linker.downcallHandle(
                            FunctionDescriptor.of(ValueLayout.JAVA_INT)
                    ),
                    0,
                    getpagesize
            );
        }

        @Override
        public int getAsInt() {
            try {
                return (int) getpagesize_MH.invokeExact();
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }
    }

    public static int get() {
        return GETTER.getAsInt();
    }
}
