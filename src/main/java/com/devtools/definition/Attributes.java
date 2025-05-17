package com.devtools.definition;

import java.util.Arrays;
import java.util.List;

public final class Attributes {

    private Attributes() {}

    public static final String ATTR_HIBERNATE_MAPPING_DEFAULT_CASCADE = "default-cascade";

    public static final String ATTR_ENTITY_NAME = "name";
    public static final String ATTR_ENTITY_TABLE = "table";
    public static final String ATTR_ENTITY_DYNAMIC_INSERT = "dynamic-insert";
    public static final String ATTR_ENTITY_DYNAMIC_UPDATE = "dynamic-update";
    public static final String ATTR_ENTITY_ABSTRACT = "abstract";
    public static final String ATTR_ENTITY_MUTABLE = "mutable";
    public static final String ATTR_ENTITY_EXTENDS = "extends";
    public static final String ATTR_ENTITY_DISCRIMINATOR_VALUE = "discriminator-value";

    public static final String ATTR_CACHE_USAGE = "usage";

    public static final String ATTR_VERSION_NAME = "name";
    public static final String ATTR_VERSION_TYPE = "type";

    public static final String ATTR_DISCRIMINATOR_COLUMN = "column";
    public static final String ATTR_DISCRIMINATOR_NAME = "name";
    public static final String ATTR_DISCRIMINATOR_LENGTH = "length";

    public static final String ATTR_ID_NAME = "name";
    public static final String ATTR_ID_TYPE = "type";

    public static final String ATTR_NATURAL_ID_MUTABLE = "mutable";

    public static final String ATTR_GENERATOR_CLASS = "class";
    public static final String ATTR_PARAM_NAME = "name";

    public static final String ATTR_COLUMN_NAME = "name";
    public static final String ATTR_COLUMN_LENGTH = "length";
    public static final String ATTR_COLUMN_NOT_NULL = "not-null";
    public static final String ATTR_COLUMN_INDEX = "index";
    public static final String ATTR_COLUMN_UNIQUE = "unique";
    public static final String ATTR_COLUMN_UNIQUE_KEY = "unique-key";
    public static final String ATTR_COLUMN_DEFAULT = "default";
    public static final String ATTR_COLUMN_SQL_TYPE = "sql-type";
    public static final String ATTR_COLUMN_PRECISION = "precision";
    public static final String ATTR_COLUMN_SCALE = "scale";

    public static final String ATTR_COMPONENT_NAME = "name";
    public static final String ATTR_COMPONENT_CLASS = "class";

    public static final String ATTR_PROPERTIES_NAME = "name";

    public static final String ATTR_PROPERTY_NAME = "name";
    public static final String ATTR_PROPERTY_TYPE = "type";
    public static final String ATTR_PROPERTY_UPDATE = "update";
    public static final String ATTR_PROPERTY_OPTIMISTIC_LOCK = "optimistic-lock";

    public static final String ATTR_RELATIONSHIP_NAME = "name";
    public static final String ATTR_RELATIONSHIP_TABLE = "table";
    public static final String ATTR_RELATIONSHIP_CLASS = "class";
    public static final String ATTR_RELATIONSHIP_LAZY = "lazy";
    public static final String ATTR_RELATIONSHIP_FETCH = "fetch";
    public static final String ATTR_RELATIONSHIP_CASCADE = "cascade";
    public static final String ATTR_RELATIONSHIP_INVERSE = "inverse";
    public static final String ATTR_RELATIONSHIP_ACCESS = "access";
    public static final String ATTR_RELATIONSHIP_ORDER_BY = "order-by";
    public static final String ATTR_RELATIONSHIP_INDEX = "index";

    public static final String ATTR_LIST_INDEX_COLUMN = "column";

    public static final String ATTR_KEY_COLUMN = "column";
    public static final String ATTR_KEY_FOREIGN_KEY = "foreign-key";

    public static final String ATTR_MAP_KEY_COLUMN = "column";
    public static final String ATTR_MAP_KEY_TYPE = "type";

