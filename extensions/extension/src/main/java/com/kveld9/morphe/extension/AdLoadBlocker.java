package com.kveld9.morphe.extension;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatches failure callbacks for Google AdMob ad loaders to prevent full-screen flows from hanging.
 */
@SuppressWarnings("unused")
public class AdLoadBlocker {

    private static final String TAG = "Morphe";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<Class<?>, ErrorConstructor> ERROR_CONSTRUCTOR_CACHE =
            new ConcurrentHashMap<Class<?>, ErrorConstructor>();

    private static final class ErrorConstructor {
        final Constructor<?> constructor;
        final int arity;

        ErrorConstructor(Constructor<?> constructor, int arity) {
            this.constructor = constructor;
            this.arity = arity;
        }

        Object newInstance() throws Exception {
            if (constructor == null) {
                return null;
            }
            return invokeConstructor(constructor, arity);
        }
    }

    public static void notifyLoadFailed(final Object callback) {
        if (callback == null) {
            return;
        }
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                dispatchFailed(callback);
            }
        });
    }

    private static void dispatchFailed(Object callback) {
        try {
            Class<?> clazz = callback.getClass();
            while (clazz != null && clazz != Object.class) {
                if (tryInvokeOnClass(clazz, callback)) {
                    return;
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean tryInvokeOnClass(Class<?> clazz, Object callback) throws Exception {
        for (Method method : clazz.getDeclaredMethods()) {
            if (isAdFailedMethod(method)) {
                if (invokeOnAdFailed(method, callback)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAdFailedMethod(Method method) {
        return "onAdFailedToLoad".equals(method.getName()) && method.getParameterTypes().length == 1;
    }

    private static boolean invokeOnAdFailed(Method method, Object callback) throws Exception {
        method.setAccessible(true);
        Class<?> paramType = method.getParameterTypes()[0];
        if (paramType == int.class) {
            method.invoke(callback, 3);
            return true;
        }
        Object error = createLoadAdError(paramType);
        if (error != null) {
            method.invoke(callback, error);
            return true;
        }
        return false;
    }

    private static Object createLoadAdError(Class<?> errorClass) {
        try {
            ErrorConstructor ec = resolveErrorConstructor(errorClass);
            return ec.newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ErrorConstructor resolveErrorConstructor(Class<?> errorClass) {
        ErrorConstructor cached = ERROR_CONSTRUCTOR_CACHE.get(errorClass);
        if (cached != null) {
            return cached;
        }
        synchronized (ERROR_CONSTRUCTOR_CACHE) {
            cached = ERROR_CONSTRUCTOR_CACHE.get(errorClass);
            if (cached != null) {
                return cached;
            }

            ErrorConstructor found = findWorkingConstructor(errorClass);
            if (found == null) {
                Log.w(TAG, "[Remove Ads] No compatible constructor found for " + errorClass.getName());
                found = new ErrorConstructor(null, -1);
            }
            ERROR_CONSTRUCTOR_CACHE.put(errorClass, found);
            return found;
        }
    }

    private static ErrorConstructor findWorkingConstructor(Class<?> errorClass) {
        try {
            Constructor<?>[] ctors = errorClass.getDeclaredConstructors();
            for (int arity = 5; arity >= 3; arity--) {
                ErrorConstructor match = findMatchingConstructor(ctors, arity);
                if (match != null) {
                    return match;
                }
            }
            ErrorConstructor match1 = findMatchingConstructor(ctors, 1);
            if (match1 != null) {
                return match1;
            }
            try {
                Constructor<?> defaultCtor = errorClass.getDeclaredConstructor();
                defaultCtor.setAccessible(true);
                defaultCtor.newInstance();
                return new ErrorConstructor(defaultCtor, 0);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ErrorConstructor findMatchingConstructor(Constructor<?>[] ctors, int targetArity) {
        for (Constructor<?> ctor : ctors) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == targetArity && params[0] == int.class) {
                try {
                    ctor.setAccessible(true);
                    invokeConstructor(ctor, targetArity);
                    return new ErrorConstructor(ctor, targetArity);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Object invokeConstructor(Constructor<?> ctor, int arity) throws Exception {
        switch (arity) {
            case 5:
                return ctor.newInstance(3, "blocked", "com.google.android.gms.ads", null, null);
            case 4:
                return ctor.newInstance(3, "blocked", "com.google.android.gms.ads", null);
            case 3:
                return ctor.newInstance(3, "blocked", "com.google.android.gms.ads");
            case 1:
                return ctor.newInstance(3);
            case 0:
                return ctor.newInstance();
            default:
                return null;
        }
    }
}
