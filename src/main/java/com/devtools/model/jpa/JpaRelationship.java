package com.devtools.model.jpa;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class JpaRelationship extends JpaAnnotation {

    public enum Type {
        OneToMany, OneToOne, ManyToOne, ManyToMany
    }

    private Type relationshipType;
    private String collectionType;
    private String fetch;
    private String cascade;
    private boolean inverse = false;
    private String orderColumn;
    private String access;
    private String table;
    private boolean optional = true;
    private String mappedBy;
    private List<JpaColumn> referencedColumns = new ArrayList<>();

    public void setCollectionType(final String collectionType) {
        this.collectionType = StringUtils.isNotBlank(collectionType) ? collectionType : null;
    }

    public void setFetch(final String fetch) {
        this.fetch = StringUtils.isNotBlank(fetch) ? fetch : null;
    }

    public void setCascade(final String cascade, final String defaultCascade) {
        this.cascade = StringUtils.isNotBlank(cascade) ? cascade :
                StringUtils.isNotBlank(defaultCascade) && !"none".equals(defaultCascade) ? defaultCascade : null;
    }

    public void setOrderColumn(final String orderColumn) {
        this.orderColumn = StringUtils.isNotBlank(orderColumn) ? orderColumn : null;
    }

    public void setAccess(final String access) {
        this.access = StringUtils.isNotBlank(access) ? access : null;
    }

    public void setTable(final String table) {
        this.table = StringUtils.isNotBlank(table) ? table : null;
    }

    public void setMappedBy(final String mappedBy) {
        this.mappedBy = StringUtils.isNotBlank(mappedBy) ? mappedBy : null;
    }

    public void addReferencedColumn(final JpaColumn referencedColumn) {
        referencedColumns.add(referencedColumn);
    }
}
