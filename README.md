
# hbm2Java

**hbm2Java** is a Java command-line tool designed to convert Hibernate mapping files (`*.hbm.xml`) in a folder (and all of its subfolders recursively) into Java classes annotated with JPA annotations.
This tool facilitates the migration from legacy Hibernate XML configurations to the modern JPA annotation-based approach, supporting both new file generation and annotation of existing Java classes.

## Features

### Core Functionality
- **HBM Parsing**: Parses Hibernate `*.hbm.xml` files and extracts entity metadata
- **JPA Annotation Generation**: Generates appropriate JPA annotations for entities, fields, relationships, etc.
- **Dual Operation Modes**: 
  - **Generation Mode**: Creates new Java entity files with JPA annotations
  - **Annotation Mode**: Adds JPA annotations to existing Java entity files
- **Batch Processing**: Processes multiple HBM files in a single operation

### Persistence Features Covered
- **Inheritance Support**: Handles entity inheritance with automatic strategy detection (SINGLE_TABLE, JOINED, TABLE_PER_CLASS)
- **Relationship Mapping**: Supports One-to-One, One-to-Many, Many-to-One, and Many-to-Many relationships
- **Embeddable Entities**: Automatically annotates embeddable classes for composite types
- **Attribute Overrides**: Supports customization of column mappings for composite attributes
- **Foreign Key Management**: Configures bidirectional relationships and foreign key mappings
- **Named Queries**: Preserves and converts Hibernate named queries to JPA format
- **Discriminator Columns**: Handles inheritance discriminator configurations

### Technical Features
- **Robust Error Handling**: Comprehensive error reporting and graceful failure recovery
- **Detailed Logging**: Extensive logging for monitoring conversion progress and debugging
- **File Validation**: Input validation and directory structure verification
- **Package Structure Preservation**: Maintains existing package structures when annotating files

## Prerequisites

- **Java Development Kit (JDK) 17 or higher**
- **Apache Maven 3.x** (for building from source)

## Building the Project

1. **Clone the repository**
   ```bash
   git clone https://github.com/marcosene/hbm2Java.git
   cd hbm2Java
   ```

2. **Build the project using Maven**
   ```bash
   mvn clean package
   ```

   Upon successful build, the executable JAR file will be located in the `target` directory as `hbm2java-1.0-SNAPSHOT.jar`.

## Usage

### Command Syntax

```bash
java -jar target/hbm2java-1.0-SNAPSHOT.jar <inputFolder> <outputFolder> [--annotateExisting]
```

### Parameters

| Parameter | Required | Description                                                                   |
|-----------|----------|-------------------------------------------------------------------------------|
| `inputFolder` | Yes | Base folder for searching Hibernate `*.hbm.xml` files recursively             |
| `outputFolder` | Yes | Target directory for generated files or base search folder for existing files |
| `--annotateExisting` | No | Flag to annotate existing Java files instead of generating new ones           |

### Ignoring Duplicated Fields

If you have super-class fields duplicated in multiple inheritors HBM mappings files with the same settings, the tool can ignore them during JPA annotation by creating and configuring an `ignore.properties` file in the `src/main/resources` directory.

This could be `carefully` used in some special case you don't need one or more field to be annotated.

The format for entries in `ignore.properties` is: `simpleClassName={field1,field2,...}`.

For example:
`MyClass=fieldToIgnore,anotherField`

Placeholders can be used to reference other lists of fields. For example:
- `MyFirstClass=fieldToIgnore,anotherField`
- `MySecondClass=oneMoreField,${MyFirstClass}` (where `${MyFirstClass}` will be replaced by `fieldToIgnore,anotherField`)

### Operation Modes

#### 1. Generation Mode (Default)
Creates new Java entity files with JPA annotations:

```bash
java -jar target/hbm2java-1.0-SNAPSHOT.jar ./hbm-files ./src/main/java
```

#### 2. Annotation Mode
Adds JPA annotations to existing Java entity files:

```bash
java -jar target/hbm2java-1.0-SNAPSHOT.jar ./hbm-files ./src/main/java --annotateExisting
```

