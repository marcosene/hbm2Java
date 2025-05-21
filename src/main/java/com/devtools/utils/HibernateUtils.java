package com.devtools.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

public interface HibernateUtils {
    static String mapHibernateTypeToJava(final String hibernateType) {
        if (hibernateType == null) {
            return null;
        }
        return switch (hibernateType.toLowerCase()) {
            case "string", "text", "character", "char" -> String.class.getSimpleName();
            case "integer", "int" -> Integer.class.getSimpleName();
            case "long", "big_integer" -> Long.class.getSimpleName();
            case "short" -> Short.class.getSimpleName();
            case "byte" -> Byte.class.getSimpleName();
            case "boolean", "yes_no", "numeric_boolean" -> Boolean.class.getSimpleName();
            case "double", "float", "big_decimal", "decimal" -> Double.class.getSimpleName();
            case "date", "timestamp", "time" -> Date.class.getSimpleName();
            case "localdate" -> LocalDate.class.getCanonicalName();
            case "localdatetime" -> LocalDateTime.class.getCanonicalName();
            case "localtime" -> LocalTime.class.getCanonicalName();
            case "uuid" -> UUID.class.getSimpleName();
            case "binary", "blob" -> "byte[]";
            case "clob" -> String.class.getSimpleName();  // Usually treated as a large String in JPA
            case "serializable" -> Serializable.class.getSimpleName();
            case "set", "bag", "list", "map" -> Collection.class.getSimpleName();

            // Fallback for complex or custom types
            default -> hibernateType.endsWith("UserType") ?
                    hibernateType.replace("UserType", "") : hibernateType;  // Return as it is for user-defined or custom types
        };
    }

    // Helper method to check for native types
    static boolean isCustomType(final String type) {
        final Set<String> nativeTypes = new HashSet<>(Arrays.asList(
                "int", "long", "float", "double", "boolean", "char", "byte", "short", "Short",
                "String", "Integer", "Long", "Boolean", "Double", "Float", "Date",
                "BigDecimal", "BigInteger", "List", "Set", "Map", "Collection"
        ));
        return !nativeTypes.contains(type);
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
