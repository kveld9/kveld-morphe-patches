package com.kveld9.morphe.extension.tiktok;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Method;

public final class TikTokLoginHook {
    private static final String TAG = "MorpheTikTok";

    private TikTokLoginHook() {}

    /**
     * Resolves pending login listeners (e.g. VideoViewComponent resume) when mandatory login is triggered.
     * Invokes onResult(1, 2, null) on the main thread to signal login dismissal and resume video playback.
     */
    public static void notifyLoginResult(final Object listener) {
        if (listener == null) {
            return;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    Method[] methods = listener.getClass().getMethods();
                    for (Method method : methods) {
                        if ("onResult".equals(method.getName()) && method.getParameterTypes().length == 3) {
                            method.invoke(listener, 1, 2, null);
                            Log.d(TAG, "[MandatoryLoginBypass] Dispatched onResult(1, 2, null) to login listener.");
                            return;
                        }
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "[MandatoryLoginBypass] Error dispatching to login listener: " + t.getMessage());
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            new Handler(Looper.getMainLooper()).post(r);
        }
    }

    /**
     * Determines whether the active session is guest mode.
     * Returns true if unauthenticated to bypass mandatory login assertions, or false when logged in.
     */
    public static boolean isGuestMode() {
        try {
            Class<?> smClass = Class.forName("com.ss.android.ugc.aweme.framework.services.ServiceManager");
            Object sm = smClass.getMethod("get").invoke(null);
            if (sm != null) {
                Class<?> accServiceClass = Class.forName("com.ss.android.ugc.aweme.IAccountService");
                Object accService = smClass.getMethod("getService", Class.class).invoke(sm, accServiceClass);
                if (accService != null) {
                    Method getAccountUserService = accService.getClass().getMethod("LJIIJJI");
                    Object accUser = getAccountUserService.invoke(accService);
                    if (accUser != null) {
                        Method isLoginMethod = accUser.getClass().getMethod("isLogin");
                        boolean loggedIn = (Boolean) isLoginMethod.invoke(accUser);
                        return !loggedIn;
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "[MandatoryLoginBypass] Error checking login status: " + t.getMessage());
        }
        return true;
    }
}
