package com.mailscale.common;

public final class CorrelationHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private CorrelationHolder() {
    }

    public static void set(String id) {
        HOLDER.set(id);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
