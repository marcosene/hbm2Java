package com.devtools.utils;

/**
 * Utility class for handling Java class name operations.
 * Provides methods to extract package names, simple class names, and manipulate fully qualified class names.
 */
public final class ClassNameUtils {

    private ClassNameUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts the simple class name from a fully qualified class name.
     * Uses the same logic as {@link Class#getSimpleName()}.
     * 
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
     * 
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