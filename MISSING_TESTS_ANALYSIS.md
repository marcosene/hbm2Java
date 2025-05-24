# Missing Tests Analysis

Based on the `ATTRIBUTES` map in `Attributes.java`, here are the tags and their attributes that need test coverage:

## Tags with Missing Tests

### 1. TAG_JOIN
- **Attributes:** `ATTR_TABLE`
- **Status:** ❌ Missing
- **HBM:** `<join table="secondary_table">`
- **JPA:** `@SecondaryTable`

### 2. TAG_PROPERTIES  
- **Attributes:** `ATTR_NAME`, `ATTR_UNIQUE`
- **Status:** ❌ Missing
- **HBM:** `<properties name="nameAndEmail" unique="true">`
- **JPA:** `@AttributeOverrides` or custom grouping

### 3. TAG_TYPE
- **Attributes:** `ATTR_NAME`
- **Status:** ❌ Missing (only tested within property)
- **HBM:** `<type name="custom_type"/>`
- **JPA:** `@Type`

### 4. TAG_PARAM
- **Attributes:** `ATTR_NAME`
- **Status:** ❌ Missing (only tested within generator)
- **HBM:** `<param name="sequence">seq_name</param>`
- **JPA:** Generator parameters

### 5. TAG_DISCRIMINATOR
- **Attributes:** `ATTR_TYPE`
- **Status:** ❌ Missing (basic test exists but not all attributes)
- **HBM:** `<discriminator type="string" column="type_col"/>`
- **JPA:** `@DiscriminatorColumn`

### 6. TAG_MAP_KEY
- **Attributes:** `ATTR_TYPE`, `ATTR_COLUMN`
- **Status:** ❌ Missing
- **HBM:** `<map-key type="string" column="map_key_col"/>`
- **JPA:** `@MapKeyColumn`

### 7. TAG_COMPOSITE_MAP_KEY
- **Attributes:** `ATTR_CLASS`
- **Status:** ❌ Missing
- **HBM:** `<composite-map-key class="KeyClass"/>`
- **JPA:** `@MapKeyClass`

### 8. TAG_KEY_PROPERTY
- **Attributes:** `ATTR_NAME`, `ATTR_COLUMN`, `ATTR_TYPE`
- **Status:** ❌ Missing
- **HBM:** `<key-property name="prop" column="col" type="string"/>`
- **JPA:** Part of composite key

### 9. TAG_LIST_INDEX
- **Attributes:** `ATTR_COLUMN`
- **Status:** ❌ Missing (basic test exists but not all attributes)
- **HBM:** `<list-index column="position"/>`
- **JPA:** `@OrderColumn`

### 10. TAG_QUERY
- **Attributes:** `ATTR_NAME`
- **Status:** ❌ Missing
- **HBM:** `<query name="findByName">...</query>`
- **JPA:** `@NamedQuery`

### 11. TAG_SQL_QUERY
- **Attributes:** `ATTR_NAME`
- **Status:** ❌ Missing
- **HBM:** `<sql-query name="findBySQL">...</sql-query>`
- **JPA:** `@NamedNativeQuery`

### 12. TAG_RETURN_SCALAR
- **Attributes:** `ATTR_COLUMN`, `ATTR_TYPE`
- **Status:** ❌ Missing
- **HBM:** `<return-scalar column="col" type="string"/>`
- **JPA:** Part of native query result mapping

## Tags with Incomplete Attribute Coverage

### 1. TAG_COLUMN
- **Defined Attributes:** `ATTR_NAME`, `ATTR_LENGTH`, `ATTR_NOT_NULL`, `ATTR_INDEX`, `ATTR_UNIQUE`, `ATTR_DEFAULT`, `ATTR_UNIQUE_KEY`, `ATTR_SQL_TYPE`, `ATTR_PRECISION`, `ATTR_SCALE`
- **Tested Attributes:** `ATTR_NAME`, `ATTR_LENGTH`, `ATTR_NOT_NULL` (basic test)
- **Missing:** `ATTR_INDEX`, `ATTR_UNIQUE`, `ATTR_DEFAULT`, `ATTR_UNIQUE_KEY`, `ATTR_SQL_TYPE`, `ATTR_PRECISION`, `ATTR_SCALE`

### 2. TAG_PROPERTY
- **Defined Attributes:** `ATTR_NAME`, `ATTR_TYPE`, `ATTR_COLUMN`, `ATTR_UPDATE`, `ATTR_LAZY`, `ATTR_LENGTH`, `ATTR_OPTIMISTIC_LOCK`
- **Tested Attributes:** `ATTR_NAME`, `ATTR_TYPE`, `ATTR_COLUMN`, `ATTR_LENGTH` (partial)
- **Missing:** `ATTR_UPDATE`, `ATTR_LAZY`, `ATTR_OPTIMISTIC_LOCK` (comprehensive test)

### 3. TAG_CLASS
- **Defined Attributes:** `ATTR_NAME`, `ATTR_TABLE`, `ATTR_DYNAMIC_INSERT`, `ATTR_DYNAMIC_UPDATE`, `ATTR_ABSTRACT`, `ATTR_MUTABLE`, `ATTR_DISCRIMINATOR_VALUE`
- **Tested Attributes:** `ATTR_NAME`, `ATTR_TABLE` (basic)
- **Missing:** `ATTR_DYNAMIC_INSERT`, `ATTR_DYNAMIC_UPDATE`, `ATTR_ABSTRACT`, `ATTR_MUTABLE`, `ATTR_DISCRIMINATOR_VALUE`

### 4. Relationship Tags (MANY_TO_ONE, ONE_TO_ONE, etc.)
- **Defined Attributes:** `ATTR_NAME`, `ATTR_CLASS`, `ATTR_LAZY`, `ATTR_CASCADE`, `ATTR_ACCESS`, `ATTR_INDEX`, `ATTR_UPDATE`, `ATTR_NOT_NULL`, `ATTR_FOREIGN_KEY`, `ATTR_UNIQUE`, `ATTR_COLUMN`, `ATTR_CONSTRAINED`, `ATTR_PROPERTY_REF`, `ATTR_FETCH`
- **Tested Attributes:** Basic relationship mapping
- **Missing:** Many attributes like `ATTR_CONSTRAINED`, `ATTR_PROPERTY_REF`, `ATTR_INDEX`, etc.

## Priority for Missing Tests

### High Priority (Core Hibernate Features)
1. **TAG_COLUMN** - Complete all attributes
2. **TAG_JOIN** - Secondary tables
3. **TAG_DISCRIMINATOR** - Complete attributes
4. **TAG_PROPERTIES** - Property grouping

### Medium Priority (Advanced Features)
1. **TAG_MAP_KEY** / **TAG_COMPOSITE_MAP_KEY**
2. **TAG_KEY_PROPERTY**
3. **TAG_LIST_INDEX** - Complete attributes
4. **TAG_TYPE** - Standalone type definitions

### Lower Priority (Query Features)
1. **TAG_QUERY** / **TAG_SQL_QUERY**
2. **TAG_RETURN_SCALAR**
3. **TAG_PARAM** - Standalone param tests

## Hibernate 4 Documentation Verification Needed

For each test, I need to verify against Hibernate 4 documentation:
1. **Correct JPA annotation mapping**
2. **Proper attribute handling**
3. **Default values and behaviors**
4. **Annotation combinations and conflicts**

## Next Steps

1. Create comprehensive tests for missing tags
2. Complete attribute coverage for existing tags
3. Verify all conversions against Hibernate 4 docs
4. Ensure proper JPA annotation generation
5. Test edge cases and attribute combinations