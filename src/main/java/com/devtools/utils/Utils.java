package com.devtools.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public interface Utils {

    Log LOG = LogFactory.getLog(Utils.class);

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

    static void writeFile(final String filename, final StringBuilder fileContent)
            throws IOException {
        final File outputFile = new File(filename);
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(fileContent.toString());
        }

        LOG.info("Successfully generated " + filename);
    }

    static boolean createFolder(final String path) {
        final File folder = new File(path);

        if (folder.exists()) {
            if (!folder.isDirectory()) {
                LOG.error(path + " exists but is not a directory.");
                return true; // Returning true to indicate failure
            }
            return false; // No need to create, it already exists
        }

        // Try creating the directory
        if (folder.mkdirs()) {
            LOG.info("Folder created successfully: " + path);
            return false; // Successfully created
        } else {
            LOG.error("Failed to create folder: " + path);
            return true; // Failure in folder creation
        }
    }

    static String getSimpleClass(final String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) {
            return null;
        }
        final int lastDotIndex = fullClassName.lastIndexOf('.');
        return (lastDotIndex != -1) ? fullClassName.substring(lastDotIndex + 1) : fullClassName;
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
