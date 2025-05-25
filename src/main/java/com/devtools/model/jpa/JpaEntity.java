package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.devtools.utils.ClassNameUtils;

@Getter
@Setter
public class JpaEntity extends JpaAbstract {

    private String defaultCascade;
    private String table;
    private String parentClass;
    private String inheritance;
    private JpaDiscriminator discriminator;
    private boolean dynamicInsert = false;
    private boolean dynamicUpdate = false;
    private boolean abstractClass = false;
    private boolean mutable = true;
    private boolean embeddable = false;
    private boolean secondTable = false;
    private JpaColumn secondTableKeys;
    private String cacheUsage;

    private JpaPrimaryKey primaryKey;
    private final List<JpaColumn> columns = new ArrayList<>();
    private final List<JpaCompositeColumn> compositeColumns = new ArrayList<>();
    private final List<JpaRelationship> relationships = new ArrayList<>();
    private final List<JpaEntity> embeddedEntities = new ArrayList<>();
    private final List<JpaNamedQuery> namedQueries = new ArrayList<>();

    public String getName() {
        return name != null ? name : type;
    }

    public String getType() {
        return type != null ? type : name;
    }

    public String getPackageName() {
        return ClassNameUtils.getPackageName(getType());
    }

    public String getSimpleName() {
        return ClassNameUtils.getSimpleClassName(getType());
    }

    public void setDefaultCascade(final String defaultCascade) {
        if (StringUtils.isNotBlank(defaultCascade)) {
            this.defaultCascade = defaultCascade;
        }
    }

    public void setTable(final String table) {
        if (StringUtils.isNotBlank(table)) {
            this.table = table;
        }
    }

    public String getSimpleParentClass() {
        return ClassNameUtils.getSimpleClassName(parentClass);
    }

    public void setParentClass(final String parentClass) {
        if (StringUtils.isNotBlank(parentClass)) {
            this.parentClass = parentClass;
        }
    }

    public JpaDiscriminator getDiscriminator() {
        if (discriminator == null) {
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
            this.mutable = Boolean.parseBoolean(mutable);
        }
    }

    public void setCacheUsage(final String cacheUsage) {
        if (StringUtils.isNotBlank(cacheUsage)) {
            this.cacheUsage = cacheUsage;
        }
    }

    public void addColumn(final JpaColumn column) {
        if (column != null && !columns.contains(column)) {
            columns.add(column);
        }
    }

    public void addCompositeColumn(final JpaCompositeColumn compositeColumn) {
        if (compositeColumn != null && !compositeColumns.contains(compositeColumn)) {
            compositeColumns.add(compositeColumn);
        }
    }

    public void addRelationship(final JpaRelationship relationship) {
        if (relationship != null && !relationships.contains(relationship)) {
            relationships.add(relationship);
        }
    }

    public void addEmbeddedEntity(final JpaEntity embeddedEntity) {
        if (embeddedEntity != null && !embeddedEntities.contains(embeddedEntity)) {
            embeddedEntities.add(embeddedEntity);
        }
    }

    public void addNamedQuery(final JpaNamedQuery namedQuery) {
        if (namedQuery != null && !namedQueries.contains(namedQuery)) {
            namedQueries.add(namedQuery);
        }
    }
}
