package com.devtools.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for parsing Hibernate generator parameters.
 * Handles extraction of initial values and allocation sizes from generator parameter strings.
 */
public final class GeneratorUtils {

    private GeneratorUtils() {}

    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("^START WITH\\s+(\\d+)(?:\\s+CACHE\\s+(\\d+))?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Parses generator parameters to extract initial value or allocation size.
     * @param paramValue the parameter to extract (e.g., "initialValue" or "allocationSize")
     * @param generatorParams the map containing generator parameters
     * @param parametersKey the key for the parameters string in the map
     * @return the parsed value or empty string if not found/invalid
     */
    public static String parseGeneratorParameters(final String paramValue, final Map<String, String> generatorParams, final String parametersKey) {
        if (paramValue == null || !generatorParams.containsKey(parametersKey)) {
            return "";
        }
        
        final Matcher matcher = PARAMETERS_PATTERN.matcher(generatorParams.get(parametersKey));

        if (matcher.matches()) {
            try {
                if ("initialValue".equals(paramValue) && StringUtils.isNotBlank(matcher.group(1))) {
                    return matcher.group(1);
                } else if ("allocationSize".equals(paramValue) && StringUtils.isNotBlank(matcher.group(2))) {
                    return matcher.group(2);
                }
                return "";
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Could not parse generator parameters for: " + paramValue, e);
            }
        } else {
            throw new IllegalArgumentException(
                    "Generator Parameter string does not match expected format 'START WITH <number> CACHE <number>': " + paramValue);
        }
    }
}