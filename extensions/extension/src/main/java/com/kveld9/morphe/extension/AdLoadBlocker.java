package com.kveld9.morphe.extension;

import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Dispatches failure callbacks for Google AdMob ad loaders to prevent full-screen flows from hanging.
 */
@SuppressWarnings("unused")
public class AdLoadBlocker {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

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
            Constructor<?>[] ctors = errorClass.getDeclaredConstructors();
            Object instance = tryInstantiate(ctors);
            if (instance != null) {
                return instance;
            }
            Constructor<?> defaultCtor = errorClass.getDeclaredConstructor();
            defaultCtor.setAccessible(true);
            return defaultCtor.newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryInstantiate(Constructor<?>[] ctors) {
        for (int arity = 5; arity >= 3; arity--) {
            Object obj = instantiateMatching(ctors, arity);
            if (obj != null) {
                return obj;
            }
        }
        return instantiateMatching(ctors, 1);
    }

    private static Object instantiateMatching(Constructor<?>[] ctors, int targetArity) {
        for (Constructor<?> ctor : ctors) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == targetArity && params[0] == int.class) {
                try {
                    ctor.setAccessible(true);
                    return invokeConstructor(ctor, targetArity);
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
            default:
                return null;
        }
    }
}
