package starry.util.window;

import org.lwjgl.glfw.GLFWNativeWin32;
import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.win32.*;

/**
 *  © 2025 Copyright starry Client 2.0
 *        All Rights Reserved ®
 */

public class WindowStyle {

    private static final int WDA_NONE = 0x00000000;
    private static final int WDA_EXCLUDEFROMCAPTURE = 0x00000011;

    public interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class);
        HRESULT DwmSetWindowAttribute(HWND hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    }

    public interface User32Capture extends StdCallLibrary {
        User32Capture INSTANCE = Native.load("user32", User32Capture.class, W32APIOptions.DEFAULT_OPTIONS);
        boolean SetWindowDisplayAffinity(HWND hwnd, int affinity);
    }

    public static void setDarkMode(long windowHandle) {
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(windowHandle);
        HWND hwndJna = new HWND(new Pointer(hwnd));
        int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
        Memory darkModeEnabled = new Memory(4);
        darkModeEnabled.setInt(0, 1);
        DwmApi.INSTANCE.DwmSetWindowAttribute(hwndJna, DWMWA_USE_IMMERSIVE_DARK_MODE, darkModeEnabled, 4);
    }

    public static boolean setCaptureExcluded(long windowHandle, boolean excluded) {
        if (!Platform.isWindows() || windowHandle == 0L) return false;
        try {
            long hwnd = GLFWNativeWin32.glfwGetWin32Window(windowHandle);
            if (hwnd == 0L) return false;
            return User32Capture.INSTANCE.SetWindowDisplayAffinity(
                    new HWND(new Pointer(hwnd)), excluded ? WDA_EXCLUDEFROMCAPTURE : WDA_NONE);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
