
# hbm2Java

**hbm2Java** is a Java command-line tool designed to convert Hibernate mapping files (`*.hbm.xml`) into Java classes annotated with JPA annotations. This facilitates the migration from Hibernate XML configurations to the more modern JPA standard.

## Features

- Parses Hibernate `*.hbm.xml` files.
- Generates corresponding Java classes with appropriate JPA annotations.
- Simplifies the transition from Hibernate XML mappings to JPA.

## Prerequisites

- Java Development Kit (JDK) 8 or higher.
- Apache Maven 3.x.

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

   Upon successful build, the executable JAR file will be located in the `target` directory.

## Usage

The tool requires two arguments:

1. **Input Folder**: Path to the directory containing Hibernate `*.hbm.xml` files.
2. **Output Folder**: Path to the directory where the generated JPA-annotated Java classes will be saved.

### Command Syntax

```bash
java -jar target/hbm2Java.jar <inputFolder> <outputFolder>
```

### Example

Assuming you have Hibernate mapping files in the `./hbm` directory and wish to generate Java classes in the `./src/main/java` directory:

```bash
java -jar target/hbm2Java.jar ./hbm ./src/main/java
```

This command will process all `.hbm.xml` files in the `./hbm` directory and generate corresponding Java classes with JPA annotations in the `./src/main/java` directory.
