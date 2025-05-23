package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.devtools.utils.Utils;

@Getter
@Setter
public abstract class JpaAnnotation {

    protected String name;
    protected final List<String> annotations = new ArrayList<>();
    protected final Set<String> imports = new TreeSet<>();
    protected volatile boolean processed = false;

    public void addAnnotation(final String annotation) {
        imports.addAll(Utils.extractFullyQualifiedClassNames(annotation));
        annotations.add(Utils.simplifyClassNames(annotation));
    }
}
