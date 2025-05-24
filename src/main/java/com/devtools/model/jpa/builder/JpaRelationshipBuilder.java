package com.devtools.model.jpa.builder;

import com.devtools.model.jpa.JpaRelationship;

public class JpaRelationshipBuilder {
    
    private final JpaRelationship relationship;

    public JpaRelationshipBuilder() {
        this.relationship = new JpaRelationship();
    }

    public JpaRelationshipBuilder collectionType(String collectionType) {
        relationship.setCollectionType(collectionType);
        return this;
    }

    public JpaRelationshipBuilder fetch(String fetch) {
        relationship.setFetch(fetch);
        return this;
    }

    public JpaRelationshipBuilder cascade(String cascade, String defaultCascade) {
        relationship.setCascade(cascade, defaultCascade);
        return this;
    }

    public JpaRelationshipBuilder orderColumn(String orderColumn) {
        relationship.setOrderColumn(orderColumn);
        return this;
    }

    public JpaRelationshipBuilder access(String access) {
        relationship.setAccess(access);
        return this;
    }

    public JpaRelationshipBuilder table(String table) {
        relationship.setTable(table);
        return this;
    }

    public JpaRelationshipBuilder mappedBy(String mappedBy) {
        relationship.setMappedBy(mappedBy);
        return this;
    }

    public JpaRelationshipBuilder optional(boolean optional) {
        relationship.setOptional(optional);
        return this;
    }

    public JpaRelationship build() {
        return relationship;
    }
}