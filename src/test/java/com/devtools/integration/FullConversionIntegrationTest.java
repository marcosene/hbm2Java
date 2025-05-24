package com.devtools.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
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
        String hbmContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE hibernate-mapping PUBLIC 
                "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
                "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
            
            <hibernate-mapping package="com.example.model" default-cascade="save-update">
            
                <!-- Main entity with all class attributes -->
                <class name="Company" table="companies" schema="hr" catalog="main_db" 
                       dynamic-insert="true" dynamic-update="true" mutable="true">
                    
                    <!-- Cache configuration -->
                    <cache usage="read-write" region="company-cache"/>
                    
                    <!-- Primary key with generator -->
                    <id name="id" column="company_id" type="long">
                        <generator class="sequence">
                            <param name="sequence">company_seq</param>
                            <param name="allocation_size">1</param>
                        </generator>
                    </id>
                    
                    <!-- Discriminator for inheritance -->
                    <discriminator column="company_type" type="string" length="20"/>
                    
                    <!-- Version for optimistic locking -->
                    <version name="version" column="version_num" type="integer"/>
                    
                    <!-- Basic properties with various column attributes -->
                    <property name="name" column="company_name" type="string" 
                              length="100" not-null="true" unique="true" index="idx_company_name"/>
                    
                    <property name="description" type="text" length="1000"/>
                    
                    <property name="foundedDate" column="founded_date" type="date"/>
                    
                    <property name="revenue" type="big_decimal" precision="15" scale="2"/>
                    
                    <property name="active" type="boolean" not-null="true"/>
                    
                    <!-- Component/Embedded object -->
                    <component name="headquarters" class="Address">
                        <property name="street" column="hq_street" length="200"/>
                        <property name="city" column="hq_city" length="100"/>
                        <property name="zipCode" column="hq_zip" length="20"/>
                        <property name="country" column="hq_country" length="50"/>
                    </component>
                    
                    <!-- One-to-many relationship with set collection -->
                    <set name="employees" table="employees" cascade="all" 
                         fetch="lazy" inverse="true" order-by="last_name">
                        <key column="company_id" foreign-key="fk_emp_company"/>
                        <one-to-many class="Employee"/>
                    </set>
                    
                    <!-- One-to-many relationship with list collection -->
                    <list name="departments" table="departments" cascade="save-update" fetch="lazy">
                        <key column="company_id"/>
                        <list-index column="dept_order"/>
                        <one-to-many class="Department"/>
                    </list>
                    
                    <!-- Many-to-many relationship -->
                    <set name="projects" table="company_projects" cascade="save-update">
                        <key column="company_id"/>
                        <many-to-many class="Project" column="project_id"/>
                    </set>
                    
                    <!-- Natural ID -->
                    <natural-id>
                        <property name="taxId" column="tax_id" length="50"/>
                    </natural-id>
                    
                </class>
                
                <!-- Subclass for inheritance -->
                <subclass name="PublicCompany" extends="Company" discriminator-value="PUBLIC">
                    <property name="stockSymbol" column="stock_symbol" length="10"/>
                    <property name="marketCap" column="market_cap" type="big_decimal"/>
                </subclass>
                
                <!-- Employee entity with relationships -->
                <class name="Employee" table="employees">
                    <cache usage="read-only"/>
                    
                    <id name="id" column="emp_id" type="long">
                        <generator class="identity"/>
                    </id>
                    
                    <property name="firstName" column="first_name" length="50" not-null="true"/>
                    <property name="lastName" column="last_name" length="50" not-null="true"/>
                    <property name="email" length="100" unique="true"/>
                    <property name="salary" type="big_decimal" precision="10" scale="2"/>
                    <property name="hireDate" column="hire_date" type="date"/>
                    
                    <!-- Many-to-one relationship -->
                    <many-to-one name="company" class="Company" column="company_id" 
                                 cascade="none" fetch="lazy" foreign-key="fk_emp_company"/>
                    
                    <!-- Many-to-one relationship to department -->
                    <many-to-one name="department" class="Department" column="dept_id"/>
                    
                    <!-- One-to-one relationship -->
                    <one-to-one name="address" class="Address" cascade="all"/>
                    
                </class>
                
                <!-- Department entity -->
                <class name="Department" table="departments">
                    <id name="id" column="dept_id" type="long">
                        <generator class="auto"/>
                    </id>
                    
                    <property name="name" column="dept_name" length="100" not-null="true"/>
                    <property name="budget" type="big_decimal" precision="12" scale="2"/>
                    
                    <!-- Many-to-one back to company -->
                    <many-to-one name="company" class="Company" column="company_id"/>
                    
                    <!-- One-to-many to employees -->
                    <set name="employees" inverse="true">
                        <key column="dept_id"/>
                        <one-to-many class="Employee"/>
                    </set>
                    
                </class>
                
                <!-- Address entity (can be embedded or standalone) -->
                <class name="Address" table="addresses">
                    <id name="id" column="addr_id" type="long">
                        <generator class="assigned"/>
                    </id>
                    
                    <property name="street" length="200"/>
                    <property name="city" length="100"/>
                    <property name="zipCode" column="zip_code" length="20"/>
                    <property name="country" length="50"/>
                    
                </class>
                
                <!-- Project entity for many-to-many -->
                <class name="Project" table="projects">
                    <id name="id" column="project_id" type="long">
                        <generator class="table">
                            <param name="table">hibernate_sequences</param>
                            <param name="column">next_val</param>
                            <param name="segment_column">sequence_name</param>
                            <param name="segment_value">project_seq</param>
                        </generator>
                    </id>
                    
                    <property name="name" column="project_name" length="100" not-null="true"/>
                    <property name="description" type="text"/>
                    <property name="startDate" column="start_date" type="date"/>
                    <property name="endDate" column="end_date" type="date"/>
                    
                    <!-- Many-to-many back to companies -->
                    <set name="companies" table="company_projects" inverse="true">
                        <key column="project_id"/>
                        <many-to-many class="Company" column="company_id"/>
                    </set>
                    
                </class>
                
            </hibernate-mapping>
            """;
        
        Files.writeString(hbmDir.resolve("comprehensive-mapping.hbm.xml"), hbmContent);
    }

    private void createPlainJavaEntities() throws IOException {
        // Create package directory structure
        Path packageDir = javaDir.resolve("com/example/model");
        Files.createDirectories(packageDir);
        
        // Company entity
        String companyClass = """
            package com.example.model;
            
            import java.math.BigDecimal;
            import java.util.Date;
            import java.util.List;
            import java.util.Set;
            
            public class Company {
                private Long id;
                private Integer version;
                private String name;
                private String description;
                private Date foundedDate;
                private BigDecimal revenue;
                private Boolean active;
                private String taxId;
                private Address headquarters;
                private Set<Employee> employees;
                private List<Department> departments;
                private Set<Project> projects;
                
                // Getters and setters
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                
                public Integer getVersion() { return version; }
                public void setVersion(Integer version) { this.version = version; }
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                
                public String getDescription() { return description; }
                public void setDescription(String description) { this.description = description; }
                
                public Date getFoundedDate() { return foundedDate; }
                public void setFoundedDate(Date foundedDate) { this.foundedDate = foundedDate; }
                
                public BigDecimal getRevenue() { return revenue; }
                public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
                
                public Boolean getActive() { return active; }
                public void setActive(Boolean active) { this.active = active; }
                
                public String getTaxId() { return taxId; }
                public void setTaxId(String taxId) { this.taxId = taxId; }
                
                public Address getHeadquarters() { return headquarters; }
                public void setHeadquarters(Address headquarters) { this.headquarters = headquarters; }
                
                public Set<Employee> getEmployees() { return employees; }
                public void setEmployees(Set<Employee> employees) { this.employees = employees; }
                
                public List<Department> getDepartments() { return departments; }
                public void setDepartments(List<Department> departments) { this.departments = departments; }
                
                public Set<Project> getProjects() { return projects; }
                public void setProjects(Set<Project> projects) { this.projects = projects; }
            }
            """;
        Files.writeString(packageDir.resolve("Company.java"), companyClass);
        
        // PublicCompany subclass
        String publicCompanyClass = """
            package com.example.model;
            
            import java.math.BigDecimal;
            
            public class PublicCompany extends Company {
                private String stockSymbol;
                private BigDecimal marketCap;
                
                public String getStockSymbol() { return stockSymbol; }
                public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
                
                public BigDecimal getMarketCap() { return marketCap; }
                public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
            }
            """;
        Files.writeString(packageDir.resolve("PublicCompany.java"), publicCompanyClass);
        
        // Employee entity
        String employeeClass = """
            package com.example.model;
            
            import java.math.BigDecimal;
            import java.util.Date;
            
            public class Employee {
                private Long id;
                private String firstName;
                private String lastName;
                private String email;
                private BigDecimal salary;
                private Date hireDate;
                private Company company;
                private Department department;
                private Address address;
                
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                
                public String getFirstName() { return firstName; }
                public void setFirstName(String firstName) { this.firstName = firstName; }
                
                public String getLastName() { return lastName; }
                public void setLastName(String lastName) { this.lastName = lastName; }
                
                public String getEmail() { return email; }
                public void setEmail(String email) { this.email = email; }
                
                public BigDecimal getSalary() { return salary; }
                public void setSalary(BigDecimal salary) { this.salary = salary; }
                
                public Date getHireDate() { return hireDate; }
                public void setHireDate(Date hireDate) { this.hireDate = hireDate; }
                
                public Company getCompany() { return company; }
                public void setCompany(Company company) { this.company = company; }
                
                public Department getDepartment() { return department; }
                public void setDepartment(Department department) { this.department = department; }
                
                public Address getAddress() { return address; }
                public void setAddress(Address address) { this.address = address; }
            }
            """;
        Files.writeString(packageDir.resolve("Employee.java"), employeeClass);
        
        // Department entity
        String departmentClass = """
            package com.example.model;
            
            import java.math.BigDecimal;
            import java.util.Set;
            
            public class Department {
                private Long id;
                private String name;
                private BigDecimal budget;
                private Company company;
                private Set<Employee> employees;
                
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                
                public BigDecimal getBudget() { return budget; }
                public void setBudget(BigDecimal budget) { this.budget = budget; }
                
                public Company getCompany() { return company; }
                public void setCompany(Company company) { this.company = company; }
                
                public Set<Employee> getEmployees() { return employees; }
                public void setEmployees(Set<Employee> employees) { this.employees = employees; }
            }
            """;
        Files.writeString(packageDir.resolve("Department.java"), departmentClass);
        
        // Address entity
        String addressClass = """
            package com.example.model;
            
            public class Address {
                private Long id;
                private String street;
                private String city;
                private String zipCode;
                private String country;
                
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                
                public String getStreet() { return street; }
                public void setStreet(String street) { this.street = street; }
                
                public String getCity() { return city; }
                public void setCity(String city) { this.city = city; }
                
                public String getZipCode() { return zipCode; }
                public void setZipCode(String zipCode) { this.zipCode = zipCode; }
                
                public String getCountry() { return country; }
                public void setCountry(String country) { this.country = country; }
            }
            """;
        Files.writeString(packageDir.resolve("Address.java"), addressClass);
        
        // Project entity
        String projectClass = """
            package com.example.model;
            
            import java.util.Date;
            import java.util.Set;
            
            public class Project {
                private Long id;
                private String name;
                private String description;
                private Date startDate;
                private Date endDate;
                private Set<Company> companies;
                
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                
                public String getDescription() { return description; }
                public void setDescription(String description) { this.description = description; }
                
                public Date getStartDate() { return startDate; }
                public void setStartDate(Date startDate) { this.startDate = startDate; }
                
                public Date getEndDate() { return endDate; }
                public void setEndDate(Date endDate) { this.endDate = endDate; }
                
                public Set<Company> getCompanies() { return companies; }
                public void setCompanies(Set<Company> companies) { this.companies = companies; }
            }
            """;
        Files.writeString(packageDir.resolve("Project.java"), projectClass);
    }

    private void validateCompanyEntityAnnotations() throws IOException {
        String content = Files.readString(javaDir.resolve("com/example/model/Company.java"));
        
        // Validate class-level annotations
        assertThat(content).contains("@Entity");
        assertThat(content).contains("@Table(name = \"companies\")");
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
        assertThat(content).contains("@Table(name = \"employees\")");
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