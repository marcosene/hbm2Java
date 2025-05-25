package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.devtools.utils.GeneratorUtils;

@Setter
@Getter
public class JpaPrimaryKey extends JpaAbstract {

    public static final String PARAMETERS = "parameters";
    public static final String PARAMS_SEQUENCE = "sequence";
    public static final String PARAMS_MAX_LO = "max_lo";
    public static final String PARAMS_INITIAL_VALUE = "initialValue";
    public static final String PARAMS_ALLOCATION_SIZE = "allocationSize";
    public static final String PARAMS_PROPERTY = "property";

    private String columnName;
    private String generatorType;
    private final Map<String, String> generatorParams = new HashMap<>();

    public void setColumnName(final String columnName) {
        if (StringUtils.isNotBlank(columnName)) {
            this.columnName = columnName;
        }
    }

    public void setGeneratorType(final String generatorType) {
        if (StringUtils.isNotBlank(generatorType)) {
            this.generatorType = generatorType;
        }
    }

    public String getGeneratorName() {
        return generatorParams.getOrDefault(PARAMS_SEQUENCE, "");
    }

    public String getIncrementSize() {
        return generatorParams.getOrDefault(PARAMS_MAX_LO, "");
    }

    public String getProperty() {
        return generatorParams.getOrDefault(PARAMS_PROPERTY, "");
    }

    public String getInitialValue() {
        return GeneratorUtils.parseGeneratorParameters(generatorParams, PARAMS_INITIAL_VALUE);
    }

    public String getAllocationSize() {
        return GeneratorUtils.parseGeneratorParameters(generatorParams, PARAMS_ALLOCATION_SIZE);
    }
}
