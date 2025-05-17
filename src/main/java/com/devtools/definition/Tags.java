package com.devtools.definition;

import java.util.List;

public final class Tags {

    private Tags() {}

    public static final String TAG_HIBERNATE_MAPPING = "hibernate-mapping";
    public static final String TAG_CLASS = "class";
    public static final String TAG_SUBCLASS = "subclass";
    public static final String TAG_UNION_SUBCLASS = "union-subclass";
    public static final String TAG_JOIN = "join";
    public static final String TAG_CACHE = "cache";

    public static final String TAG_VERSION  = "version";
    public static final String TAG_DISCRIMINATOR = "discriminator";
    public static final String TAG_ID = "id";
    public static final String TAG_NATURAL_ID = "natural-id";
    public static final String TAG_GENERATOR = "generator";
    public static final String TAG_PARAM = "param";

    public static final String TAG_COLUMN = "column";
    public static final String TAG_COMPONENT = "component";
    public static final String TAG_PROPERTIES = "properties";
    public static final String TAG_PROPERTY = "property";

    public static final String TAG_MANY_TO_ONE = "many-to-one";
    public static final String TAG_ONE_TO_ONE = "one-to-one";
    public static final String TAG_ONE_TO_MANY = "one-to-many";
    public static final String TAG_MANY_TO_MANY = "many-to-many";

    public static final String TAG_SET = "set";
    public static final String TAG_LIST = "list";
    public static final String TAG_BAG = "bag";
    public static final String TAG_MAP = "map";

    public static final String TAG_LIST_INDEX = "list-index";
    public static final String TAG_KEY = "key";
    public static final String TAG_MAP_KEY = "map-key";
    public static final String TAG_COMPOSITE_MAP_KEY = "composite-map-key";
    public static final String TAG_KEY_PROPERTY = "key-property";
    public static final String TAG_TYPE = "type";

    public static final String TAG_QUERY = "query";
    public static final String TAG_SQL_QUERY = "sql-query";
    public static final String TAG_RETURN_SCALAR = "return-scalar";

    // Ignored tags
    public static final String TAG_COMMENT = "comment";

    public static final List<String> TAGS = List.of(
            TAG_HIBERNATE_MAPPING,
            TAG_CLASS,
            TAG_SUBCLASS,
            TAG_UNION_SUBCLASS,
            TAG_JOIN,
            TAG_CACHE,

            TAG_VERSION,
            TAG_DISCRIMINATOR,
            TAG_ID,
            TAG_NATURAL_ID,
            TAG_GENERATOR,
            TAG_PARAM,

            TAG_COLUMN,
            TAG_COMPONENT,
            TAG_PROPERTIES,
            TAG_PROPERTY,

            TAG_MANY_TO_ONE,
            TAG_ONE_TO_ONE,
            TAG_ONE_TO_MANY,
            TAG_MANY_TO_MANY,

            TAG_SET,
            TAG_LIST,
            TAG_BAG,
            TAG_MAP,

            TAG_LIST_INDEX,
            TAG_KEY,
            TAG_MAP_KEY,
            TAG_COMPOSITE_MAP_KEY,
            TAG_KEY_PROPERTY,
            TAG_TYPE,

            TAG_QUERY,
            TAG_SQL_QUERY,
            TAG_RETURN_SCALAR,

            // Ignored tags
            TAG_COMMENT
    );
}
