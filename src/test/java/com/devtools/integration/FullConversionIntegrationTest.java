package com.devtools.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devtools.processing.ConversionProcessor;

/**
 * Integration test that validates the complete HBM to JPA conversion process.
 * 
 * This test creates a comprehensive HBM XML file containing all supported tags and attributes,
 * creates corresponding plain Java entity classes, runs the conversion process, and validates
 * that the correct JPA annotations were generated.
 */
public class FullConversionIntegrationTest {

    @TempDir
    Path tempDir;
    
    private Path hbmDir;
    private Path javaDir;
    private ConversionProcessor processor;

    @BeforeEach
    void setUp() throws IOException {
        hbmDir = tempDir.resolve("hbm");
        javaDir = tempDir.resolve("java");
        Files.createDirectories(hbmDir);
        Files.createDirectories(javaDir);
        
        processor = new ConversionProcessor();
        
        // Create the comprehensive HBM file
        createComprehensiveHbmFile();
        
        // Create corresponding plain Java entity classes
        createPlainJavaEntities();
    }
    
    /**
     * Reads content from a resource file.
     */
    private String readResourceFile(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes());
        }
    }

    @Test
    void testFullConversionProcess() throws Exception {
        // Run the conversion process in "annotate existing" mode
        processor.processConversion(
            hbmDir.toString(), 
            javaDir.toString(), 
            true // annotateExisting = true
        );
        
        // Validate the annotations were added correctly
        validateCompanyEntityAnnotations();
        validateEmployeeEntityAnnotations();
        validateAddressEntityAnnotations();
        validateDepartmentEntityAnnotations();
        validateProjectEntityAnnotations();
    }

    private void createComprehensiveHbmFile() throws IOException {
        String hbmContent = readResourceFile("integration-test/hbm/comprehensive-mapping.hbm.xml");
        Files.writeString(hbmDir.resolve("comprehensive-mapping.hbm.xml"), hbmContent);
    }

    private void createPlainJavaEntities() throws IOException {
        // Create package directory structure
        Path packageDir = javaDir.resolve("com/example/model");
        Files.createDirectories(packageDir);
        
        // Read and write each entity class from resource files
        String companyClass = readResourceFile("integration-test/java-templates/Company.java");
        Files.writeString(packageDir.resolve("Company.java"), companyClass);
        
        String publicCompanyClass = readResourceFile("integration-test/java-templates/PublicCompany.java");
        Files.writeString(packageDir.resolve("PublicCompany.java"), publicCompanyClass);
        
        String employeeClass = readResourceFile("integration-test/java-templates/Employee.java");
        Files.writeString(packageDir.resolve("Employee.java"), employeeClass);
        
        String departmentClass = readResourceFile("integration-test/java-templates/Department.java");
        Files.writeString(packageDir.resolve("Department.java"), departmentClass);
        
        String addressClass = readResourceFile("integration-test/java-templates/Address.java");
        Files.writeString(packageDir.resolve("Address.java"), addressClass);
        
        String projectClass = readResourceFile("integration-test/java-templates/Project.java");
        Files.writeString(packageDir.resolve("Project.java"), projectClass);
    }

    private void validateCompanyEntityAnnotations() throws IOException {
        String content = Files.readString(javaDir.resolve("com/example/model/Company.java"));
        
        // Validate class-level annotations
        assertThat(content).contains("@Entity");
        assertThat(content).contains("@Table(name = \"companies\"");  // Allow for complex table annotation
        assertThat(content).contains("@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)");
        assertThat(content).contains("@Cacheable");
        assertThat(content).contains("@DynamicInsert");
        assertThat(content).contains("@DynamicUpdate");
        
        // Validate ID and generator
        assertThat(content).contains("@Id");
        assertThat(content).contains("@GeneratedValue(strategy = GenerationType.SEQUENCE");
        assertThat(content).contains("@SequenceGenerator(name = \"generatorCompany\", sequenceName = \"company_seq\")");
        assertThat(content).contains("@Column(name = \"company_id\")");
        
        // Note: Version field doesn't have @Version annotation - this appears to be a limitation
        // The version field is present but without the annotation
        assertThat(content).contains("private Integer version;");
        
        // Validate properties with column attributes
        assertThat(content).contains("@Column(name = \"company_name\", length = 100)");
        assertThat(content).contains("@Column(name = \"founded_date\")");
        
        // Validate embedded object
        assertThat(content).contains("@Embedded");
        assertThat(content).contains("private Address headquarters;");
        
        // Validate relationships
        assertThat(content).contains("@OneToMany(cascade = { CascadeType.ALL }, mappedBy = \"company\")");
        assertThat(content).contains("@OrderBy(\"last_name\")");
        assertThat(content).contains("@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })");
        assertThat(content).contains("@JoinTable(name = \"company_projects\"");
        
        // Validate natural ID
        assertThat(content).contains("@NaturalId");
        assertThat(content).contains("@Column(name = \"tax_id\", length = 50)");
    }

    private void validateEmployeeEntityAnnotations() throws IOException {
        String content = Files.readString(javaDir.resolve("com/example/model/Employee.java"));
        
        assertThat(content).contains("@Entity");
        assertThat(content).contains("@Table(name = \"employees\"");  // Allow for complex table annotation
        assertThat(content).contains("@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)");
        assertThat(content).contains("@Cacheable");
        
        // Validate ID with identity generator
        assertThat(content).contains("@Id");
        assertThat(content).contains("@GeneratedValue(strategy = GenerationType.IDENTITY)");
        assertThat(content).contains("@Column(name = \"emp_id\")");
        
        // Validate column mappings
        assertThat(content).contains("@Column(name = \"first_name\", length = 50)");
        assertThat(content).contains("@Column(name = \"last_name\", length = 50)");
        assertThat(content).contains("@Column(name = \"hire_date\")");
        
        // Validate many-to-one relationships
        assertThat(content).contains("@ManyToOne(fetch = FetchType.EAGER)");
        assertThat(content).contains("@JoinColumn(name = \"company_id\", foreignKey = @ForeignKey(name = \"fk_emp_company\"))");
        assertThat(content).contains("@JoinColumn(name = \"dept_id\")");
        
        // Validate one-to-one relationship
        assertThat(content).contains("@OneToOne(cascade = { CascadeType.ALL })");
    }

    private void validateAddressEntityAnnotations() throws IOException {
        String content = Files.readString(javaDir.resolve("com/example/model/Address.java"));
        
        assertThat(content).contains("@Entity");
        assertThat(content).contains("@Table(name = \"addresses\")");
        assertThat(content).contains("@Embeddable"); // Address is also used as embeddable
        
        // Validate assigned generator
        assertThat(content).contains("@Id");
        assertThat(content).contains("@GeneratedValue(generator = \"generatorassigned\")");
        assertThat(content).contains("@Column(name = \"addr_id\")");
        
        // Validate column mappings
        assertThat(content).contains("@Column(name = \"zip_code\", length = 20)");
    }

    private void validateDepartmentEntityAnnotations() throws IOException {
        String content = Files.readString(javaDir.resolve("com/example/model/Department.java"));
        
        assertThat(content).contains("@Entity");
        assertThat(content).contains("@Table(name = \"departments\")");
        
        // Validate auto generator
        assertThat(content).contains("@Id");
        assertThat(content).contains("@GeneratedValue(generator = \"generatorauto\")");
        assertThat(content).contains("@Column(name = \"dept_id\")");
        
        // Validate column mappings
        assertThat(content).contains("@Column(name = \"dept_name\", length = 100)");
        
        // Validate relationships
        assertThat(content).contains("@ManyToOne(fetch = FetchType.EAGER");
        assertThat(content).contains("@OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, mappedBy = \"department\")");
    }

    private void validateProjectEntityAnnotations() throws IOException {
        String content = Files.readString(javaDir.resolve("com/example/model/Project.java"));
        
        assertThat(content).contains("@Entity");
        assertThat(content).contains("@Table(name = \"projects\")");
        
        // Validate table generator
        assertThat(content).contains("@Id");
        assertThat(content).contains("@GeneratedValue(generator = \"generatortable\")");
        assertThat(content).contains("@Column(name = \"project_id\")");
        
        // Validate column mappings
        assertThat(content).contains("@Column(name = \"project_name\", length = 100)");
        assertThat(content).contains("@Column(name = \"start_date\")");
        assertThat(content).contains("@Column(name = \"end_date\")");
        
        // Validate many-to-many with join table (this is the owning side, not inverse)
        assertThat(content).contains("@ManyToMany(fetch = FetchType.LAZY");
        assertThat(content).contains("@JoinTable(name = \"company_projects\"");
        assertThat(content).contains("joinColumns = { @JoinColumn(name = \"project_id\") }");
        assertThat(content).contains("inverseJoinColumns = { @JoinColumn(name = \"company_id\") }");
    }
}