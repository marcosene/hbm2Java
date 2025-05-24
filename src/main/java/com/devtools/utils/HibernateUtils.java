package com.devtools.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.N;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

public interface HibernateUtils {

    Set<String> PRIMITIVE_TYPES = new HashSet<>(Arrays.asList(
            "int", "long", "float", "double", "boolean", "char", "byte", "short"
    ));

    Set<String> NATIVE_TYPES = new HashSet<>(Arrays.asList(
            "String", "Integer", "Long", "Boolean", "Double", "Float", "Date",
            "BigDecimal", "BigInteger", "List", "Set", "Map", "Collection"
    ));

    static String mapHibernateTypeToJava(final String hibernateType) {
        return mapHibernateTypeToJava(hibernateType, false);
    }

    static String mapHibernateTypeToJava(final String hibernateType, final boolean fullName) {
        if (hibernateType == null) {
            return null;
        }
        final Class<?> clazz = switch (hibernateType.toLowerCase()) {
            case "string", "text", "character", "char" -> String.class;
            case "integer", "int" -> Integer.class;
            case "long", "big_integer" -> Long.class;
            case "short" -> Short.class;
            case "byte" -> Byte.class;
            case "boolean", "yes_no", "numeric_boolean" -> Boolean.class;
            case "double", "float", "big_decimal", "decimal" -> Double.class;
            case "date", "timestamp", "time" -> Date.class;
            case "localdate" -> LocalDate.class;
            case "localdatetime" -> LocalDateTime.class;
            case "localtime" -> LocalTime.class;
            case "uuid" -> UUID.class;
            case "clob" -> String.class;  // Usually treated as a large String in JPA
            case "serializable" -> Serializable.class;
            case "set", "bag", "list", "map" -> Collection.class;

            // Fallback for complex or custom types
            default -> null;
        };

        if (clazz != null) {
            return fullName ? clazz.getCanonicalName() : clazz.getName();
        } else {
            return fullName ? hibernateType : Utils.getSimpleClass(hibernateType);
        }
    }

    // Helper method to check for native types
    static boolean isPrimitiveType(final String type) {
        return PRIMITIVE_TYPES.contains(type);
    }

    // Helper method to check for custom types
    static boolean isCustomType(final String type) {
        return !NATIVE_TYPES.contains(type) && !PRIMITIVE_TYPES.contains(type);
    }

    static String getDiscriminatorType(final String type) {
        return switch (type) {
            case "string" -> "DiscriminatorType.STRING";
            case "char" -> "DiscriminatorType.CHAR";
            case "int" -> "DiscriminatorType.INTEGER";
            default -> throw new IllegalStateException("Unexpected discriminator type: " + type);
        };
    }

    static String convertCascadeTypes(final String cascade) {
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
