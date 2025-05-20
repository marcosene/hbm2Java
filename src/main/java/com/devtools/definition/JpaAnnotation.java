package com.devtools.definition;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class JpaAnnotation {

    private final List<String> annotations = new ArrayList<>();

    public void addAnnotation(final String annotation) {
        annotations.add(annotation);
    }
}
