package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class JpaPrimaryKey extends JpaAnnotation {

    public static final String PARAMETERS = "parameters";
    public static final String PARAMS_SEQUENCE = "sequence";
    public static final String PARAMS_MAX_LO = "max_lo";
    public static final String PARAMS_INITIAL_VALUE = "initialValue";
    public static final String PARAMS_ALLOCATION_SIZE = "allocationSize";
    public static final String PARAMS_PROPERTY = "property";

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
        return com.devtools.utils.GeneratorUtils.parseGeneratorParameters(PARAMS_INITIAL_VALUE, generatorParams, PARAMETERS);
    }

    public String getAllocationSize() {
        return com.devtools.utils.GeneratorUtils.parseGeneratorParameters(PARAMS_ALLOCATION_SIZE, generatorParams, PARAMETERS);
    }

    public String getProperty() {
        return generatorParams.getOrDefault(PARAMS_PROPERTY, "");
    }


}