NOTE: If target class is not found searching recursively from the base input folder, a new entity class will be generated in the root of the output folder.

### Examples

#### Basic Usage - Generate New Files
```bash
# Convert HBM files to new JPA entity files
java -jar target/hbm2java-1.0-SNAPSHOT.jar ./hibernate-mappings ./generated-entities
```

#### Annotate Existing Files
```bash
# Add JPA annotations to existing entity files
java -jar target/hbm2java-1.0-SNAPSHOT.jar ./hibernate-mappings ./src/main/java --annotateExisting
```

## Architecture

The tool follows a modular architecture with clear separation of concerns:

### Core Components

- **`Hbm2Java`**: Main entry point handling command-line arguments and orchestration
- **`ConversionProcessor`**: Core conversion logic coordinator
- **`HbmParser`**: XML parsing and entity model creation
- **`AnnotationBuilder`**: JPA annotation generation
- **`AnnotationApplier`**: Integration of annotations into existing files
- **`EntityGenerator`**: New entity for file generation

### Processing Flow

1. **Input Validation**: Validates command-line arguments and directory structure
2. **File Discovery**: Locates all `*.hbm.xml` files in the input directory recursively
3. **HBM Parsing**: Parses XML files and creates internal entity models
4. **Entity Configuration**: Configures inheritance, relationships, and embeddable settings
5. **Annotation Generation**: Creates appropriate JPA annotations for each entity
6. **Output Generation**: Either generates new files or annotates existing ones

## Supported Hibernate Features

### Entity Mappings
- ✅ Basic entity mappings (`<class>`)
- ✅ Table and column mappings
- ✅ Primary key configurations (simple and composite)
- ✅ Property mappings with various data types

### Inheritance
- ✅ Single Table Inheritance
- ✅ Joined Table Inheritance  
- ✅ Table Per Class Inheritance
- ✅ Discriminator columns and values

### Relationships
- ✅ One-to-One associations
- ✅ One-to-Many associations
- ✅ Many-to-One associations
- ✅ Many-to-Many associations
- ✅ Bidirectional relationship configuration
- ✅ Cascade settings
- ✅ Fetch strategies (LAZY/EAGER)

### Advanced Features
- ✅ Composite primary keys
- ✅ Embeddable components
- ✅ Named queries
- ✅ Secondary tables
- ✅ Join columns and foreign keys

## Hibernate-Specific Annotations

While `hbm2java` primarily focuses on generating standard JPA annotations, it leverages certain Hibernate-specific annotations to map advanced Hibernate features or to provide more precise control over the mapping when a direct JPA equivalent is insufficient. Below are some of the key Hibernate annotations utilized:

### `@org.hibernate.annotations.Table(appliesTo = "...", optional = false)`
- **Purpose**: Used in older Hibernate versions to specify additional options for a secondary table. **Note: This annotation is deprecated in modern Hibernate.**
- **Usage by `hbm2java`**: `hbm2java` might generate this for older HBM files that define secondary table mappings with specific Hibernate configurations not covered by early JPA `@SecondaryTable` versions.
    - `appliesTo`: Specifies the logical name of the secondary table declaration within the HBM.
    - `optional = false`: Corresponds to HBM's `optional="false"` on a `<join>`, indicating that a row must exist in the secondary table for the entity. This influences join types (inner join) and data integrity expectations.
- **Modern JPA Equivalent**: `@jakarta.persistence.SecondaryTable` and `@jakarta.persistence.SecondaryTables` are the standard JPA way to map an entity to multiple tables. `hbm2java` will prefer these where possible.

### `@org.hibernate.annotations.Cache(usage = ...)`
- **Purpose**: Configures the second-level caching strategy for an entity or collection.
- **Usage by `hbm2java`**: Translates caching configurations from HBM files (`<cache usage="..."/>`) into this annotation.
    - `usage`: Specifies the `org.hibernate.annotations.CacheConcurrencyStrategy` (e.g., `READ_ONLY`, `READ_WRITE`, `NONSTRICT_READ_WRITE`, `TRANSACTIONAL`). This dictates how concurrent access to cached items is managed.
    - `region` (optional): Specifies a custom cache region name.
    - `include` (optional): Determines if lazy-loaded properties are also cached (e.g., "all" or "non-lazy").
