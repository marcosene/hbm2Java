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
 * Advanced test coverage for Hibernate mapping tags including collections, 
 * complex relationships, and specialized attributes.
 */
public class AdvancedHibernateMappingTest {

    @TempDir
    Path tempDir;
    
    private HbmParser parser;

    @BeforeEach
    void setUp() {
        parser = new HbmParser();
    }

    /**
     * Test set collection with one-to-many
     */
    @Test
    void testSetCollectionWithOneToMany() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="CustomerEntity" table="customers">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <set name="orders" cascade="all" lazy="false">
                        <key column="customer_id"/>
                        <one-to-many class="OrderEntity"/>
                    </set>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getRelationships()).hasSize(1);
        final JpaRelationship relationship = entity.getRelationships().get(0);
        
        assertThat(relationship.getName()).isEqualTo("orders");
        assertThat(relationship.getType()).isEqualTo("OrderEntity");
        assertThat(relationship.getCollectionType()).isEqualTo("set");
        assertThat(relationship.getRelationshipType()).isEqualTo(JpaRelationship.Type.OneToMany);
        assertThat(relationship.getCascade()).isEqualTo("all");
        assertThat(relationship.getFetch()).isEqualTo("eager");
        
        // Check key column
        assertThat(relationship.getReferencedColumns()).hasSize(1);
        assertThat(relationship.getReferencedColumns().get(0).getColumnName()).isEqualTo("customer_id");
    }

    /**
     * Test list collection with list-index
     */
    @Test
    void testListCollectionWithIndex() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="PlaylistEntity" table="playlists">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <list name="songs">
                        <key column="playlist_id"/>
                        <list-index column="song_order"/>
                        <one-to-many class="SongEntity"/>
                    </list>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getRelationships()).hasSize(1);
        final JpaRelationship relationship = entity.getRelationships().get(0);
        
        assertThat(relationship.getName()).isEqualTo("songs");
        assertThat(relationship.getCollectionType()).isEqualTo("list");
        assertThat(relationship.getOrderColumn()).isEqualTo("song_order");
    }

    /**
     * Test bag collection
     */
    @Test
    void testBagCollection() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="CategoryEntity" table="categories">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <bag name="items">
                        <key column="category_id"/>
                        <one-to-many class="ItemEntity"/>
                    </bag>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getRelationships()).hasSize(1);
        final JpaRelationship relationship = entity.getRelationships().get(0);
        
        assertThat(relationship.getName()).isEqualTo("items");
        assertThat(relationship.getCollectionType()).isEqualTo("bag");
        assertThat(relationship.getRelationshipType()).isEqualTo(JpaRelationship.Type.OneToMany);
    }

    /**
     * Test map collection (element tag not implemented, so test basic map)
     */
    @Test
    void testMapCollection() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="ConfigEntity" table="configs">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <map name="properties">
                        <key column="config_id"/>
                        <map-key type="string" column="property_key"/>
                        <one-to-many class="PropertyEntity"/>
                    </map>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getRelationships()).hasSize(1);
        final JpaRelationship relationship = entity.getRelationships().get(0);
        
        assertThat(relationship.getName()).isEqualTo("properties");
        assertThat(relationship.getCollectionType()).isEqualTo("map");
    }

    /**
     * Test many-to-many relationship
     */
    @Test
    void testManyToManyRelationship() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="StudentEntity" table="students">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <set name="courses" table="student_courses">
                        <key column="student_id"/>
                        <many-to-many class="CourseEntity" column="course_id"/>
                    </set>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getRelationships()).hasSize(1);
        final JpaRelationship relationship = entity.getRelationships().get(0);
        
        assertThat(relationship.getName()).isEqualTo("courses");
        assertThat(relationship.getType()).isEqualTo("CourseEntity");
        assertThat(relationship.getTable()).isEqualTo("student_courses");
        assertThat(relationship.getRelationshipType()).isEqualTo(JpaRelationship.Type.ManyToMany);
    }

    /**
     * Test one-to-one relationship with foreign key
     */
    @Test
    void testOneToOneWithForeignKey() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="UserEntity" table="users">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <one-to-one name="profile" class="ProfileEntity"
                                foreign-key="FK_USER_PROFILE"
                                cascade="save-update"/>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getRelationships()).hasSize(1);
        final JpaRelationship relationship = entity.getRelationships().get(0);
        
        assertThat(relationship.getName()).isEqualTo("profile");
        assertThat(relationship.getType()).isEqualTo("ProfileEntity");
        assertThat(relationship.getRelationshipType()).isEqualTo(JpaRelationship.Type.OneToOne);
        assertThat(relationship.getCascade()).isEqualTo("save-update");
    }

    /**
     * Test component tag (partially implemented)
     */
    @Test
    void testComponentTag() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="PersonEntity" table="persons">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <component name="address" class="AddressComponent">
                        <property name="street" type="string"/>
                        <property name="city" type="string"/>
                        <property name="zipCode" type="string"/>
                    </component>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        // Component creates an embedded field, not individual columns
        assertThat(entity.getEmbeddedFields()).hasSize(1);
        final JpaEntity embeddedEntity = entity.getEmbeddedFields().get(0);
        assertThat(embeddedEntity.getName()).isEqualTo("AddressComponent"); // Uses class name, not field name
    }

    /**
     * Test natural-id tag
     */
    @Test
    void testNaturalIdTag() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="ProductEntity" table="products">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <natural-id mutable="false">
                        <property name="sku" type="string"/>
                    </natural-id>
                    <property name="name" type="string"/>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getColumns()).hasSize(2);
        
        final JpaColumn skuColumn = entity.getColumns().stream()
            .filter(c -> "sku".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(skuColumn.getNaturalId()).isNotEqualTo(JpaColumn.NaturalId.NONE);
        
        final JpaColumn nameColumn = entity.getColumns().stream()
            .filter(c -> "name".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(nameColumn.getNaturalId()).isEqualTo(JpaColumn.NaturalId.NONE);
    }

    /**
     * Test property with multiple column attributes
     */
    @Test
    void testPropertyWithComplexColumn() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="DocumentEntity" table="documents">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <property name="content" type="text" lazy="true" access="field">
                        <column name="document_content"
                                length="65535"
                                not-null="false"
                                unique="false"
                                index="idx_content"/>
                    </property>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        assertThat(entity.getColumns()).hasSize(1);
        final JpaColumn column = entity.getColumns().get(0);
        
        assertThat(column.getName()).isEqualTo("content");
        assertThat(column.getType()).isEqualTo("text");
        assertThat(column.getColumnName()).isEqualTo("document_content");
        assertThat(column.getLength()).isEqualTo(65535);
        assertThat(column.isNullable()).isTrue();
        assertThat(column.isUnique()).isFalse();
        assertThat(column.isLazy()).isTrue();
        assertThat(column.getIndex()).isEqualTo("idx_content");
    }

    /**
     * Test timestamp property (not implemented, so test that it's ignored)
     */
    @Test
    void testTimestampProperty() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="AuditEntity" table="audit_log">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <timestamp name="lastModified" column="last_modified"/>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        // Timestamp tag is not implemented, so no columns should be created
        assertThat(entity.getColumns()).hasSize(0);
    }

    /**
     * Test join table
     */
    @Test
    void testJoinTable() throws Exception {
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="EmployeeEntity" table="employees">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <join table="employee_details">
                        <key column="employee_id"/>
                        <property name="biography" type="text"/>
                        <property name="notes" type="text"/>
                    </join>
                </class>
            </hibernate-mapping>
            """;

        final JpaBase jpaBase = parseHbmContent(hbmContent);
        final JpaEntity entity = jpaBase.getEntities().get(0);
        
        // Join table creates a secondary table mapping
        assertThat(entity.getColumns()).hasSize(2);
        
        final JpaColumn bioColumn = entity.getColumns().stream()
            .filter(c -> "biography".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(bioColumn.getType()).isEqualTo("text");
        
        final JpaColumn notesColumn = entity.getColumns().stream()
            .filter(c -> "notes".equals(c.getName()))
            .findFirst().orElseThrow();
        assertThat(notesColumn.getType()).isEqualTo("text");
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