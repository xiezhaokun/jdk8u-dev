package sun.hotspot;

import java.lang.reflect.Executable;

public class WhiteBox {
    private static final WhiteBox INSTANCE = new WhiteBox();

    private WhiteBox() {
    }

    private static native void registerNatives();

    public static WhiteBox getWhiteBox() {
        return INSTANCE;
    }

    static {
        registerNatives();
    }

    private native boolean enqueueMethodForCompilation0(Executable method, int compLevel, int entryBci);

    public boolean enqueueMethodForCompilation(Executable method, int compLevel) {
        return enqueueMethodForCompilation0(method, compLevel, -1);
    }

    public boolean isMethodCompiled(Executable method) {
        return isMethodCompiled(method, false);
    }

    public native boolean isMethodCompiled(Executable method, boolean isOsr);

    public native boolean isMethodQueuedForCompilation(Executable method);
}
