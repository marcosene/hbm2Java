package com.devtools.model.hbm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devtools.model.jpa.JpaBase;
import com.devtools.model.jpa.JpaEntity;
import com.devtools.model.jpa.JpaColumn;
import com.devtools.model.jpa.JpaRelationship;
import com.devtools.processing.AnnotationBuilder;
import com.devtools.processing.HbmParser;

/**
 * Basic test coverage for Hibernate mapping tags to verify parsing functionality.
 * Tests focus on verifying that tags are correctly parsed and basic JPA model is created.
 */
public class BasicHibernateMappingTest {

    @TempDir
    Path tempDir;
    
    private HbmParser parser;
    private AnnotationBuilder annotationBuilder;

    @BeforeEach
    void setUp() {
        parser = new HbmParser();
        annotationBuilder = new AnnotationBuilder(tempDir.toString());
    }

    /**
     * Test basic class tag parsing
     */
    @Test
    void testBasicClassTag() throws Exception {
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="TestEntity" table="test_table">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <property name="name" type="string"/>
                </class>
            </hibernate-mapping>
            """;

        JpaBase jpaBase = parseHbmContent(hbmContent);
        
        assertThat(jpaBase.getEntities()).hasSize(1);
        JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getName()).isEqualTo("TestEntity");
        assertThat(entity.getTable()).isEqualTo("test_table");
        assertThat(entity.getPrimaryKey()).isNotNull();
        assertThat(entity.getPrimaryKey().getName()).isEqualTo("id");
        assertThat(entity.getColumns()).hasSize(1);
        assertThat(entity.getColumns().get(0).getName()).isEqualTo("name");
    }

    /**
     * Test property tag with different types
     */
    @Test
    void testPropertyTagTypes() throws Exception {
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="TypeTestEntity" table="type_test">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <property name="stringProp" type="string"/>
                    <property name="intProp" type="int"/>
                    <property name="boolProp" type="boolean"/>
                    <property name="dateProp" type="date"/>
                </class>
            </hibernate-mapping>
            """;

        JpaBase jpaBase = parseHbmContent(hbmContent);
        JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getColumns()).hasSize(4);
        
        JpaColumn stringCol = entity.getColumns().stream()
            .filter(c -> "stringProp".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(stringCol.getType()).isEqualTo("string");
        
        JpaColumn intCol = entity.getColumns().stream()
            .filter(c -> "intProp".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(intCol.getType()).isEqualTo("int");
        
        JpaColumn boolCol = entity.getColumns().stream()
            .filter(c -> "boolProp".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(boolCol.getType()).isEqualTo("boolean");
        
        JpaColumn dateCol = entity.getColumns().stream()
            .filter(c -> "dateProp".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(dateCol.getType()).isEqualTo("date");
    }

    /**
     * Test column tag attributes
     */
    @Test
    void testColumnTagAttributes() throws Exception {
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="ColumnTestEntity" table="column_test">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <property name="description" type="string">
                        <column name="desc_column" length="500" not-null="true"/>
                    </property>
                </class>
            </hibernate-mapping>
            """;

        JpaBase jpaBase = parseHbmContent(hbmContent);
        JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getColumns()).hasSize(1);
        JpaColumn column = entity.getColumns().get(0);
        
        assertThat(column.getName()).isEqualTo("description");
        assertThat(column.getColumnName()).isEqualTo("desc_column");
        assertThat(column.getLength()).isEqualTo(500);
        assertThat(column.isNullable()).isFalse();
    }

    /**
     * Test generator tag variations
     */
    @Test
    void testGeneratorVariations() throws Exception {
        // Test known generators that map to specific JPA types
        String[][] generatorMappings = {
            {"identity", "IDENTITY"},
            {"sequence", "SEQUENCE"},
            {"increment", "AUTO"},
            {"native", "AUTO"}
        };
        
        for (String[] mapping : generatorMappings) {
            String hibernateGenerator = mapping[0];
            String expectedJpaType = mapping[1];
            
            String hbmContent = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                    "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
                <hibernate-mapping>
                    <class name="GeneratorTestEntity" table="generator_test">
                        <id name="id" type="long">
                            <generator class="%s"/>
                        </id>
                    </class>
                </hibernate-mapping>
                """, hibernateGenerator);

            JpaBase jpaBase = parseHbmContent(hbmContent);
            JpaEntity entity = jpaBase.getEntities().get(0);
            
            assertThat(entity.getPrimaryKey()).isNotNull();
            assertThat(entity.getPrimaryKey().getGeneratorType()).isEqualTo(expectedJpaType);
        }
        
        // Test unknown generators that map to GENERATOR
        String[] unknownGenerators = {"uuid", "assigned", "custom"};
        for (String generator : unknownGenerators) {
            String hbmContent = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                    "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
                <hibernate-mapping>
                    <class name="GeneratorTestEntity" table="generator_test">
                        <id name="id" type="long">
                            <generator class="%s"/>
                        </id>
                    </class>
                </hibernate-mapping>
                """, generator);

            JpaBase jpaBase = parseHbmContent(hbmContent);
            JpaEntity entity = jpaBase.getEntities().get(0);
            
            assertThat(entity.getPrimaryKey()).isNotNull();
            assertThat(entity.getPrimaryKey().getGeneratorType()).isEqualTo("GENERATOR");
        }
    }

    /**
     * Test basic many-to-one relationship
     */
    @Test
    void testBasicManyToOneRelationship() throws Exception {
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="OrderEntity" table="orders">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <many-to-one name="customer" class="CustomerEntity" column="customer_id"/>
                </class>
            </hibernate-mapping>
            """;

        JpaBase jpaBase = parseHbmContent(hbmContent);
        JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getRelationships()).hasSize(1);
        JpaRelationship relationship = entity.getRelationships().get(0);
        
        assertThat(relationship.getName()).isEqualTo("customer");
        assertThat(relationship.getType()).isEqualTo("CustomerEntity");
        assertThat(relationship.getRelationshipType()).isEqualTo(JpaRelationship.Type.ManyToOne);
    }

    /**
     * Test basic one-to-many collection
     */
    @Test
    void testBasicOneToManyCollection() throws Exception {
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="CustomerEntity" table="customers">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <set name="orders">
                        <key column="customer_id"/>
                        <one-to-many class="OrderEntity"/>
                    </set>
                </class>
            </hibernate-mapping>
            """;

        JpaBase jpaBase = parseHbmContent(hbmContent);
        JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getRelationships()).hasSize(1);
        JpaRelationship relationship = entity.getRelationships().get(0);
        
        assertThat(relationship.getName()).isEqualTo("orders");
        assertThat(relationship.getType()).isEqualTo("OrderEntity");
        assertThat(relationship.getCollectionType()).isEqualTo("set");
        assertThat(relationship.getRelationshipType()).isEqualTo(JpaRelationship.Type.OneToMany);
    }

    /**
     * Test discriminator tag
     */
    @Test
    void testDiscriminatorTag() throws Exception {
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="BaseEntity" table="base_table">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <discriminator type="string">
                        <column name="entity_type"/>
                    </discriminator>
                </class>
            </hibernate-mapping>
            """;

        JpaBase jpaBase = parseHbmContent(hbmContent);
        JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getDiscriminator(false)).isNotNull();
        assertThat(entity.getDiscriminator(false).getColumn()).isEqualTo("entity_type");
        assertThat(entity.getDiscriminator(false).getType()).isEqualTo("string");
    }

