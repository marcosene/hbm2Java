package com.devtools.model.jpa;

import static org.apache.commons.lang3.StringUtils.trim;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class JpaColumn extends JpaAbstract {

    public static final int DEFAULT_COLUMN_LENGTH = 255;
    public static final int DEFAULT_COLUMN_PRECISION = 0;
    public static final int DEFAULT_COLUMN_SCALE = 0;

    public enum NaturalId {
        NONE, MUTABLE, IMMUTABLE
    }

    private String columnName;
    private String table;
    private Integer length = DEFAULT_COLUMN_LENGTH;
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
    private boolean embedded = false;
    private boolean inverseJoin = false;
    private NaturalId naturalId = NaturalId.NONE;
    private String columnDefinition;
    private Integer precision = DEFAULT_COLUMN_PRECISION;
    private Integer scale = DEFAULT_COLUMN_SCALE;

    public void setColumnName(final String columnName) {
        if (StringUtils.isNotBlank(columnName)) {
            this.columnName = trim(columnName);
        }
    }

    public void setTable(final String table) {
        if (StringUtils.isNotBlank(table)) {
            this.table = trim(table);
        }
    }

    public void setLength(final String length) {
        if (StringUtils.isNotBlank(length)) {
            this.length = Integer.parseInt(trim(length));
        }
    }

    public void setForeignKey(final String foreignKey) {
        if (StringUtils.isNotBlank(foreignKey)) {
            this.foreignKey = trim(foreignKey);
        }
    }

    public void setIndex(final String index) {
        if (StringUtils.isNotBlank(index)) {
            this.index = trim(index);
        }
    }

    public void setDefaultValue(final String defaultValue) {
        if (StringUtils.isNotBlank(defaultValue)) {
            this.defaultValue = trim(defaultValue);
        }
    }

    public void setUniqueConstraint(final String uniqueConstraint) {
        if (StringUtils.isNotBlank(uniqueConstraint)) {
            this.uniqueConstraint = trim(uniqueConstraint);
        }
    }

    public void setColumnDefinition(final String columnDefinition) {
        if (StringUtils.isNotBlank(columnDefinition)) {
            this.columnDefinition = trim(columnDefinition);
        }
    }
}
