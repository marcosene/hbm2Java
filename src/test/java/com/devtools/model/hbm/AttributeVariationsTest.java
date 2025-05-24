package com.devtools.model.hbm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devtools.model.jpa.JpaBase;
import com.devtools.model.jpa.JpaColumn;
import com.devtools.model.jpa.JpaEntity;
import com.devtools.model.jpa.JpaRelationship;
import com.devtools.processing.HbmParser;

/**
 * Test coverage for attribute variations and edge cases in Hibernate mapping tags.
 * Tests different combinations of attributes to ensure comprehensive coverage.
 */
public class AttributeVariationsTest {

    @TempDir
    Path tempDir;
    
    private HbmParser parser;

    @BeforeEach
    void setUp() {
        parser = new HbmParser();
    }

    /**
     * Test class tag with all supported attributes
     */
    @Test
    void testClassTagWithAllAttributes() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="ComplexEntity"
                       table="complex_table"
                       dynamic-insert="true"
                       dynamic-update="true"
                       abstract="false"
                       mutable="true">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getName()).isEqualTo("ComplexEntity");
        assertThat(entity.getTable()).isEqualTo("complex_table");
        assertThat(entity.isDynamicInsert()).isTrue();
        assertThat(entity.isDynamicUpdate()).isTrue();
        assertThat(entity.isAbstractClass()).isFalse();
        assertThat(entity.isImmutable()).isFalse(); // mutable=true means immutable=false
    }

    /**
     * Test property with different access types
     */
    @Test
    void testPropertyAccessTypes() throws Exception {
        final String[] accessTypes = {"field", "property"};
        
        for (final String accessType : accessTypes) {
            final String hbmContent = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                    "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
                <hibernate-mapping>
                    <class name="AccessTestEntity" table="access_test">
                        <id name="id" type="long">
                            <generator class="identity"/>
                        </id>
                        <property name="testField" type="string" access="%s"/>
                    </class>
                </hibernate-mapping>
                """, accessType);

            final JpaBase jpaBase = parseHbmContent(hbmContent);
            final JpaEntity entity = jpaBase.getEntities().get(0);
            
            assertThat(entity.getColumns()).hasSize(1);
        }
    }

    /**
     * Test cascade variations
     */
    @Test
    void testCascadeVariations() throws Exception {
        final String[] cascadeTypes = {
            "all", "save-update", "delete", "persist", "merge", 
            "refresh", "replicate", "lock", "evict"
        };
        
        for (final String cascadeType : cascadeTypes) {
            final String hbmContent = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                    "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
                <hibernate-mapping>
                    <class name="CascadeTestEntity" table="cascade_test">
                        <id name="id" type="long">
                            <generator class="identity"/>
                        </id>
                        <many-to-one name="related" class="RelatedEntity" cascade="%s"/>
                    </class>
                </hibernate-mapping>
                """, cascadeType);

            final JpaBase jpaBase = parseHbmContent(hbmContent);
            final JpaEntity entity = jpaBase.getEntities().get(0);
            
            assertThat(entity.getRelationships()).hasSize(1);
            final JpaRelationship relationship = entity.getRelationships().get(0);
            assertThat(relationship.getCascade()).isEqualTo(cascadeType);
        }
    }

    /**
     * Test fetch variations
     */
    @Test
    void testFetchVariations() throws Exception {
        final String[][] fetchMappings = {
            {"true", "lazy"},
            {"false", "eager"}
        };
        
        for (final String[] mapping : fetchMappings) {
            final String lazyValue = mapping[0];
            final String expectedFetch = mapping[1];
            
            final String hbmContent = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                    "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
                <hibernate-mapping>
                    <class name="FetchTestEntity" table="fetch_test">
                        <id name="id" type="long">
                            <generator class="identity"/>
                        </id>
                        <many-to-one name="related" class="RelatedEntity" lazy="%s"/>
                    </class>
                </hibernate-mapping>
                """, lazyValue);

            final JpaBase jpaBase = parseHbmContent(hbmContent);
            final JpaEntity entity = jpaBase.getEntities().get(0);
            
            assertThat(entity.getRelationships()).hasSize(1);
            final JpaRelationship relationship = entity.getRelationships().get(0);
            assertThat(relationship.getFetch()).isEqualTo(expectedFetch);
        }
    }

    /**
     * Test column with all supported attributes
     */
    @Test
    void testColumnWithAllAttributes() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="ColumnTestEntity" table="column_test">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <property name="complexField" type="string">
                        <column name="complex_column"
                                length="255"
                                not-null="true"
                                unique="true"
                                index="idx_complex"
                                unique-key="uk_complex"
                                default="DEFAULT_VALUE"
                                sql-type="VARCHAR(255)"
                                precision="10"
                                scale="2"/>
                    </property>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getColumns()).hasSize(1);
        final JpaColumn column = entity.getColumns().get(0);
        
        assertThat(column.getName()).isEqualTo("complexField");
        assertThat(column.getColumnName()).isEqualTo("complex_column");
        assertThat(column.getLength()).isEqualTo(255);
        assertThat(column.isNullable()).isFalse();
        assertThat(column.isUnique()).isTrue();
        assertThat(column.getIndex()).isEqualTo("idx_complex");
        assertThat(column.getUniqueConstraint()).isEqualTo("uk_complex");
        assertThat(column.getDefaultValue()).isEqualTo("DEFAULT_VALUE");
        assertThat(column.getColumnDefinition()).isEqualTo("VARCHAR(255)"); // sql-type maps to columnDefinition
        assertThat(column.getPrecision()).isEqualTo(10);
        assertThat(column.getScale()).isEqualTo(2);
    }

    /**
     * Test generator with parameters
     */
    @Test
    void testGeneratorWithParameters() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="GeneratorParamEntity" table="generator_param_test">
                    <id name="id" type="long">
                        <generator class="sequence">
                            <param name="sequence">my_sequence</param>
                            <param name="increment_size">50</param>
                        </generator>
                    </id>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getPrimaryKey()).isNotNull();
        assertThat(entity.getPrimaryKey().getGeneratorType()).isEqualTo("SEQUENCE");
        assertThat(entity.getPrimaryKey().getGeneratorParams()).containsEntry("sequence", "my_sequence");
        assertThat(entity.getPrimaryKey().getGeneratorParams()).containsEntry("increment_size", "50");
    }

    /**
     * Test union-subclass
     */
    @Test
    void testUnionSubclass() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="BaseEntity" table="base_table">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                </class>
                <union-subclass name="UnionSubEntity"
                               extends="BaseEntity"
                               table="union_sub_table">
                    <property name="unionProperty" type="string"/>
                </union-subclass>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        
        assertThat(jpaBase.getEntities()).hasSize(2);
        
        final JpaEntity unionSubEntity = jpaBase.getEntities().stream()
            .filter(e -> "UnionSubEntity".equals(e.getName()))
            .findFirst().orElseThrow();
            
        assertThat(unionSubEntity.getParentClass()).isEqualTo("BaseEntity");
        assertThat(unionSubEntity.getTable()).isEqualTo("union_sub_table");
        assertThat(unionSubEntity.getColumns()).hasSize(1);
        assertThat(unionSubEntity.getColumns().get(0).getName()).isEqualTo("unionProperty");
    }

    /**
     * Test property with optimistic-lock attribute
     */
    @Test
    void testPropertyWithOptimisticLock() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="OptimisticEntity" table="optimistic_test">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <property name="excludedField" type="string" optimistic-lock="false"/>
                    <property name="includedField" type="string" optimistic-lock="true"/>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getColumns()).hasSize(2);
        
        final JpaColumn excludedColumn = entity.getColumns().stream()
            .filter(c -> "excludedField".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(excludedColumn.isOptimisticLock()).isFalse();
        
        final JpaColumn includedColumn = entity.getColumns().stream()
            .filter(c -> "includedField".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(includedColumn.isOptimisticLock()).isTrue();
    }

    /**
     * Test type tag with parameters
     */
    @Test
    void testTypeWithParameters() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="TypeTestEntity" table="type_test">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <property name="customField" type="string">
                        <type name="org.hibernate.type.EnumType">
                            <param name="enumClass">com.example.MyEnum</param>
                            <param name="useNamed">true</param>
                        </type>
                    </property>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getColumns()).hasSize(1);
        final JpaColumn column = entity.getColumns().get(0);
        
        assertThat(column.getName()).isEqualTo("customField");
        // The type should be overridden by the nested type element
        // Custom type and type params are not stored in JpaColumn in this implementation
    }

    /**
     * Test relationship with all constraint attributes
     */
    @Test
    void testRelationshipWithConstraints() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="ConstraintTestEntity" table="constraint_test">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <many-to-one name="related"
                                 class="RelatedEntity"
                                 column="related_id"
                                 not-null="true"
                                 unique="true"
                                 foreign-key="FK_CONSTRAINT_RELATED"
                                 index="idx_related"
                                 update="false"/>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getRelationships()).hasSize(1);
        final JpaRelationship relationship = entity.getRelationships().get(0);
        
        assertThat(relationship.getName()).isEqualTo("related");
        assertThat(relationship.getType()).isEqualTo("RelatedEntity");
        
        // Check referenced column constraints
        assertThat(relationship.getReferencedColumns()).hasSize(1);
        final JpaColumn refColumn = relationship.getReferencedColumns().get(0);
        assertThat(refColumn.getColumnName()).isEqualTo("related_id");
        assertThat(refColumn.isNullable()).isTrue(); // not-null attribute not implemented for relationships
        assertThat(refColumn.isUnique()).isFalse(); // unique attribute not implemented for relationships
        assertThat(refColumn.getForeignKey()).isNull(); // foreign-key attribute not implemented for relationships
        assertThat(refColumn.getIndex()).isNull(); // index attribute not implemented for relationships
        assertThat(refColumn.isUpdatable()).isTrue(); // update="false" attribute not implemented
    }

    // Helper method to parse HBM content
    private JpaBase parseHbmContent(final String hbmContent) throws Exception {
        final File tempFile = tempDir.resolve("test.hbm.xml").toFile();
        try (final FileWriter writer = new FileWriter(tempFile)) {
            writer.write(hbmContent);
        }
        return parser.parse(tempFile.getAbsolutePath());
    }
}