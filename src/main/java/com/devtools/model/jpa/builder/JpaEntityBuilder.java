package com.devtools.model.jpa.builder;

import com.devtools.model.jpa.JpaEntity;
import com.devtools.model.jpa.JpaColumn;
import com.devtools.model.jpa.JpaRelationship;
import com.devtools.model.jpa.JpaNamedQuery;

public class JpaEntityBuilder {
    
    private final JpaEntity entity;

    public JpaEntityBuilder() {
        this.entity = new JpaEntity();
    }

    public JpaEntityBuilder name(String name) {
        entity.setName(name);
        return this;
    }

    public JpaEntityBuilder table(String table) {
        entity.setTable(table);
        return this;
    }

    public JpaEntityBuilder defaultCascade(String defaultCascade) {
        entity.setDefaultCascade(defaultCascade);
        return this;
    }

    public JpaEntityBuilder parentClass(String parentClass) {
        entity.setParentClass(parentClass);
        return this;
    }

    public JpaEntityBuilder addColumn(JpaColumn column) {
        entity.addColumn(column);
        return this;
    }

    public JpaEntityBuilder addRelationship(JpaRelationship relationship) {
        entity.addRelationship(relationship);
        return this;
    }

    public JpaEntityBuilder addNamedQuery(JpaNamedQuery namedQuery) {
        entity.addNamedQuery(namedQuery);
        return this;
    }

    public JpaEntityBuilder addAnnotation(String annotation) {
        entity.addAnnotation(annotation);
        return this;
    }

    public JpaEntity build() {
        return entity;
    }
}