- **Technical Detail**: This annotation allows fine-grained control over how entities are stored and retrieved from the L2 cache, impacting performance and data consistency in multi-user environments.

### `@org.hibernate.annotations.Immutable`
- **Purpose**: Marks an entity or a collection as immutable. This means its state (excluding the identifier) will not change after it's created.
- **Usage by `hbm2java`**: Applied if the HBM mapping indicates that an entity or collection is immutable (`mutable="false"`).
- **Technical Detail**: Hibernate can perform certain optimizations for immutable entities/collections, such as skipping dirty checks during flush, which can improve performance. It's crucial that the application does not attempt to modify such an entity/collection post-creation.

### `@org.hibernate.annotations.DynamicInsert`
- **Purpose**: Specifies that SQL `INSERT` statements generated by Hibernate should only include columns whose corresponding properties are not null at the time of insertion.
- **Usage by `hbm2java`**: Applied if the HBM mapping specifies `dynamic-insert="true"`.
- **Technical Detail**: This can be useful for tables with many columns that have database-defined default values or are nullable. It makes the generated `INSERT` statements cleaner and can sometimes improve performance by reducing the amount of data sent to the database.

### `@org.hibernate.annotations.DynamicUpdate`
- **Purpose**: Specifies that SQL `UPDATE` statements generated by Hibernate should only include columns whose corresponding properties have actually changed.
- **Usage by `hbm2java`**: Applied if the HBM mapping specifies `dynamic-update="true"`.
- **Technical Detail**: This can optimize `UPDATE` operations, especially for tables with many columns or when used with optimistic locking strategies that check all columns. It ensures that only modified data is sent, potentially reducing network traffic and avoiding unnecessary updates on unchanged columns.

### `@org.hibernate.annotations.Proxy`
- **Purpose**: Indicates whether a class should be proxied by Hibernate at runtime to support lazy loading of the entity instance.
- **Usage by `hbm2java`**: Applied if the HBM mapping specifies `lazy="true"` at the <class> level.
- **Technical Detail**: When enabled (`@Proxy(lazy = true)`), Hibernate will generate a subclass proxy for the entity, allowing the actual data to be loaded only when accessed. This supports performance optimization by deferring database access until necessary. For lazy loading to work properly, the class must be non-final, have a no-arg constructor, and rely on getter methods for property access. Disabling the proxy (`@Proxy(lazy = false)`) forces eager loading of the entity.

### `@org.hibernate.annotations.GenericGenerator(...)`
- **Purpose**: Provides a flexible way to define custom or extended identifier generation strategies beyond standard JPA generators. **Note: This annotation is superseded by `@org.hibernate.annotations.IdGeneratorType` in modern Hibernate, but remains relevant for HBM conversion.**
- **Usage by `hbm2java`**: Used for mapping Hibernate-specific ID generation strategies like `seqhilo` or `foreign`.
    - `name`: The logical name of the generator.
    - `strategy`: The fully qualified class name of the generator implementation (e.g., `org.hibernate.id.enhanced.SequenceStyleGenerator`, `org.hibernate.id.ForeignGenerator`).
    - `parameters`: An array of `@org.hibernate.annotations.Parameter` annotations to configure the generator (e.g., `sequence_name` for sequence-based generators, `property` for foreign generators).
- **Technical Detail**: This allows `hbm2java` to preserve sophisticated ID generation logic from HBM files that don't map directly to standard JPA `@GeneratedValue` strategies.

### `@org.hibernate.annotations.Parameter(...)`
- **Purpose**: Used within `@GenericGenerator` (and other Hibernate extension annotations) to provide key-value configuration parameters to the underlying strategy implementation.
- **Usage by `hbm2java`**: Specifies configuration details for custom ID generators, type definitions, etc.
    - `name`: The name of the parameter.
    - `value`: The value of the parameter.

