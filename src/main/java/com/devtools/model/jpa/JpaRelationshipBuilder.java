package com.devtools.model.jpa;

public class JpaRelationshipBuilder {
    
    private final JpaRelationship relationship;

    public JpaRelationshipBuilder() {
        this.relationship = new JpaRelationship();
    }

    public JpaRelationshipBuilder name(final String name) {
        relationship.setName(name);
        return this;
    }

    public JpaRelationshipBuilder type(final String type) {
        relationship.setType(type);
        return this;
    }

    public JpaRelationshipBuilder relationshipType(final JpaRelationship.Type relationshipType) {
        relationship.setRelationshipType(relationshipType);
        return this;
    }

    public JpaRelationshipBuilder collectionType(final String collectionType) {
        relationship.setCollectionType(collectionType);
        return this;
    }

    public JpaRelationshipBuilder fetch(final String fetch) {
        relationship.setFetch(fetch);
        return this;
    }

    public JpaRelationshipBuilder cascade(final String cascade, final String defaultCascade) {
        relationship.setCascade(cascade, defaultCascade);
        return this;
    }

    public JpaRelationshipBuilder cascade(final String cascade) {
        relationship.setCascade(cascade, null);
        return this;
    }

    public JpaRelationshipBuilder inverse(final boolean inverse) {
        relationship.setInverse(inverse);
        return this;
    }

    public JpaRelationshipBuilder orderColumn(final String orderColumn) {
        relationship.setOrderColumn(orderColumn);
        return this;
    }

    public JpaRelationshipBuilder access(final String access) {
        relationship.setAccess(access);
        return this;
    }

    public JpaRelationshipBuilder table(final String table) {
        relationship.setTable(table);
        return this;
    }

    public JpaRelationshipBuilder optional(final boolean optional) {
        relationship.setOptional(optional);
        return this;
    }

    public JpaRelationshipBuilder mappedBy(final String mappedBy) {
        relationship.setMappedBy(mappedBy);
        return this;
    }

    public JpaRelationshipBuilder addReferencedColumn(final JpaColumn referencedColumn) {
        relationship.addReferencedColumn(referencedColumn);
        return this;
    }

    public JpaRelationship build() {
        return relationship;
    }
}