    /**
     * Test version tag
     */
    @Test
    void testVersionTag() throws Exception {
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="VersionedEntity" table="versioned_table">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <version name="version" type="int">
                        <column name="version_column"/>
                    </version>
                </class>
            </hibernate-mapping>
            """;

        JpaBase jpaBase = parseHbmContent(hbmContent);
        JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getColumns()).hasSize(1);
        JpaColumn versionColumn = entity.getColumns().get(0);
        
        assertThat(versionColumn.getName()).isEqualTo("version");
        assertThat(versionColumn.getType()).isEqualTo("int");
        assertThat(versionColumn.isVersion()).isTrue();
    }

    /**
     * Test cache tag
     */
    @Test
    void testCacheTag() throws Exception {
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="CachedEntity" table="cached_table">
                    <cache usage="read-write"/>
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                </class>
            </hibernate-mapping>
            """;

        JpaBase jpaBase = parseHbmContent(hbmContent);
        JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getCacheUsage()).isEqualTo("read-write");
    }

    /**
     * Test subclass tag
     */
    @Test
    void testSubclassTag() throws Exception {
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="BaseEntity" table="base_table">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                </class>
                <subclass name="SubEntity" extends="BaseEntity" discriminator-value="SUB">
                    <property name="subProperty" type="string"/>
                </subclass>
            </hibernate-mapping>
            """;

        JpaBase jpaBase = parseHbmContent(hbmContent);
        
        assertThat(jpaBase.getEntities()).hasSize(2);
        
        JpaEntity subEntity = jpaBase.getEntities().stream()
            .filter(e -> "SubEntity".equals(e.getName()))
            .findFirst().orElseThrow();
            
        assertThat(subEntity.getParentClass()).isEqualTo("BaseEntity");
        assertThat(subEntity.getDiscriminator(false).getValue()).isEqualTo("SUB");
        assertThat(subEntity.getColumns()).hasSize(1);
        assertThat(subEntity.getColumns().get(0).getName()).isEqualTo("subProperty");
    }

    // Helper method to parse HBM content
    private JpaBase parseHbmContent(String hbmContent) throws Exception {
        File tempFile = tempDir.resolve("test.hbm.xml").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(hbmContent);
        }
        return parser.parse(tempFile.getAbsolutePath());
    }
}