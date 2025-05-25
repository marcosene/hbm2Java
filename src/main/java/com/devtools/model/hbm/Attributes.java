package com.devtools.model.hbm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Attributes {

    private Attributes() {}

    public static final String ATTR_DEFAULT_CASCADE = "default-cascade";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TABLE = "table";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_DYNAMIC_INSERT = "dynamic-insert";
    public static final String ATTR_DYNAMIC_UPDATE = "dynamic-update";
    public static final String ATTR_ABSTRACT = "abstract";
    public static final String ATTR_MUTABLE = "mutable";
    public static final String ATTR_EXTENDS = "extends";
    public static final String ATTR_DISCRIMINATOR_VALUE = "discriminator-value";
    public static final String ATTR_USAGE = "usage";
    public static final String ATTR_COLUMN = "column";
    public static final String ATTR_LENGTH = "length";
    public static final String ATTR_NOT_NULL = "not-null";
    public static final String ATTR_UNIQUE = "unique";
    public static final String ATTR_UNIQUE_KEY = "unique-key";
    public static final String ATTR_DEFAULT = "default";
    public static final String ATTR_SQL_TYPE = "sql-type";
    public static final String ATTR_PRECISION = "precision";
    public static final String ATTR_SCALE = "scale";
    public static final String ATTR_UPDATE = "update";
    public static final String ATTR_OPTIMISTIC_LOCK = "optimistic-lock";
    public static final String ATTR_LAZY = "lazy";
    public static final String ATTR_CASCADE = "cascade";
    public static final String ATTR_INVERSE = "inverse";
    public static final String ATTR_ACCESS = "access";
    public static final String ATTR_ORDER_BY = "order-by";
    public static final String ATTR_INDEX = "index";
    public static final String ATTR_FOREIGN_KEY = "foreign-key";
    public static final String ATTR_FETCH = "fetch";
    public static final String ATTR_CONSTRAINED = "constrained";
    public static final String ATTR_PROPERTY_REF = "property-ref";
    public static final String ATTR_UNSAVED_VALUE = "unsaved-value";

    public static final Map<String, List<String>> ATTRIBUTES;

    static {
        ATTRIBUTES = new HashMap<>();
        ATTRIBUTES.put(Tags.TAG_HIBERNATE_MAPPING, List.of(ATTR_DEFAULT_CASCADE));
        ATTRIBUTES.put(Tags.TAG_CLASS, List.of(ATTR_NAME, ATTR_TABLE, ATTR_DYNAMIC_INSERT, ATTR_DYNAMIC_UPDATE,
                ATTR_ABSTRACT, ATTR_MUTABLE, ATTR_DISCRIMINATOR_VALUE));
        ATTRIBUTES.put(Tags.TAG_SUBCLASS, List.of(ATTR_NAME, ATTR_TABLE, ATTR_DYNAMIC_INSERT, ATTR_DYNAMIC_UPDATE,
                ATTR_ABSTRACT, ATTR_MUTABLE, ATTR_DISCRIMINATOR_VALUE, ATTR_EXTENDS
        ));
        ATTRIBUTES.put(Tags.TAG_UNION_SUBCLASS, List.of(ATTR_NAME, ATTR_TABLE, ATTR_DYNAMIC_INSERT, ATTR_DYNAMIC_UPDATE,
                ATTR_ABSTRACT, ATTR_MUTABLE, ATTR_EXTENDS));
        ATTRIBUTES.put(Tags.TAG_CACHE, List.of(ATTR_USAGE));
        ATTRIBUTES.put(Tags.TAG_JOIN, List.of(ATTR_TABLE));
        ATTRIBUTES.put(Tags.TAG_PROPERTIES, List.of(ATTR_NAME, ATTR_UNIQUE));
        ATTRIBUTES.put(Tags.TAG_PROPERTY, List.of(ATTR_NAME, ATTR_TYPE, ATTR_COLUMN, ATTR_UPDATE, ATTR_LAZY,
                ATTR_LENGTH, ATTR_OPTIMISTIC_LOCK));
        ATTRIBUTES.put(Tags.TAG_COLUMN, List.of(ATTR_NAME, ATTR_LENGTH, ATTR_NOT_NULL, ATTR_INDEX, ATTR_UNIQUE,
                ATTR_DEFAULT, ATTR_UNIQUE_KEY, ATTR_SQL_TYPE, ATTR_PRECISION, ATTR_SCALE));
        ATTRIBUTES.put(Tags.TAG_TYPE, List.of(ATTR_NAME));
        ATTRIBUTES.put(Tags.TAG_PARAM, List.of(ATTR_NAME));
        ATTRIBUTES.put(Tags.TAG_DISCRIMINATOR, List.of(ATTR_TYPE));
        ATTRIBUTES.put(Tags.TAG_ID, List.of(ATTR_NAME, ATTR_COLUMN, ATTR_TYPE,
                ATTR_UNSAVED_VALUE // unsaved-vale="null" is default for JPA (there is no need to annotate it)
        ));
        ATTRIBUTES.put(Tags.TAG_GENERATOR, List.of(ATTR_CLASS));
        ATTRIBUTES.put(Tags.TAG_NATURAL_ID, List.of(ATTR_MUTABLE));

        final List<String> relationshipAttrs = List.of(ATTR_NAME, ATTR_CLASS, ATTR_LAZY, ATTR_CASCADE, ATTR_ACCESS,
                ATTR_INDEX, ATTR_UPDATE, ATTR_NOT_NULL, ATTR_FOREIGN_KEY, ATTR_UNIQUE, ATTR_COLUMN, ATTR_CONSTRAINED,
                ATTR_PROPERTY_REF, ATTR_FETCH
        );
        ATTRIBUTES.put(Tags.TAG_MANY_TO_ONE, relationshipAttrs);
        ATTRIBUTES.put(Tags.TAG_ONE_TO_ONE, relationshipAttrs);
        ATTRIBUTES.put(Tags.TAG_ONE_TO_MANY, relationshipAttrs);
        ATTRIBUTES.put(Tags.TAG_MANY_TO_MANY, relationshipAttrs);

        ATTRIBUTES.put(Tags.TAG_VERSION, List.of(ATTR_NAME, ATTR_TYPE));

        final List<String> collectionAttrs = List.of(ATTR_NAME, ATTR_TABLE, ATTR_INVERSE, ATTR_LAZY, ATTR_CASCADE,
                ATTR_ORDER_BY, ATTR_FETCH
        );
        ATTRIBUTES.put(Tags.TAG_SET, collectionAttrs);
        ATTRIBUTES.put(Tags.TAG_LIST, collectionAttrs);
        ATTRIBUTES.put(Tags.TAG_BAG, collectionAttrs);
        ATTRIBUTES.put(Tags.TAG_MAP, collectionAttrs);

        ATTRIBUTES.put(Tags.TAG_KEY, List.of(ATTR_COLUMN, ATTR_FOREIGN_KEY));
        ATTRIBUTES.put(Tags.TAG_MAP_KEY, List.of(ATTR_TYPE, ATTR_COLUMN));
        ATTRIBUTES.put(Tags.TAG_COMPOSITE_MAP_KEY, List.of(ATTR_CLASS));
        ATTRIBUTES.put(Tags.TAG_KEY_PROPERTY, List.of(ATTR_NAME, ATTR_COLUMN, ATTR_TYPE));
        ATTRIBUTES.put(Tags.TAG_LIST_INDEX, List.of(ATTR_COLUMN));
        ATTRIBUTES.put(Tags.TAG_COMPONENT, List.of(ATTR_NAME, ATTR_CLASS));
        ATTRIBUTES.put(Tags.TAG_QUERY, List.of(ATTR_NAME));
        ATTRIBUTES.put(Tags.TAG_SQL_QUERY, List.of(ATTR_NAME));
        ATTRIBUTES.put(Tags.TAG_RETURN_SCALAR, List.of(ATTR_COLUMN, ATTR_TYPE));
    }
}
