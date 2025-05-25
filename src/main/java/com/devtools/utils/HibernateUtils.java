package com.devtools.utils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

/**
 * Utility class for Hibernate to JPA mapping operations.
 * Provides methods to convert Hibernate types to Java types, handle discriminators, and cascade types.
 */
public final class HibernateUtils {

    private HibernateUtils() {
        // Utility class - prevent instantiation
    }

    /** Set of Java primitive type names. */
    public static final Set<String> PRIMITIVE_TYPES = new HashSet<>(Arrays.asList(
            "int", "long", "float", "double", "boolean", "char", "byte", "short"
    ));

    /** Set of Java native/wrapper type names. */
    public static final Set<String> NATIVE_TYPES = new HashSet<>(Arrays.asList(
            "String", "Integer", "Long", "Boolean", "Double", "Float", "Date", "Byte", "Character",
            "BigDecimal", "BigInteger", "List", "Set", "Map", "Collection"
    ));

    /**
     * Maps a Hibernate type to its corresponding Java type.
     * Uses simple class names by default.
     * 
     * @param hibernateType the Hibernate type name
     * @return the corresponding Java type name
     */
    public static String mapHibernateTypeToJava(final String hibernateType) {
        return mapHibernateTypeToJava(hibernateType, false);
    }

    /**
     * Maps a Hibernate type to its corresponding Java type.
     * 
     * @param hibernateType the Hibernate type name
     * @param fullName if true, returns fully qualified class names; if false, returns simple class names
     * @return the corresponding Java type name
     */
    public static String mapHibernateTypeToJava(final String hibernateType, final boolean fullName) {
        if (hibernateType == null) {
            return null;
        }
        final Class<?> clazz = switch (hibernateType.toLowerCase()) {
            case "string", "text", "character", "char" -> String.class;
            case "integer", "int" -> Integer.class;
            case "long", "big_integer" -> Long.class;
            case "big_decimal" -> BigDecimal.class;
            case "short" -> Short.class;
            case "byte" -> Byte.class;
            case "boolean", "yes_no", "numeric_boolean" -> Boolean.class;
            case "double", "float", "decimal" -> Double.class;
            case "date", "timestamp", "time" -> Date.class;
            case "localdate" -> LocalDate.class;
            case "localdatetime" -> LocalDateTime.class;
            case "localtime" -> LocalTime.class;
            case "uuid" -> UUID.class;
            case "clob" -> String.class;  // Usually treated as a large String in JPA
            case "serializable" -> Serializable.class;
            case "set", "bag", "list" -> Collection.class;
            case "map" -> Map.class;

            // Fallback for complex or custom types
            default -> null;
        };

        if (clazz != null) {
            return fullName ? clazz.getCanonicalName() : clazz.getSimpleName();
        } else {
            return fullName ? hibernateType : ClassNameUtils.getSimpleClassName(hibernateType);
        }
    }

    /**
     * Checks if the given type is a Java primitive type.
     *
     * @param type the type name to check
     * @return true if the type is a primitive type, false otherwise
     */
    public static boolean isPrimitiveType(final String type) {
        return PRIMITIVE_TYPES.contains(type);
    }

    /**
     * Checks if the given type is a custom (non-native, non-primitive) type.
     * 
     * @param type the type name to check
     * @return true if the type is a custom type, false otherwise
     */
    public static boolean isCustomType(final String type) {
        return !NATIVE_TYPES.contains(type) && !PRIMITIVE_TYPES.contains(type);
    }

    /**
     * Converts a discriminator type string to its JPA DiscriminatorType enum equivalent.
     * 
     * @param type the discriminator type ("string", "char", or "int")
     * @return the corresponding DiscriminatorType enum reference
     * @throws IllegalStateException if the type is not supported
     */
    public static String getDiscriminatorType(final String type) {
        return switch (type) {
            case "string" -> "DiscriminatorType.STRING";
            case "char" -> "DiscriminatorType.CHAR";
            case "int" -> "DiscriminatorType.INTEGER";
            default -> throw new IllegalStateException("Unexpected discriminator type: " + type);
        };
    }

    /**
     * Converts Hibernate cascade types to JPA CascadeType equivalents.
     * 
     * @param cascade the Hibernate cascade string (comma-separated values)
     * @return the corresponding JPA CascadeType references as a string
     */
    public static String convertCascadeTypes(final String cascade) {
        final StringBuilder cascadeTypes = new StringBuilder();

        if (StringUtils.isNotBlank(cascade)) {
            final String[] cascadeArray = cascade.split(",");
            for (final String cascadeType : cascadeArray) {
                // Convert the Hibernate cascade value to JPA CascadeType equivalent
                switch (cascadeType.trim()) {
                    case "save-update":
                        cascadeTypes.append("javax.persistence.CascadeType.PERSIST, ");
                        cascadeTypes.append("javax.persistence.CascadeType.MERGE, ");
                        break;
                    case "delete":
                        cascadeTypes.append("javax.persistence.CascadeType.REMOVE, ");
                        break;
                    case "all":
                        cascadeTypes.append("javax.persistence.CascadeType.ALL, ");
                        break;
                    case "all-delete-orphan":
                        cascadeTypes.append("javax.persistence.CascadeType.PERSIST, ");
                        cascadeTypes.append("javax.persistence.CascadeType.MERGE, ");
                        cascadeTypes.append("javax.persistence.CascadeType.REMOVE, ");
                        cascadeTypes.append("javax.persistence.CascadeType.DETACH, ");
                        break;
                    case "save":
                        cascadeTypes.append("javax.persistence.CascadeType.PERSIST, ");
                        break;
                    case "update":
                        cascadeTypes.append("javax.persistence.CascadeType.MERGE, ");
                        break;
                    default:
                        // Handle unknown cascade types if necessary
                        break;
                }
            }

            // Remove the trailing comma and space
            if (!cascadeTypes.isEmpty()) {
                cascadeTypes.setLength(cascadeTypes.length() - 2);
            }
        }

        return cascadeTypes.toString();
    }
}