### `@org.hibernate.annotations.NaturalId(mutable = ...)`
- **Purpose**: Designates one or more properties as part of the entity's natural identifier (a key that has business meaning, unlike a surrogate primary key).
- **Usage by `hbm2java`**: Applied to properties that form the natural ID, based on HBM mappings like `<natural-id>`.
    - `mutable`: A boolean (defaults to `false`) indicating whether the natural ID properties can change after the entity is persisted. If `true`, Hibernate needs to do extra work to manage changes to natural IDs in the session and second-level cache.
- **Technical Detail**: Hibernate provides an API (`Session.byNaturalId()`) for efficiently loading entities by their natural ID. Marking natural IDs also influences how Hibernate might cache these entities.

### `@org.hibernate.annotations.OptimisticLock(excluded = ...)`
- **Purpose**: Controls whether a specific property participates in optimistic locking checks.
- **Usage by `hbm2java`**: Typically used with `excluded = true` if an HBM mapping indicates that a property should not cause a version increment or be checked during optimistic locking.
    - `excluded = true`: The property is not included in optimistic lock version checks. Modifications to this property alone will not increment the `@Version` field.
- **Technical Detail**: This is useful for properties that might change frequently but whose changes don't signify a conflicting update from a business perspective (e.g., a last-access timestamp).

### `@org.hibernate.annotations.Type(type = "...")`
- **Purpose**: Specifies a custom Hibernate `UserType` or a named Hibernate type to be used for mapping a property to a database column. This is used when the default JPA or Hibernate type mapping is insufficient.
- **Usage by `hbm2java`**: `hbm2java` (specifically the `AnnotationApplier` part) may generate this annotation if the HBM file defines a custom Hibernate type or a type that requires specific parameters (e.g., custom enum mappings using `enumClass` parameter).
    - `type`: The name of the Hibernate type (e.g., "yes_no", "true_false", or a fully qualified class name of a `UserType` implementation).
    - `parameters` (optional): An array of `@org.hibernate.annotations.Parameter` to configure the type (e.g., specifying the `enumClass` for a parameterized enum type).
- **Technical Detail**: `@Type` allows for highly customized serialization/deserialization logic, custom SQL type mapping, and handling of non-standard Java types. For example, mapping a Java `enum` to a `CHAR` column with specific character values ('Y'/'N') instead of ordinal or string name.

## Output Examples

### Generated Entity Example
```java
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    
    @Column(name = "username", nullable = false, length = 50)
    private String username;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;
    
    // Getters and setters...
}
```

### Inheritance Example
```java
@Entity
@Table(name = "vehicles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "vehicle_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Vehicle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "brand")
    private String brand;
    
    // Common properties...
}

@Entity
@DiscriminatorValue("CAR")
public class Car extends Vehicle {
    
    @Column(name = "doors")
    private Integer doors;
    
    // Car-specific properties...
}
```

## Logging and Monitoring

The tool provides comprehensive logging at different levels:

- **INFO**: High-level progress information
- **DEBUG**: Detailed processing information
- **ERROR**: Error conditions and stack traces
- **WARN**: Non-fatal issues and warnings

### Log Configuration
Logging is configured via `log4j2.xml` in the resources directory. You can adjust log levels and output formats as needed.

## Troubleshooting

### Common Issues

#### No HBM Files Found
```
WARN - No .hbm.xml files found in: /path/to/input
```
**Solution**: Verify the input directory (or any subfolder) contains `*.hbm.xml` files and the path is correct.

#### Parsing Errors
```
ERROR - Error parsing HBM file: /path/to/file.hbm.xml
```
**Solution**: Check the HBM file for XML syntax errors or unsupported Hibernate features.

#### Output Directory Issues
```
ERROR - Failed to create or validate output folder: /path/to/output
```
**Solution**: Ensure the output directory is writable and the path is valid.

#### Entity Processing Failures
```
ERROR - Error processing entity 'EntityName'
```
**Solution**: Check the logs for specific error details and verify the entity configuration in the HBM file.

### Performance Considerations

- **Large Projects**: For projects with many HBM files, consider processing in batches
- **Memory Usage**: The tool loads all entities into memory; ensure adequate heap space for large projects
- **File I/O**: Processing speed depends on disk I/O performance, especially when annotating existing files

