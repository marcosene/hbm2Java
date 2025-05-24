package com.devtools.model.jpa;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import com.devtools.model.jpa.builder.JpaColumnBuilder;

@Getter
@Setter
@NoArgsConstructor
public class JpaColumn extends JpaAnnotation {

    public enum NaturalId {
        NONE, MUTABLE, IMMUTABLE
    }

    private String columnName;
    private Integer length = JpaDefaults.DEFAULT_COLUMN_LENGTH;
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
    private Integer precision = JpaDefaults.DEFAULT_COLUMN_PRECISION;
    private Integer scale = JpaDefaults.DEFAULT_COLUMN_SCALE;

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

    public static JpaColumnBuilder builder() {
        return new JpaColumnBuilder();
    }
}