    public static final String ATTR_COMPOSITE_MAP_KEY_CLASS = "class";

    public static final String ATTR_KEY_PROPERTY_NAME = "name";
    public static final String ATTR_KEY_PROPERTY_COLUMN = "column";

    public static final String ATTR_TYPE_NAME = "name";

    public static final String ATTR_QUERY_NAME = "name";
    public static final String ATTR_SQL_QUERY_NAME = "name";
    public static final String ATTR_RETURN_SCALAR_COLUMN = "column";
    public static final String ATTR_RETURN_SCALAR_TYPE = "type";

    // Ignored attributes
    public static final String ATTR_ID_UNSAVED_VALUE = "unsaved-value";

    public static final List<String> ATTRIBUTES = Arrays.asList(
            ATTR_HIBERNATE_MAPPING_DEFAULT_CASCADE,
            ATTR_ENTITY_NAME,
            ATTR_ENTITY_TABLE,
            ATTR_ENTITY_DYNAMIC_INSERT,
            ATTR_ENTITY_DYNAMIC_UPDATE,
            ATTR_ENTITY_ABSTRACT,
            ATTR_ENTITY_MUTABLE,
            ATTR_ENTITY_EXTENDS,
            ATTR_ENTITY_DISCRIMINATOR_VALUE,
            ATTR_CACHE_USAGE,
            ATTR_VERSION_NAME,
            ATTR_VERSION_TYPE,
            ATTR_DISCRIMINATOR_COLUMN,
            ATTR_DISCRIMINATOR_NAME,
            ATTR_DISCRIMINATOR_LENGTH,
            ATTR_ID_NAME,
            ATTR_ID_TYPE,
            ATTR_NATURAL_ID_MUTABLE,
            ATTR_GENERATOR_CLASS,
            ATTR_PARAM_NAME,
            ATTR_COLUMN_NAME,
            ATTR_COLUMN_LENGTH,
            ATTR_COLUMN_NOT_NULL,
            ATTR_COLUMN_INDEX,
            ATTR_COLUMN_UNIQUE,
            ATTR_COLUMN_UNIQUE_KEY,
            ATTR_COLUMN_DEFAULT,
            ATTR_COLUMN_SQL_TYPE,
            ATTR_COLUMN_PRECISION,
            ATTR_COLUMN_SCALE,
            ATTR_COMPONENT_NAME,
            ATTR_COMPONENT_CLASS,
            ATTR_PROPERTIES_NAME,
            ATTR_PROPERTY_NAME,
            ATTR_PROPERTY_TYPE,
            ATTR_PROPERTY_UPDATE,
            ATTR_PROPERTY_OPTIMISTIC_LOCK,
            ATTR_RELATIONSHIP_NAME,
            ATTR_RELATIONSHIP_TABLE,
            ATTR_RELATIONSHIP_CLASS,
            ATTR_RELATIONSHIP_LAZY,
            ATTR_RELATIONSHIP_CASCADE,
            ATTR_RELATIONSHIP_INVERSE,
            ATTR_RELATIONSHIP_ACCESS,
            ATTR_RELATIONSHIP_ORDER_BY,
            ATTR_RELATIONSHIP_INDEX,
            ATTR_LIST_INDEX_COLUMN,
            ATTR_KEY_COLUMN,
            ATTR_KEY_FOREIGN_KEY,
            ATTR_MAP_KEY_COLUMN,
            ATTR_MAP_KEY_TYPE,
            ATTR_COMPOSITE_MAP_KEY_CLASS,
            ATTR_KEY_PROPERTY_NAME,
            ATTR_KEY_PROPERTY_COLUMN,
            ATTR_TYPE_NAME,
            ATTR_SQL_QUERY_NAME,
            ATTR_RETURN_SCALAR_COLUMN,
            ATTR_RETURN_SCALAR_TYPE,

            // Ignored attributes
            ATTR_ID_UNSAVED_VALUE,
            ATTR_RELATIONSHIP_FETCH // we rely on ATTR_RELATIONSHIP_LAZY
    );
}
