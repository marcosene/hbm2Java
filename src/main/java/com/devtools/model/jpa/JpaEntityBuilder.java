package com.devtools.model.jpa;

public class JpaEntityBuilder {
    
    private final JpaEntity entity;

    public JpaEntityBuilder() {
        this.entity = new JpaEntity();
    }

    public JpaEntityBuilder name(final String name) {
        entity.setName(name);
        return this;
    }

    public JpaEntityBuilder type(final String type) {
        entity.setType(type);
        return this;
    }

    public JpaEntityBuilder defaultCascade(final String defaultCascade) {
        entity.setDefaultCascade(defaultCascade);
        return this;
    }

    public JpaEntityBuilder table(final String table) {
        entity.setTable(table);
        return this;
    }

    public JpaEntityBuilder parentClass(final String parentClass) {
        entity.setParentClass(parentClass);
        return this;
    }

    public JpaEntityBuilder inheritance(final String inheritance) {
        entity.setInheritance(inheritance);
        return this;
    }

    public JpaEntityBuilder discriminator(final JpaDiscriminator discriminator) {
        entity.setDiscriminator(discriminator);
        return this;
    }

    public JpaEntityBuilder dynamicInsert(final boolean dynamicInsert) {
        entity.setDynamicInsert(String.valueOf(dynamicInsert));
        return this;
    }

    public JpaEntityBuilder dynamicUpdate(final boolean dynamicUpdate) {
        entity.setDynamicUpdate(String.valueOf(dynamicUpdate));
        return this;
    }

    public JpaEntityBuilder abstractClass(final boolean abstractClass) {
        entity.setAbstractClass(String.valueOf(abstractClass));
        return this;
    }

    public JpaEntityBuilder mutable(final boolean mutable) {
        entity.setMutable(String.valueOf(mutable));
        return this;
    }

    public JpaEntityBuilder embeddable(final boolean embeddable) {
        entity.setEmbeddable(embeddable);
        return this;
    }

    public JpaEntityBuilder secondTable(final boolean secondTable) {
        entity.setSecondTable(secondTable);
        return this;
    }

    public JpaEntityBuilder secondTableKeys(final JpaColumn secondTableKeys) {
        entity.setSecondTableKeys(secondTableKeys);
        return this;
    }

    public JpaEntityBuilder cacheUsage(final String cacheUsage) {
        entity.setCacheUsage(cacheUsage);
        return this;
    }

    public JpaEntityBuilder primaryKey(final JpaPrimaryKey primaryKey) {
        entity.setPrimaryKey(primaryKey);
        return this;
    }

    public JpaEntityBuilder addColumn(final JpaColumn column) {
        entity.addColumn(column);
        return this;
    }

    public JpaEntityBuilder addRelationship(final JpaRelationship relationship) {
        entity.addRelationship(relationship);
        return this;
    }

    public JpaEntityBuilder addEmbeddedField(final JpaEntity embeddedField) {
        entity.addEmbeddedField(embeddedField);
        return this;
    }

    public JpaEntityBuilder addNamedQuery(final JpaNamedQuery namedQuery) {
        entity.addNamedQuery(namedQuery);
        return this;
    }

    public JpaEntity build() {
        return entity;
    }
}