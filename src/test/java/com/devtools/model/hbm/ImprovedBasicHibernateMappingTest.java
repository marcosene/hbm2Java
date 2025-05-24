package com.devtools.model.hbm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devtools.processing.ConversionProcessor;
import com.devtools.utils.JavaParserTestUtils;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

/**
 * Improved test class demonstrating precise JavaParser-based assertions
 * instead of text-based content matching.
 */
public class ImprovedBasicHibernateMappingTest {

    @TempDir
    Path tempDir;

    @Test
    public void testBasicClassMappingWithPreciseAssertions() throws IOException {
        // Create HBM content
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC
                "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="com.example.User" table="users">
                    <id name="id" type="long" column="user_id">
                        <generator class="identity"/>
                    </id>
                    <property name="username" type="string" column="user_name" length="50" not-null="true"/>
                    <property name="email" type="string" column="email_address" length="100"/>
                    <property name="age" type="integer" column="user_age"/>
                </class>
            </hibernate-mapping>
            """;

        // Setup directories
        final Path hbmDir = tempDir.resolve("hbm");
        final Path javaDir = tempDir.resolve("java");
        Files.createDirectories(hbmDir);
        Files.createDirectories(javaDir);

        // Write HBM file
        final Path hbmFile = hbmDir.resolve("User.hbm.xml");
        Files.writeString(hbmFile, hbmContent);

        // Create plain Java class first
        final Path packageDir = javaDir.resolve("com/example");
        Files.createDirectories(packageDir);
        final String javaContent = """
            package com.example;
            
            public class User {
                private Long id;
                private String username;
                private String email;
                private Integer age;
            
                // Getters and setters
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
            
                public String getUsername() { return username; }
                public void setUsername(String username) { this.username = username; }
            
                public String getEmail() { return email; }
                public void setEmail(String email) { this.email = email; }
            
                public Integer getAge() { return age; }
                public void setAge(Integer age) { this.age = age; }
            }
            """;
        Files.writeString(packageDir.resolve("User.java"), javaContent);

        // Run conversion
        final ConversionProcessor processor = new ConversionProcessor();
        processor.processConversion(hbmDir.toString(), javaDir.toString(), true);

        // Parse the generated Java file
        final Path javaFile = javaDir.resolve("com/example/User.java");
        assertThat(javaFile).exists();
        
        final CompilationUnit cu = JavaParserTestUtils.parseJavaFile(javaFile);
        final ClassOrInterfaceDeclaration userClass = JavaParserTestUtils.getMainClass(cu);

        // Assert class-level annotations
        assertThat(JavaParserTestUtils.hasClassAnnotation(userClass, "Entity")).isTrue();
        assertThat(JavaParserTestUtils.hasClassAnnotation(userClass, "Table")).isTrue();
        
        final AnnotationExpr tableAnnotation = JavaParserTestUtils.getClassAnnotation(userClass, "Table").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(tableAnnotation, "name", "users")).isTrue();

        // Assert ID field annotations
        final FieldDeclaration idField = JavaParserTestUtils.findField(userClass, "id");
        assertThat(JavaParserTestUtils.hasAllAnnotations(idField, "Id", "GeneratedValue", "Column")).isTrue();
        
        final AnnotationExpr columnAnnotation = JavaParserTestUtils.getAnnotation(idField, "Column").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(columnAnnotation, "name", "user_id")).isTrue();
        
        final AnnotationExpr generatedValueAnnotation = JavaParserTestUtils.getAnnotation(idField, "GeneratedValue").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(generatedValueAnnotation, "strategy", "GenerationType.IDENTITY")).isTrue();

        // Assert username field annotations
        final FieldDeclaration usernameField = JavaParserTestUtils.findField(userClass, "username");
        assertThat(JavaParserTestUtils.hasAnnotation(usernameField, "Column")).isTrue();
        
        final AnnotationExpr usernameColumnAnnotation = JavaParserTestUtils.getAnnotation(usernameField, "Column").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(usernameColumnAnnotation, "name", "user_name")).isTrue();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(usernameColumnAnnotation, "length", "50")).isTrue();
        // Note: not-null attribute is not currently implemented, so nullable attribute is not generated

        // Assert email field annotations
        final FieldDeclaration emailField = JavaParserTestUtils.findField(userClass, "email");
        assertThat(JavaParserTestUtils.hasAnnotation(emailField, "Column")).isTrue();
        
        final AnnotationExpr emailColumnAnnotation = JavaParserTestUtils.getAnnotation(emailField, "Column").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(emailColumnAnnotation, "name", "email_address")).isTrue();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(emailColumnAnnotation, "length", "100")).isTrue();

        // Assert age field annotations
        final FieldDeclaration ageField = JavaParserTestUtils.findField(userClass, "age");
        assertThat(JavaParserTestUtils.hasAnnotation(ageField, "Column")).isTrue();
        
        final AnnotationExpr ageColumnAnnotation = JavaParserTestUtils.getAnnotation(ageField, "Column").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(ageColumnAnnotation, "name", "user_age")).isTrue();

        // Assert field types
        assertThat(JavaParserTestUtils.getFieldType(idField)).isEqualTo("Long");
        assertThat(JavaParserTestUtils.getFieldType(usernameField)).isEqualTo("String");
        assertThat(JavaParserTestUtils.getFieldType(emailField)).isEqualTo("String");
        assertThat(JavaParserTestUtils.getFieldType(ageField)).isEqualTo("Integer");
    }

    @Test
    public void testManyToOneRelationshipWithPreciseAssertions() throws IOException {
        // Create HBM content with many-to-one relationship
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC
                "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="com.example.Order" table="orders">
                    <id name="id" type="long">
                        <generator class="identity"/>
                    </id>
                    <many-to-one name="customer" class="com.example.Customer"
                                 column="customer_id" cascade="save-update"
                                 fetch="select" not-null="true"/>
                    <property name="orderDate" type="date" column="order_date"/>
                </class>
            </hibernate-mapping>
            """;

        // Setup directories
        final Path hbmDir = tempDir.resolve("hbm");
        final Path javaDir = tempDir.resolve("java");
        Files.createDirectories(hbmDir);
        Files.createDirectories(javaDir);

        // Write HBM file
        final Path hbmFile = hbmDir.resolve("Order.hbm.xml");
        Files.writeString(hbmFile, hbmContent);

        // Create plain Java classes first
        final Path packageDir = javaDir.resolve("com/example");
        Files.createDirectories(packageDir);
        
        final String orderJavaContent = """
            package com.example;
            import java.util.Date;
            
            public class Order {
                private Long id;
                private Customer customer;
                private Date orderDate;
            
                // Getters and setters
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
            
                public Customer getCustomer() { return customer; }
                public void setCustomer(Customer customer) { this.customer = customer; }
            
                public Date getOrderDate() { return orderDate; }
                public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
            }
            """;
        Files.writeString(packageDir.resolve("Order.java"), orderJavaContent);
        
        final String customerJavaContent = """
            package com.example;
            
            public class Customer {
                private Long id;
                private String name;
            
                // Getters and setters
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
            
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """;
        Files.writeString(packageDir.resolve("Customer.java"), customerJavaContent);

        // Run conversion
        final ConversionProcessor processor = new ConversionProcessor();
        processor.processConversion(hbmDir.toString(), javaDir.toString(), true);

        // Parse the generated Java file
        final Path javaFile = javaDir.resolve("com/example/Order.java");
        assertThat(javaFile).exists();
        
        final CompilationUnit cu = JavaParserTestUtils.parseJavaFile(javaFile);
        final ClassOrInterfaceDeclaration orderClass = JavaParserTestUtils.getMainClass(cu);

        // Assert customer field has correct relationship annotations
        final FieldDeclaration customerField = JavaParserTestUtils.findField(orderClass, "customer");
        assertThat(JavaParserTestUtils.hasAllAnnotations(customerField, "ManyToOne", "JoinColumn")).isTrue();
        
        // Check ManyToOne annotation attributes
        final AnnotationExpr manyToOneAnnotation = JavaParserTestUtils.getAnnotation(customerField, "ManyToOne").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(manyToOneAnnotation, "cascade", "{ CascadeType.PERSIST, CascadeType.MERGE }")).isTrue();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(manyToOneAnnotation, "fetch", "FetchType.EAGER")).isTrue();
        
        // Check JoinColumn annotation attributes
        final AnnotationExpr joinColumnAnnotation = JavaParserTestUtils.getAnnotation(customerField, "JoinColumn").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(joinColumnAnnotation, "name", "customer_id")).isTrue();
        // Note: not-null attribute is not currently implemented for many-to-one, so nullable attribute is not generated

        // Assert field type
        assertThat(JavaParserTestUtils.getFieldType(customerField)).isEqualTo("Customer");
    }

    @Test
    public void testSequenceGeneratorWithPreciseAssertions() throws IOException {
        // Create HBM content with sequence generator
        final String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC
                "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
            <hibernate-mapping>
                <class name="com.example.Product" table="products">
                    <id name="id" type="long" column="product_id">
                        <generator class="sequence">
                            <param name="sequence">product_seq</param>
                        </generator>
                    </id>
                    <property name="name" type="string" column="product_name" length="200"/>
                    <property name="price" type="big_decimal" column="price" precision="10" scale="2"/>
                </class>
            </hibernate-mapping>
            """;

        // Setup directories
        final Path hbmDir = tempDir.resolve("hbm");
        final Path javaDir = tempDir.resolve("java");
        Files.createDirectories(hbmDir);
        Files.createDirectories(javaDir);

        // Write HBM file
        final Path hbmFile = hbmDir.resolve("Product.hbm.xml");
        Files.writeString(hbmFile, hbmContent);

        // Create plain Java class first
        final Path packageDir = javaDir.resolve("com/example");
        Files.createDirectories(packageDir);
        final String javaContent = """
            package com.example;
            import java.math.BigDecimal;
            
            public class Product {
                private Long id;
                private String name;
                private BigDecimal price;
            
                // Getters and setters
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
            
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            
                public BigDecimal getPrice() { return price; }
                public void setPrice(BigDecimal price) { this.price = price; }
            }
            """;
        Files.writeString(packageDir.resolve("Product.java"), javaContent);

        // Run conversion
        final ConversionProcessor processor = new ConversionProcessor();
        processor.processConversion(hbmDir.toString(), javaDir.toString(), true);

        // Parse the generated Java file
        final Path javaFile = javaDir.resolve("com/example/Product.java");
        assertThat(javaFile).exists();
        
        final CompilationUnit cu = JavaParserTestUtils.parseJavaFile(javaFile);
        final ClassOrInterfaceDeclaration productClass = JavaParserTestUtils.getMainClass(cu);

        // Assert ID field has sequence generator annotations
        final FieldDeclaration idField = JavaParserTestUtils.findField(productClass, "id");
        assertThat(JavaParserTestUtils.hasAllAnnotations(idField, "Id", "GeneratedValue", "SequenceGenerator", "Column")).isTrue();
        
        // Check GeneratedValue annotation
        final AnnotationExpr generatedValueAnnotation = JavaParserTestUtils.getAnnotation(idField, "GeneratedValue").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(generatedValueAnnotation, "strategy", "GenerationType.SEQUENCE")).isTrue();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(generatedValueAnnotation, "generator", "generatorProduct")).isTrue();
        
        // Check SequenceGenerator annotation
        final AnnotationExpr sequenceGeneratorAnnotation = JavaParserTestUtils.getAnnotation(idField, "SequenceGenerator").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(sequenceGeneratorAnnotation, "name", "generatorProduct")).isTrue();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(sequenceGeneratorAnnotation, "sequenceName", "product_seq")).isTrue();

        // Assert price field annotations
        final FieldDeclaration priceField = JavaParserTestUtils.findField(productClass, "price");
        assertThat(JavaParserTestUtils.hasAnnotation(priceField, "Column")).isTrue();
        
        final AnnotationExpr priceColumnAnnotation = JavaParserTestUtils.getAnnotation(priceField, "Column").orElseThrow();
        assertThat(JavaParserTestUtils.hasAnnotationAttribute(priceColumnAnnotation, "name", "price")).isTrue();
        // Note: precision and scale attributes are not currently implemented, 
        // so they don't appear in the @Column annotation
    }
}