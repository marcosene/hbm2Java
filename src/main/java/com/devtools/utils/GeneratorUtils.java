package com.devtools.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.devtools.model.jpa.JpaPrimaryKey;

/**
 * Utility class for parsing Hibernate generator parameters.
 * Handles extraction of initial values and allocation sizes from generator parameter strings.
 */
public final class GeneratorUtils {

    private GeneratorUtils() {
        // Utility class - prevent instantiation
    }

    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("^START WITH\\s+(\\d+)(?:\\s+CACHE\\s+(\\d+))?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Parses generator parameters to extract initial value or allocation size.
     * @param generatorParams the map containing generator parameters
     * @param param the parameter to extract (e.g., "initialValue" or "allocationSize")
     * @return the parsed value or empty string if not found/invalid
     */
    public static String parseGeneratorParameters(final Map<String, String> generatorParams, final String param) {
        if (param == null || !generatorParams.containsKey(JpaPrimaryKey.PARAMETERS)) {
            return "";
        }

        final String parameterValue = generatorParams.get(JpaPrimaryKey.PARAMETERS);
        final Matcher matcher = PARAMETERS_PATTERN.matcher(parameterValue);

        if (matcher.matches()) {
            if (JpaPrimaryKey.PARAMS_INITIAL_VALUE.equals(param) && StringUtils.isNotBlank(matcher.group(1))) {
                return matcher.group(1);
            } else if (JpaPrimaryKey.PARAMS_ALLOCATION_SIZE.equals(param) && StringUtils.isNotBlank(matcher.group(2))) {
                return matcher.group(2);
            }
            return "";
        } else {
            throw new IllegalArgumentException("Generator Parameter string does not match expected format "
                                               + "'START WITH <number> CACHE <number>': " + parameterValue);
        }
    }
}