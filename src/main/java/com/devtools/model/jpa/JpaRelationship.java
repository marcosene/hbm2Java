package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class JpaRelationship extends JpaAbstract {

    public enum Type {
        OneToMany, OneToOne, ManyToOne, ManyToMany
    }

    private Type relationshipType;
    private String collectionType;
    private String fetch;
    private String cascade;
    private boolean inverse = false;
    private String orderBy;
    private String listIndex;
    private String access;
    private String table;
    private boolean optional = true;
    private String mappedBy;
    private String compositeMapKey;
    private List<JpaColumn> referencedColumns = new ArrayList<>();

    public void setCollectionType(final String collectionType) {
        if (StringUtils.isNotBlank(collectionType)) {
            this.collectionType = collectionType;
        }
    }

    public void setFetch(final String fetch) {
        if (StringUtils.isNotBlank(fetch)) {
            this.fetch = fetch;
        }
    }

    public void setCascade(final String cascade, final String defaultCascade) {
        this.cascade = StringUtils.isNotBlank(cascade) ? cascade :
                StringUtils.isNotBlank(defaultCascade) && !"none".equals(defaultCascade) ? defaultCascade : null;
    }

    public void setOrderBy(final String orderBy) {
        if (StringUtils.isNotBlank(orderBy)) {
            this.orderBy = orderBy;
        }
    }

    public void setListIndex(final String listIndex) {
        if (StringUtils.isNotBlank(listIndex)) {
            this.listIndex = listIndex;
        }
    }

    public void setAccess(final String access) {
        if (StringUtils.isNotBlank(access)) {
            this.access = access;
        }
    }

    public void setTable(final String table) {
        if (StringUtils.isNotBlank(table)) {
            this.table = table;
        }
    }

    public void setMappedBy(final String mappedBy) {
        if (StringUtils.isNotBlank(mappedBy)) {
            this.mappedBy = mappedBy;
        }
    }

    public void setCompositeMapKey(final String compositeMapKey) {
        if (StringUtils.isNotBlank(compositeMapKey)) {
            this.compositeMapKey = compositeMapKey;
        }
    }

    public void addReferencedColumn(final JpaColumn referencedColumn) {
        if (referencedColumn != null && !referencedColumns.contains(referencedColumn)) {
            referencedColumns.add(referencedColumn);
        }
    }
}
