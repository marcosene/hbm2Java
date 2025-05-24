package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public abstract class JpaAnnotation {

    protected String type;
    protected String name;

    private Map<String, String> typeParams = new LinkedHashMap<>();
    private final List<String> annotations = new ArrayList<>();
    private final Set<String> imports = new TreeSet<>();

    private volatile boolean processed = false;

    public String getReturnType() {
        if (!typeParams.isEmpty() && typeParams.containsKey("enumClass")) {
            return typeParams.get("enumClass");
        }
        if (type != null && type.endsWith("Type")) {
            final String normalizedReturnType = type.endsWith("UserType") ?
                    type.replace("UserType", "") : type.replace("Type", "");
            return normalizedReturnType.replace(".usertypes", "");
        } else {
            return type;
        }
    }

    public void setType(final String type) {
        this.type = StringUtils.isNotBlank(type) ? type : null;
    }

    public void setName(final String name) {
        this.name = StringUtils.isNotBlank(name) ? name : null;
    }

    public void addTypeParam(final String key, final String value) {
        typeParams.put(key, value);
    }

    public void addAnnotation(final String annotation) {
        imports.addAll(com.devtools.utils.StringUtils.extractFullyQualifiedClassNames(annotation));
        annotations.add(com.devtools.utils.StringUtils.removePackagesFromText(annotation));
    }
}
