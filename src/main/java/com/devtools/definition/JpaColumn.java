package com.devtools.definition;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class JpaColumn {

    public enum NaturalId {
        NONE, MUTABLE, IMMUTABLE
    }

    private String type;
    private String name;
    private String columnName;
    private Integer length;
    private boolean nullable = true;
    private boolean updatable = true;
    private String foreignKey;
    private String index;
    private boolean version;
    private boolean unique;
    private boolean optimisticLock = true;
    private String defaultValue;
    private String uniqueConstraint = null;
    private boolean composite;
    private boolean inverseJoin = false;
    private NaturalId naturalId = NaturalId.NONE;
    private String columnDefinition;
    private Integer precision = 0;
    private Integer scale = 0;
    private Map<String, String> typeParams = new LinkedHashMap<>();

    public JpaColumn(final String type, final String name) {
        this.type = type;
        this.name = name;
    }

    public String getType(final boolean returnType) {
        if (!typeParams.isEmpty() && returnType) {
            if (typeParams.containsKey("enumClass")) {
                return typeParams.get("enumClass");
            }
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

    public void addTypeParam(final String key, final String value) {
        typeParams.put(key, value);
    }
}
