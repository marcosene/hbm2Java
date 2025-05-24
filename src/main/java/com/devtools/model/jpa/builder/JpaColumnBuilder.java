package com.devtools.model.jpa.builder;

import org.apache.commons.lang3.StringUtils;
import com.devtools.model.jpa.JpaColumn;

public class JpaColumnBuilder {
    
    private final JpaColumn column;

    public JpaColumnBuilder() {
        this.column = new JpaColumn();
    }

    public JpaColumnBuilder columnName(String columnName) {
        column.setColumnName(columnName);
        return this;
    }

    public JpaColumnBuilder length(String length) {
        column.setLength(length);
        return this;
    }

    public JpaColumnBuilder nullable(boolean nullable) {
        column.setNullable(nullable);
        return this;
    }

    public JpaColumnBuilder unique(boolean unique) {
        column.setUnique(unique);
        return this;
    }

    public JpaColumnBuilder updatable(boolean updatable) {
        column.setUpdatable(updatable);
        return this;
    }

    public JpaColumnBuilder foreignKey(String foreignKey) {
        column.setForeignKey(foreignKey);
        return this;
    }

    public JpaColumnBuilder index(String index) {
        column.setIndex(index);
        return this;
    }

    public JpaColumnBuilder defaultValue(String defaultValue) {
        column.setDefaultValue(defaultValue);
        return this;
    }

    public JpaColumnBuilder uniqueConstraint(String uniqueConstraint) {
        column.setUniqueConstraint(uniqueConstraint);
        return this;
    }

    public JpaColumnBuilder columnDefinition(String columnDefinition) {
        column.setColumnDefinition(columnDefinition);
        return this;
    }

    public JpaColumn build() {
        return column;
    }
}