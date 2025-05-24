# Integration Test Resources

This directory contains resource files used by the `FullConversionIntegrationTest` to validate the complete HBM to JPA conversion process.

## Directory Structure

```
integration-test/
├── README.md                           # This file
├── hbm/
│   └── comprehensive-mapping.hbm.xml   # Complete HBM mapping file
└── java-templates/
    ├── Company.java                    # Main entity template
    ├── PublicCompany.java              # Inheritance subclass template
    ├── Employee.java                   # Employee entity template
    ├── Department.java                 # Department entity template
    ├── Address.java                    # Address entity template (dual-purpose)
    └── Project.java                    # Project entity template
```

## HBM Mapping File

**File:** `hbm/comprehensive-mapping.hbm.xml`

This file contains a comprehensive Hibernate mapping configuration that includes:

### Class-Level Features
- Entity mapping with table, schema, catalog attributes
- Cache configuration (read-write, read-only)
- Dynamic insert/update settings
- Inheritance with discriminator
- Natural ID mapping

### Generator Types
- **Sequence generator** with parameters (Company)
- **Identity generator** (Employee)
- **Auto generator** (Department)
- **Assigned generator** (Address)
- **Table generator** with full configuration (Project)

### Property Mappings
- Basic properties with column attributes (name, length, nullable, unique)
- Type mappings (string, text, date, big_decimal, boolean)
- Precision and scale for decimal types
- Index definitions

### Relationships
- **One-to-Many** with Set collections (cascade, fetch, inverse, order-by)
- **One-to-Many** with List collections (list-index)
- **Many-to-One** with foreign keys
- **One-to-One** with cascade
- **Many-to-Many** with join tables

### Advanced Features
- Component/Embedded objects with attribute overrides
- Inheritance with subclass and discriminator values
- Version fields for optimistic locking

## Java Entity Templates

**Directory:** `java-templates/`

Contains plain Java entity classes (without JPA annotations) that serve as the starting point for the conversion process. These templates include:

### Entity Relationships
- **Company** ↔ **Employee** (One-to-Many / Many-to-One)
- **Company** ↔ **Department** (One-to-Many / Many-to-One)
- **Company** ↔ **Project** (Many-to-Many)
- **Employee** ↔ **Department** (Many-to-One / One-to-Many)
- **Employee** ↔ **Address** (One-to-One)
- **Company** → **Address** (Embedded component)

### Inheritance Hierarchy
- **Company** (base class)
  - **PublicCompany** (subclass)

### Entity Features
Each entity template includes:
- All necessary fields with appropriate Java types
- Complete getter/setter methods
- Proper package declarations
- Required imports for collections and data types

## Usage in Integration Test

The `FullConversionIntegrationTest` uses these resources as follows:

1. **Setup Phase:**
   - Reads the HBM file from `hbm/comprehensive-mapping.hbm.xml`
   - Reads each Java template from `java-templates/`
   - Creates temporary directories and copies files

2. **Conversion Phase:**
   - Runs the `ConversionProcessor` on the HBM file
   - Processes the plain Java entities
   - Generates JPA-annotated versions

3. **Validation Phase:**
   - Verifies that all expected JPA annotations are present
   - Validates annotation attributes and configurations
   - Confirms relationship mappings are correct

## Benefits of External Resource Files

Moving the HBM content and Java templates to external files provides:

1. **Better Maintainability:** Easier to edit and update mapping configurations
2. **Improved Readability:** Test code focuses on logic rather than data
3. **Reusability:** Resources can be used by other tests or tools
4. **Version Control:** Changes to mappings are clearly visible in diffs
5. **IDE Support:** Proper syntax highlighting and validation for XML and Java
6. **Separation of Concerns:** Test logic separated from test data

## Modification Guidelines

When modifying these resource files:

1. **HBM File Changes:**
   - Ensure XML is well-formed and valid
   - Update corresponding Java templates if entity structure changes
   - Consider impact on test assertions

2. **Java Template Changes:**
   - Maintain consistency with HBM mapping
   - Keep templates as plain POJOs (no JPA annotations)
   - Update imports if new types are added

3. **Test Impact:**
   - Review test assertions after making changes
   - Ensure all validation methods still pass
   - Update test documentation if behavior changes