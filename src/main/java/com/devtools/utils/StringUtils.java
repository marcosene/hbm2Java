package com.devtools.utils;

public final class StringUtils {

    private StringUtils() {}

    /**
     * Extracts the simple class name from a fully qualified class name.
     * @param fullClassName the full class name (e.g., "com.example.MyClass")
     * @return the simple class name (e.g., "MyClass") or null if input is null/empty
     */
    public static String getSimpleClassName(final String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) {
            return null;
        }
        return fullClassName.contains(".") ? 
            fullClassName.substring(fullClassName.lastIndexOf(".") + 1) : fullClassName;
    }

    /**
     * Extracts the package name from a fully qualified class name.
     * @param fullClassName the full class name (e.g., "com.example.MyClass")
     * @return the package name (e.g., "com.example") or empty string if no package
     */
    public static String getPackageName(final String fullClassName) {
        if (fullClassName != null && fullClassName.contains(".")) {
            return fullClassName.substring(0, fullClassName.lastIndexOf("."));
        }
        return "";
    }
}