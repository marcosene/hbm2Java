package com.devtools.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * Utility class for string manipulation operations.
 * Complements Apache Commons Lang3 StringUtils with project-specific functionality.
 */
public final class StringUtils {

    private StringUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a string to camelCase format.
     * Handles underscore-separated strings and converts them to camelCase.
     * 
     * @param input the string to convert (typically underscore_separated)
     * @return the camelCase version of the input string
     */
    public static String toCamelCase(final String input) {
        if (org.apache.commons.lang3.StringUtils.isBlank(input)) {
            return "";
        }

        // Convert the entire string to lowercase first to handle mixed cases correctly
        final String lowerString = input.toLowerCase();

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

    /**
     * Extracts fully qualified class names from a text input.
     * Ignores quoted strings and filters out constants.
     * 
     * @param input the input text to analyze
     * @return a set of fully qualified class names found in the input
     */
    public static Set<String> extractFullyQualifiedClassNames(final String input) {
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
    public static String removePackagesFromText(final String input) {
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
    private static String simplifyOutsideQuotes(final String s) {
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