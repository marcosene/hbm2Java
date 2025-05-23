package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

@Setter
@Getter
public class JpaPrimaryKey extends JpaAnnotation {

    public static final String PARAMETERS = "parameters";
    public static final String PARAMS_SEQUENCE = "sequence";
    public static final String PARAMS_MAX_LO = "max_lo";
    public static final String PARAMS_INITIAL_VALUE = "initialValue";
    public static final String PARAMS_ALLOCATION_SIZE = "allocationSize";
    public static final String PARAMS_PROPERTY = "property";

    private String type;
    private String columnName;
    private String generatorType;
    private final Map<String, String> generatorParams = new HashMap<>();

    public String getGeneratorName() {
        return generatorParams.getOrDefault(PARAMS_SEQUENCE, "");
    }

    public String getIncrementSize() {
        return generatorParams.getOrDefault(PARAMS_MAX_LO, "");
    }

    public String getInitialValue() {
        return parseGeneratorParameters(PARAMS_INITIAL_VALUE);
    }

    public String getAllocationSize() {
        return parseGeneratorParameters(PARAMS_ALLOCATION_SIZE);
    }

    public String getProperty() {
        return generatorParams.getOrDefault(PARAMS_PROPERTY, "");
    }

    private String parseGeneratorParameters(final String paramValue) {
        if (paramValue == null || !generatorParams.containsKey(PARAMETERS)) {
            return "";
        }
        final Pattern PARAMETERS_PATTERN = Pattern.compile("^START WITH\\s+(\\d+)(?:\\s+CACHE\\s+(\\d+))?$",
                Pattern.CASE_INSENSITIVE);
        final Matcher matcher = PARAMETERS_PATTERN.matcher(generatorParams.get(PARAMETERS));

        if (matcher.matches()) {
            try {
                // Convert the captured strings to integers
                if (PARAMS_INITIAL_VALUE.equals(paramValue) && StringUtils.isNotBlank(matcher.group(1))) {
                    return matcher.group(1);
                } else {
                    if (PARAMS_ALLOCATION_SIZE.equals(paramValue) && StringUtils.isNotBlank(matcher.group(2))) {
                        return matcher.group(2);
                    }
                    return "";
                }
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
