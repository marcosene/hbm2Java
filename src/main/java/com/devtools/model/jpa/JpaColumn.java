package com.devtools.model.jpa;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@NoArgsConstructor
public class JpaColumn extends JpaAnnotation {

    public enum NaturalId {
        NONE, MUTABLE, IMMUTABLE
    }

    private String type;
    private String columnName;
    private Integer length = 255;
    private boolean nullable = true;
    private boolean updatable = true;
    private String foreignKey;
    private String index;
    private boolean version = false;
    private boolean unique = false;
    private boolean lazy = false;
    private boolean optimisticLock = true;
    private String defaultValue;
    private String uniqueConstraint = null;
    private boolean composite = false;
    private boolean embedded = false;
    private boolean inverseJoin = false;
    private NaturalId naturalId = NaturalId.NONE;
    private String columnDefinition;
    private Integer precision = 0;
    private Integer scale = 0;
    private Map<String, String> typeParams = new LinkedHashMap<>();

    public String getType(final boolean returnType) {
        if (!typeParams.isEmpty() && returnType && typeParams.containsKey("enumClass")) {
            return typeParams.get("enumClass");
        }
        if (type != null && returnType) {
            final String normalizedReturnType = type.endsWith("UserType") ? type.replace("UserType", "") :
                    type.endsWith("Type") ? type.replace("Type", "") : type;
            return normalizedReturnType.replace(".usertypes", "");
        } else {
            return type;
        }
    }

    public String getType() {
        return getType(true);
    }

    public void setType(final String type) {
        if (StringUtils.isNotBlank(type)) {
            this.type = type;
        }
    }

    public void setName(final String name) {
        if (StringUtils.isNotBlank(name)) {
            this.name = name;
        }
    }

    public void setColumnName(final String columnName) {
        if (StringUtils.isNotBlank(columnName)) {
            this.columnName = columnName;
        }
    }

    public void setLength(final String length) {
        if (StringUtils.isNotBlank(length)) {
            this.length = Integer.parseInt(length);
        }
    }

    public void setForeignKey(final String foreignKey) {
        if (StringUtils.isNotBlank(foreignKey)) {
            this.foreignKey = foreignKey;
        }
    }

    public void setIndex(final String index) {
        if (StringUtils.isNotBlank(index)) {
            this.index = index;
        }
    }

    public void setDefaultValue(final String defaultValue) {
        if (StringUtils.isNotBlank(defaultValue)) {
            this.defaultValue = defaultValue;
        }
    }

    public void setUniqueConstraint(final String uniqueConstraint) {
        if (StringUtils.isNotBlank(uniqueConstraint)) {
            this.uniqueConstraint = uniqueConstraint;
        }
    }

    public void setColumnDefinition(final String columnDefinition) {
        if (StringUtils.isNotBlank(columnDefinition)) {
            this.columnDefinition = columnDefinition;
        }
    }

    public void addTypeParam(final String key, final String value) {
        typeParams.put(key, value);
    }
}
