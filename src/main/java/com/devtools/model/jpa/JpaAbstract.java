package com.devtools.model.jpa;

import static org.apache.commons.lang3.StringUtils.trim;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.devtools.utils.Utils;

@Getter
@Setter
public abstract class JpaAbstract {

    protected String type;
    protected String name;

    private Map<String, String> typeParams = new LinkedHashMap<>();
    private final List<String> annotations = new ArrayList<>();
    private final Set<String> imports = new TreeSet<>();

    private volatile boolean processed = false;

    public void setType(final String type) {
        if (StringUtils.isNotBlank(type)) {
            this.type = trim(type);
        }
    }

    public void setName(final String name) {
        if (StringUtils.isNotBlank(name)) {
            this.name = trim(name);
        }
    }

    public String getReturnType() {
        if (typeParams.containsKey("enumClass")) {
            return typeParams.get("enumClass");
        }
        if (type != null && type.endsWith("Type")) {
            final String returnType = type.endsWith("UserType") ?
                    type.replace("UserType", "") : type.replace("Type", "");
            // remove possible "usertypes" in the package name
            return returnType.replace(".usertypes", "");
        } else {
            return type;
        }
    }

    public void addTypeParam(final String key, final String value) {
        typeParams.put(trim(key), trim(value));
    }

    public void addAnnotation(final String annotation) {
        if (StringUtils.isNotBlank(annotation)) {
            imports.addAll(Utils.extractFullyQualifiedClassNames(annotation));
            annotations.add(Utils.removePackagesFromText(annotation));
        }
    }
}
