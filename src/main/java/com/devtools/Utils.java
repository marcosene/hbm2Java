package com.devtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public interface Utils {

    Log LOG = LogFactory.getLog(Utils.class);

    static List<Element> getChildrenByTag(final Element parentElement, final String tagName) {
        final List<Element> matchingChildren = new ArrayList<>();

        // Get all direct children of the parent element
        final NodeList children = parentElement.getChildNodes();

        // Iterate through the NodeList
        for (int i = 0; i < children.getLength(); i++) {
            final Node childNode = children.item(i);

            // Check if the child node is an Element node (Node.ELEMENT_NODE)
            // This filters out text nodes, comments, processing instructions, etc.
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                final Element childElement = (Element) childNode; // Cast to Element

                // Check if the element has the desired tag name
                if (tagName == null || childElement.getTagName().equals(tagName)) {
                    matchingChildren.add(childElement);
                }
            }
        }

        return matchingChildren;
    }

    static Element getFirstChildByTag(final Element parentElement, final String tagName) {
        final List<Element> elements = getChildrenByTag(parentElement, tagName);
        if (elements.isEmpty()) {
            return null;
        }
        return elements.get(0);
    }

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

    static String lowercaseUntilLastUpper(final String input) {
        final int length = input.length();
        int lastUpperIndex = -1;

        // Find the last consecutive uppercase letter at the start
        for (int i = 0; i < length; i++) {
            if (!Character.isUpperCase(input.charAt(i))) {
                break;
            }
            lastUpperIndex = i;
        }

        // If there are any leading uppercase letters, process them
        if (lastUpperIndex >= 0) {
            final String lowerPart = input.substring(0, lastUpperIndex + 1).toLowerCase();
            return lowerPart + input.substring(lastUpperIndex + 1);
        }

        // Return the original string if no uppercase sequence was found
        return input;
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

    static String getFileNameNoExtensions(final String absoluteFilename) {
        // Extract just the filename
        String fileName = Paths.get(absoluteFilename).getFileName().toString();

        // Remove all extensions
        final int firstDotIndex = fileName.indexOf('.');
        if (firstDotIndex > 0) {
            fileName = fileName.substring(0, firstDotIndex);
        }

        // Capitalize the first letter
        if (!fileName.isEmpty()) {
            fileName = fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
        }
        return fileName;
    }

    static String toCamelCase(final String upperString) {
        if (StringUtils.isBlank(upperString)) {
            return "";
        }

        // Convert the entire string to lowercase first to handle mixed cases correctly
        final String lowerString = upperString.toLowerCase();

        final StringBuilder camelCaseBuilder = new StringBuilder();
        boolean capitalizeNext = false;

        for (int i = 0; i < lowerString.length(); i++) {
            final char currentChar = lowerString.charAt(i);

            if (currentChar == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    camelCaseBuilder.append(Character.toUpperCase(currentChar));
                    capitalizeNext = false;
                } else {
                    camelCaseBuilder.append(currentChar);
                }
            }
        }

        // Ensure the very first character is lowercase (for true camelCase)
        if (!camelCaseBuilder.isEmpty()) {
            camelCaseBuilder.setCharAt(0, Character.toLowerCase(camelCaseBuilder.charAt(0)));
        }

        return camelCaseBuilder.toString();
    }

    static void writeEntity(final String filename, final StringBuilder entityCode)
            throws IOException {
        final File outputFile = new File(filename);
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(entityCode.toString());
        }

        LOG.info("Entity successfully written to " + filename);
    }

    static boolean createFolder(final String path) {
        final File folder = new File(path);
        // Check if the outputFolder exists
        if (!folder.exists()) {
            // If it doesn't exist, try to create it
            if (folder.mkdirs()) {
                LOG.info("Folder created successfully: " + path);
            } else {
                LOG.error("Failed to create folder: " + path);
                return true;
            }
        } else if (!folder.isDirectory()) {
            LOG.error(path + " is not a directory.");
            return true;
        }
        return false;
    }

    static String getSimpleClass(final String fullClassName) {
        if (fullClassName == null) {
            return null;
        }
        return fullClassName.contains(".")
                ? fullClassName.substring(fullClassName.lastIndexOf(".") + 1)
                : fullClassName;
    }

    static Set<String> extractFullyQualifiedClassNames(final String input) {
        final Set<String> classNames = new HashSet<>();

        // Remove quoted strings (single and double)
        final String withoutQuotes = input.replaceAll("\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'", " ");

        // Match fully qualified identifiers: package.name.ClassName or package.name.ClassName.CONSTANT
        final Pattern pattern = Pattern.compile("((?:[a-zA-Z_$][\\w$]*\\.)+[A-Z][\\w$]*(?:\\.[A-Z_][A-Z_\\d]*)?)");

        final Matcher matcher = pattern.matcher(withoutQuotes);

        while (matcher.find()) {
            String match = matcher.group(1);

            // If the last part looks like a constant (e.g., ALL_CAPS), remove it
            final int lastDot = match.lastIndexOf('.');
            if (lastDot != -1) {
                final String lastToken = match.substring(lastDot + 1);
                if (lastToken.matches("[A-Z_][A-Z_\\d]*")) {
                    match = match.substring(0, lastDot);
                }
            }

            classNames.add(match);
        }

        return classNames;
    }

    /**
     * Removes package names from fully qualified Java class names while respecting:
     * 1) In a constant like "javax.persistence.InheritanceType.SINGLE_TABLE", only the class and constant ("InheritanceType.SINGLE_TABLE") remain.
     * 2) Text between double quotes is untouched.
     *
     * @param input the input string possibly containing fully qualified class names
     * @return the input string with package names removed from fully qualified names (outside quotes)
     */
    static String simplifyClassNames(final String input) {
        // Split the input on double quotes.
        // Even-index segments (0, 2, …) are outside quotes; odd-index segments are inside quotes.
        final String[] parts = input.split("\"", -1);
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 0) {
                // Process parts outside quotes using our replacement method.
                result.append(simplifyOutsideQuotes(parts[i]));
            } else {
                // Reinsert quotes along with the untouched text inside them.
                result.append("\"").append(parts[i]).append("\"");
            }
        }
        return result.toString();
    }

    /**
     * Processes text outside quotes, replacing fully qualified names with their simplified versions.
     * For example:
     *   "javax.persistence.Table"  -> "Table"
     *   "javax.persistence.InheritanceType.SINGLE_TABLE" -> "InheritanceType.SINGLE_TABLE"
     *
     * @param s the input text outside of quotes
     * @return the text with package names removed from fully qualified names
     */
    static String simplifyOutsideQuotes(final String s) {
        // This pattern matches a fully qualified name that:
        // • Starts with one or more package segments (all lowercase letters) followed by dots.
        // • Then an uppercase-starting class name (captured in group(1)).
        // • Optionally, if followed by a dot and an ALL-UPPERCASE (and underscores) token (captured in group(2)), that is considered a constant.
        final Pattern CLASS_PATTERN = Pattern.compile(
                "\\b(?:[a-z]+\\.)+([A-Z][A-Za-z0-9_]*)(?:\\.([A-Z][A-Z0-9_]*))?\\b"
        );

        final Matcher matcher = CLASS_PATTERN.matcher(s);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final String replacement;
            // If group 2 (the constant) is present, include it in the replacement.
            if (matcher.group(2) != null) {
                replacement = matcher.group(1) + "." + matcher.group(2);
            } else {
                replacement = matcher.group(1);
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
