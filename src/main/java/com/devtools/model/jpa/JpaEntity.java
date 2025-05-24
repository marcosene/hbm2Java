package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class JpaEntity extends JpaAnnotation {

    private String defaultCascade;
    private String table;
    private String parentClass;
    private String inheritance;
    private JpaDiscriminator discriminator;
    private boolean dynamicInsert = false;
    private boolean dynamicUpdate = false;
    private boolean abstractClass = false;
    private boolean immutable = false;
    private boolean embeddable = false;
    private boolean secondTable = false;
    private JpaColumn secondTableKeys;
    private String cacheUsage;

    private JpaPrimaryKey primaryKey;
    private final List<JpaColumn> columns = new ArrayList<>();
    private final List<JpaRelationship> relationships = new ArrayList<>();
    private final List<JpaEntity> embeddedFields = new ArrayList<>();
    private final List<JpaNamedQuery> namedQueries = new ArrayList<>();

    public void setDefaultCascade(final String defaultCascade) {
        if (StringUtils.isNotBlank(defaultCascade)) {
            this.defaultCascade = defaultCascade;
        }
    }

    public String getName() {
        if (name != null) {
            return name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : name;
        }
        return null;
    }

    public void setName(final String className) {
        if (StringUtils.isNotBlank(className)) {
            this.name = className;
        }
    }

    public void setTable(final String table) {
        if (StringUtils.isNotBlank(table)) {
            this.table = table;
        }
    }

    public String getFullParentClass() {
        return parentClass;
    }

    public String getParentClass() {
        if (parentClass != null) {
            return parentClass.contains(".") ? parentClass.substring(parentClass.lastIndexOf(".") + 1) : parentClass;
        }
        return null;
    }

    public void setParentClass(final String parentClass) {
        if (StringUtils.isNotBlank(parentClass)) {
            this.parentClass = parentClass;
        }
    }

    public JpaDiscriminator getDiscriminator(final boolean create) {
        if (discriminator == null && create) {
            discriminator = new JpaDiscriminator();
        }
        return discriminator;
    }

    public void setDynamicInsert(final String dynamicInsert) {
        this.dynamicInsert = StringUtils.isNotBlank(dynamicInsert) && Boolean.parseBoolean(dynamicInsert);
    }

    public void setDynamicUpdate(final String dynamicUpdate) {
        this.dynamicUpdate = StringUtils.isNotBlank(dynamicUpdate) && Boolean.parseBoolean(dynamicUpdate);
    }

    public void setAbstractClass(final String abstractClass) {
        this.abstractClass = StringUtils.isNotBlank(abstractClass) && Boolean.parseBoolean(abstractClass);
    }

    public void setMutable(final String mutable) {
        if (StringUtils.isNotBlank(mutable)) {
            this.immutable = !Boolean.parseBoolean(mutable);
        }
    }

    public void addColumn(final JpaColumn column) {
        columns.add(column);
    }

    public void addRelationship(final JpaRelationship relationship) {
        relationships.add(relationship);
    }

    public void addEmbeddedField(final JpaEntity embeddedField) {
        embeddedFields.add(embeddedField);
    }

    public void addNamedQuery(final JpaNamedQuery namedQuery) {
        namedQueries.add(namedQuery);
    }

    public String getPackageName() {
        // Extract the package name from the full class name
        if (name != null && name.contains(".")) {
            return name.substring(0, name.lastIndexOf("."));
        }
        // Return a default package if not defined (or throw an exception if required)
        return "";
    }
}
