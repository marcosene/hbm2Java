package com.devtools.definition;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.devtools.Utils;

@Getter
public abstract class JpaAnnotation {

    private final List<String> annotations = new ArrayList<>();
    private final Set<String> imports = new TreeSet<>();

    public void addAnnotation(final String annotation) {
        imports.addAll(Utils.extractFullyQualifiedClassNames(annotation));
        annotations.add(Utils.simplifyClassNames(annotation));
    }
}
