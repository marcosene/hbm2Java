package com.devtools.definition;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class JpaRelationship {

    private String name;
    private String targetEntity;
    private String type;
    private String fetch;
    private String cascade;
    private boolean inverse;
    private String collectionType;
    private String orderColumn;
    private String access;
    private String table;
    private List<JpaColumn> referencedColumns = new ArrayList<>();

    public void setFetch(final String fetch) {
        this.fetch = StringUtils.isNotBlank(fetch) ? fetch : null;
    }

    public void setCascade(final String cascade, final String defaultCascade) {
        this.cascade = StringUtils.isNotBlank(cascade) ? cascade :
                StringUtils.isNotBlank(defaultCascade) && !"none".equals(defaultCascade) ? defaultCascade : null;
    }

    public void setCollectionType(final String collectionType) {
        this.collectionType = StringUtils.isNotBlank(collectionType) ? collectionType : null;
    }

    public void setOrderColumn(final String orderColumn) {
        this.orderColumn = StringUtils.isNotBlank(orderColumn) ? orderColumn : null;
    }

    public void setAccess(final String access) {
        this.access = StringUtils.isNotBlank(access) ? access : null;
    }

    public void addReferencedColumn(final JpaColumn referencedColumn) {
        referencedColumns.add(referencedColumn);
    }

    public String getSimpleClass() {
        final String fullClassName = getTargetEntity();
        return fullClassName.contains(".")
                ? fullClassName.substring(fullClassName.lastIndexOf(".") + 1)
                : fullClassName;
    }
}
