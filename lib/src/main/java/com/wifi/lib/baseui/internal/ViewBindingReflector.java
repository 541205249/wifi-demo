package com.wifi.lib.baseui.internal;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class ViewBindingReflector {
    private ViewBindingReflector() {
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <VB extends ViewBinding> VB inflate(@NonNull Object host, @NonNull LayoutInflater inflater) {
        return (VB) inflateInternal(resolveBindingClass(host.getClass()), inflater, null, false);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <VB extends ViewBinding> VB inflate(
            @NonNull Object host,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            boolean attachToParent
    ) {
        return (VB) inflateInternal(resolveBindingClass(host.getClass()), inflater, parent, attachToParent);
    }

    @NonNull
    private static Object inflateInternal(
            @NonNull Class<?> bindingClass,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            boolean attachToParent
    ) {
        try {
            Method method = bindingClass.getMethod(
                    "inflate",
                    LayoutInflater.class,
                    ViewGroup.class,
                    boolean.class
            );
            return method.invoke(null, inflater, parent, attachToParent);
        } catch (NoSuchMethodException ignored) {
            try {
                Method method = bindingClass.getMethod("inflate", LayoutInflater.class);
                return method.invoke(null, inflater);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException("Unable to inflate ViewBinding for " + bindingClass.getName(), e);
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to inflate ViewBinding for " + bindingClass.getName(), e);
        }
    }

    @NonNull
    private static Class<?> resolveBindingClass(@NonNull Class<?> sourceClass) {
        Class<?> current = sourceClass;
        while (current != null && current != Object.class) {
            Type genericSuperclass = current.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                Type[] arguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
                for (Type argument : arguments) {
                    Class<?> candidate = unwrapType(argument);
                    if (candidate != null && ViewBinding.class.isAssignableFrom(candidate)) {
                        return candidate;
                    }
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Cannot resolve ViewBinding generic type from " + sourceClass.getName());
    }

    @Nullable
    private static Class<?> unwrapType(@NonNull Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            }
        }
        return null;
    }
}
