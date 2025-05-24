package com.devtools.model.jpa;

import org.apache.commons.lang3.StringUtils;

public class JpaColumnBuilder {
    
    private final JpaColumn column;

    public JpaColumnBuilder() {
        this.column = new JpaColumn();
    }

    public JpaColumnBuilder type(final String type) {
        column.setType(type);
        return this;
    }

    public JpaColumnBuilder name(final String name) {
        column.setName(name);
        return this;
    }

    public JpaColumnBuilder columnName(final String columnName) {
        column.setColumnName(columnName);
        return this;
    }

    public JpaColumnBuilder length(final Integer length) {
        if (length != null) {
            column.setLength(length.toString());
        }
        return this;
    }

    public JpaColumnBuilder nullable(final boolean nullable) {
        column.setNullable(nullable);
        return this;
    }

    public JpaColumnBuilder updatable(final boolean updatable) {
        column.setUpdatable(updatable);
        return this;
    }

    public JpaColumnBuilder foreignKey(final String foreignKey) {
        column.setForeignKey(foreignKey);
        return this;
    }

    public JpaColumnBuilder index(final String index) {
        column.setIndex(index);
        return this;
    }

    public JpaColumnBuilder version(final boolean version) {
        column.setVersion(version);
        return this;
    }

    public JpaColumnBuilder unique(final boolean unique) {
        column.setUnique(unique);
        return this;
    }

    public JpaColumnBuilder lazy(final boolean lazy) {
        column.setLazy(lazy);
        return this;
    }

    public JpaColumnBuilder optimisticLock(final boolean optimisticLock) {
        column.setOptimisticLock(optimisticLock);
        return this;
    }

    public JpaColumnBuilder defaultValue(final String defaultValue) {
        column.setDefaultValue(defaultValue);
        return this;
    }

    public JpaColumnBuilder uniqueConstraint(final String uniqueConstraint) {
        column.setUniqueConstraint(uniqueConstraint);
        return this;
    }

    public JpaColumnBuilder composite(final boolean composite) {
        column.setComposite(composite);
        return this;
    }

    public JpaColumnBuilder embedded(final boolean embedded) {
        column.setEmbedded(embedded);
        return this;
    }

    public JpaColumnBuilder inverseJoin(final boolean inverseJoin) {
        column.setInverseJoin(inverseJoin);
        return this;
    }

    public JpaColumnBuilder naturalId(final JpaColumn.NaturalId naturalId) {
        column.setNaturalId(naturalId);
        return this;
    }

    public JpaColumnBuilder columnDefinition(final String columnDefinition) {
        column.setColumnDefinition(columnDefinition);
        return this;
    }

    public JpaColumnBuilder precision(final Integer precision) {
        if (precision != null) {
            column.setPrecision(precision);
        }
        return this;
    }

    public JpaColumnBuilder scale(final Integer scale) {
        if (scale != null) {
            column.setScale(scale);
        }
        return this;
    }

    public JpaColumn build() {
        return column;
    }
}