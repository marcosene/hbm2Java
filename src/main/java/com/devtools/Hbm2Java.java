package com.devtools;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.devtools.model.jpa.JpaBase;
import com.devtools.model.jpa.JpaColumn;
import com.devtools.model.jpa.JpaEntity;
import com.devtools.model.jpa.JpaRelationship;
import com.devtools.processing.AnnotationApplier;
import com.devtools.processing.AnnotationBuilder;
import com.devtools.processing.EntityGenerator;
import com.devtools.processing.HbmParser;
import com.devtools.utils.ClassNameUtils;
import com.devtools.utils.FileUtils;

/**
 * The {@code Hbm2Java} class facilitates the conversion of Hibernate HBM XML files into
 * JPA annotations through a three-step process:
 * <ol>
 *   <li>Parsing the XML file into a {@link JpaBase} model.</li>
 *   <li>Generating appropriate JPA annotations within the same model.</li>
 *   <li>Applying annotations to existing Java classes or generating new entity class files.</li>
 * </ol>
 *
 * <p>This class processes all `.hbm.xml` files in a specified input directory, extracting
 * metadata and generating annotations accordingly. The annotated entities are then either
 * integrated into existing Java files or stored in the designated output directory.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   java Hbm2Java /path/to/inputFolder /path/to/baseOutputFolder [--annotateExisting]
 * </pre>
 * <p>Where inputFolder is the folder containing all *hbm.xml files
 *     and baseOutputFolder is the the base folder used to search for existing files (when --annotateExisting) or
 *     output folder when generating new ones</p>
 * <p>If the optional {@code --annotateExisting} parameter is provided, annotations will be
 * added to existing Java entity classes instead of generating new ones.</p>
 *
 * <p>Key components involved:</p>
 * <ul>
 *   <li>{@link HbmParser} - Parses HBM XML files into JPA model objects.</li>
 *   <li>{@link AnnotationBuilder} - Generates annotations for parsed entities.</li>
 *   <li>{@link AnnotationApplier} - Integrates annotations into existing files</li>
 *   <li>{@link EntityGenerator} - or generates new entity classes.</li>
 * </ul>
 *
 * <p>Logging must be checked to track progress, and error handling ensures robustness against
 * invalid input files or processing failures.</p>
 */
public class Hbm2Java {

    private static final Log LOG = LogFactory.getLog(Hbm2Java.class);
    
    // Constants
    private static final String ANNOTATE_EXISTING_FLAG = "--annotateExisting";
    private static final String HBM_FILE_EXTENSION = ".hbm.xml";
    private static final int MIN_REQUIRED_ARGS = 2;
    private static final int MAX_ARGS = 3;

    public static void main(final String[] args) {
        try {
            validateArguments(args);
            
            final String inputFolder = args[0];
            final String outputFolder = args[1];
            final boolean annotateExisting = args.length > 2 && ANNOTATE_EXISTING_FLAG.equals(args[2]);

            LOG.info("Starting HBM to Java conversion...");
            LOG.info("Input folder: " + inputFolder);
            LOG.info("Output folder: " + outputFolder);
            LOG.info("Mode: " + (annotateExisting ? "Annotate existing files" : "Generate new files"));

            processConversion(inputFolder, outputFolder, annotateExisting);
            
            LOG.info("HBM to Java conversion completed successfully.");
            
        } catch (final IllegalArgumentException e) {
            LOG.error("Invalid arguments: " + e.getMessage());
            printUsage();
            System.exit(1);
        } catch (final Exception e) {
            LOG.error("Unexpected error during conversion", e);
            System.exit(1);
        }
    }

    private static void validateArguments(final String[] args) {
        if (args == null || args.length < MIN_REQUIRED_ARGS || args.length > MAX_ARGS) {
            throw new IllegalArgumentException("Invalid number of arguments. Expected 2-3 arguments, got: " + 
                (args == null ? 0 : args.length));
        }

        if (StringUtils.isBlank(args[0])) {
            throw new IllegalArgumentException("Input folder cannot be empty");
        }

        if (StringUtils.isBlank(args[1])) {
            throw new IllegalArgumentException("Output folder cannot be empty");
        }

        if (args.length == MAX_ARGS && !ANNOTATE_EXISTING_FLAG.equals(args[2])) {
            throw new IllegalArgumentException("Invalid third argument. Expected '" + ANNOTATE_EXISTING_FLAG + 
                "' but got: " + args[2]);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java Hbm2Java <inputFolder> <outputFolder> [" + ANNOTATE_EXISTING_FLAG + "]");
        System.err.println("  inputFolder: Directory containing *" + HBM_FILE_EXTENSION + " files");
        System.err.println("  outputFolder: Directory for output (base search folder when annotating existing files)");
        System.err.println("  " + ANNOTATE_EXISTING_FLAG + ": Optional flag to annotate existing Java files instead of generating new ones");
    }

    private static void processConversion(final String inputFolder, final String outputFolder, 
            final boolean annotateExisting) {
        
        if (FileUtils.createDirectories(outputFolder)) {
            throw new RuntimeException("Failed to create or validate output folder: " + outputFolder);
        }

        final File inputDir = validateInputDirectory(inputFolder);
        final File[] hbmFiles = findHbmFiles(inputDir, inputFolder);
        
        if (hbmFiles.length == 0) {
            LOG.warn("No " + HBM_FILE_EXTENSION + " files found in: " + inputFolder);
            return;
        }

        LOG.info("Found " + hbmFiles.length + " HBM files to process");

        final Map<String, JpaEntity> jpaEntityMap = parseHbmFiles(hbmFiles);
        
        if (jpaEntityMap.isEmpty()) {
            LOG.warn("No entities were successfully parsed from HBM files");
            return;
        }

        LOG.info("Successfully parsed " + jpaEntityMap.size() + " entities");

        configureEntitySettings(jpaEntityMap);
        generateOrAnnotateEntities(jpaEntityMap, outputFolder, annotateExisting);
    }

    private static File validateInputDirectory(final String inputFolder) {
        final File inputDir = new File(inputFolder);
        
        if (!inputDir.exists()) {
            throw new IllegalArgumentException("Input folder does not exist: " + inputFolder);
        }
        
        if (!inputDir.isDirectory()) {
            throw new IllegalArgumentException("Input path is not a directory: " + inputFolder);
        }
        
        return inputDir;
    }

    private static File[] findHbmFiles(final File inputDir, final String inputFolder) {
        final File[] files = inputDir.listFiles((dir, name) -> name.endsWith(HBM_FILE_EXTENSION));
        
        if (files == null) {
            throw new RuntimeException("Unable to list files in directory: " + inputFolder);
        }
        
        return files;
    }

    private static Map<String, JpaEntity> parseHbmFiles(final File[] hbmFiles) {
        final Map<String, JpaEntity> jpaEntityMap = new TreeMap<>();
        final HbmParser hbmParser = new HbmParser();
        
        for (final File hbmFile : hbmFiles) {
            final String hbmFilePath = hbmFile.getAbsolutePath();
            LOG.info("Parsing HBM file: " + hbmFilePath);

            try {
                final JpaBase jpaBase = hbmParser.parse(hbmFilePath);

                if (jpaBase == null) {
                    LOG.error("Failed to parse HBM file: " + hbmFilePath);
                    continue;
                }

                final int entitiesCount = jpaBase.getEntities().size();
                jpaBase.getEntities().forEach(entity -> jpaEntityMap.put(entity.getName(), entity));
                LOG.debug("Extracted " + entitiesCount + " entities from: " + hbmFile.getName());
                
            } catch (final Exception e) {
                LOG.error("Error parsing HBM file: " + hbmFilePath, e);
            }
        }
        
        return jpaEntityMap;
    }

    private static void configureEntitySettings(final Map<String, JpaEntity> jpaEntityMap) {
        LOG.info("Configuring entity inheritance, relationships, and embeddable settings...");
        checkAdditionalSettings(jpaEntityMap);
    }

    private static void generateOrAnnotateEntities(final Map<String, JpaEntity> jpaEntityMap, 
                                                 final String outputFolder, final boolean annotateExisting) {
        
        final AnnotationBuilder annotationBuilder = new AnnotationBuilder(outputFolder);
        final AnnotationApplier annotationApplier = new AnnotationApplier();
        final EntityGenerator entityGenerator = new EntityGenerator();
        
        int successCount = 0;
        int errorCount = 0;

        for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
            final String entityName = jpaEntity.getName();
            LOG.info("Processing entity: " + entityName);
            
            try {
                annotationBuilder.build(jpaEntity);

                if (annotateExisting) {
                    annotationApplier.replace(jpaEntity, outputFolder);
                    LOG.debug("Successfully annotated existing entity: " + entityName);
                } else {
                    entityGenerator.generate(jpaEntity, outputFolder);
                    LOG.debug("Successfully generated new entity: " + entityName);
                }
                
                successCount++;
                
            } catch (final Exception e) {
                LOG.error("Error processing entity '" + entityName + "' (class: " + 
                    jpaEntity.getFullParentClass() + ")", e);
                errorCount++;
            }
        }

        LOG.info("Entity processing completed. Success: " + successCount + ", Errors: " + errorCount);
    }

    private static void checkAdditionalSettings(final Map<String, JpaEntity> jpaEntityMap) {
        // Process inheritance settings for entities with parent classes
        configureInheritanceSettings(jpaEntityMap);
        
        // Process foreign key inverse relationships
        configureForeignKeyRelationships(jpaEntityMap);
        
        // Process embeddable settings for composite columns
        configureEmbeddableSettings(jpaEntityMap);
    }

    private static void configureInheritanceSettings(final Map<String, JpaEntity> jpaEntityMap) {
        for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
            final String parentClassName = jpaEntity.getParentClass();
            
            if (parentClassName == null) {
                continue;
            }
            
            setInheritanceOnParentClass(jpaEntityMap, jpaEntity);
        }
    }

    private static void configureForeignKeyRelationships(final Map<String, JpaEntity> jpaEntityMap) {
        for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
            setForeignKeyInverseRelationship(jpaEntityMap, jpaEntity);
        }
    }

    private static void configureEmbeddableSettings(final Map<String, JpaEntity> jpaEntityMap) {
        // Create a copy of the values to avoid ConcurrentModificationException
        for (final JpaEntity jpaEntity : new HashSet<>(jpaEntityMap.values())) {
            processCompositeColumns(jpaEntityMap, jpaEntity.getColumns());
            
            for (final JpaRelationship jpaRelationship : jpaEntity.getRelationships()) {
                processCompositeColumns(jpaEntityMap, jpaRelationship.getReferencedColumns());
            }
        }
    }

    private static void setInheritanceOnParentClass(final Map<String, JpaEntity> jpaEntityMap, final JpaEntity jpaEntity) {
        final JpaEntity parentEntity = jpaEntityMap.get(jpaEntity.getParentClass());
        
        if (parentEntity == null || parentEntity.getDiscriminator() == null || 
            parentEntity.getDiscriminator().getColumn() == null) {
            return;
        }
        
        final String inheritanceStrategy = determineInheritanceStrategy(jpaEntity, parentEntity);
        if (inheritanceStrategy != null) {
            parentEntity.setInheritance(inheritanceStrategy);
        }
    }

    private static String determineInheritanceStrategy(final JpaEntity childEntity, final JpaEntity parentEntity) {
        if (childEntity.isSecondTable()) {
            return "JOINED";
        }
        
        if (StringUtils.isNotBlank(childEntity.getTable())) {
            return "TABLE_PER_CLASS";
        }
        
        // Default to SINGLE_TABLE if no inheritance strategy is already set
        return parentEntity.getInheritance() == null ? "SINGLE_TABLE" : null;
    }

    private static void setForeignKeyInverseRelationship(final Map<String, JpaEntity> jpaEntityMap, final JpaEntity jpaEntity) {
        for (final JpaRelationship jpaRelationship : jpaEntity.getRelationships()) {
            if (!isOneToManyWithForeignKey(jpaRelationship)) {
                continue;
            }
            
            final String foreignKey = jpaRelationship.getReferencedColumns().get(0).getForeignKey();
            final JpaEntity inverseEntity = jpaEntityMap.get(ClassNameUtils.getSimpleClassName(jpaRelationship.getReturnType()));
            
            if (inverseEntity != null) {
                updateInverseRelationshipForeignKey(inverseEntity, jpaEntity, foreignKey);
            }
        }
    }

    private static boolean isOneToManyWithForeignKey(final JpaRelationship jpaRelationship) {
        return JpaRelationship.Type.OneToMany.equals(jpaRelationship.getRelationshipType()) &&
               !jpaRelationship.getReferencedColumns().isEmpty() &&
               StringUtils.isNotBlank(jpaRelationship.getReferencedColumns().get(0).getForeignKey());
    }

    private static void updateInverseRelationshipForeignKey(final JpaEntity inverseEntity, final JpaEntity jpaEntity, 
                                                          final String foreignKey) {
        for (final JpaRelationship inverseRelationship : inverseEntity.getRelationships()) {
            if (JpaRelationship.Type.ManyToOne.equals(inverseRelationship.getRelationshipType()) &&
                inverseRelationship.getReturnType().equals(jpaEntity.getType())) {
                
                if (!inverseRelationship.getReferencedColumns().isEmpty()) {
                    inverseRelationship.getReferencedColumns().get(0).setForeignKey(foreignKey);
                    LOG.debug("Updated foreign key for inverse relationship: " + foreignKey);
                }
                break;
            }
        }
    }

    private static void processCompositeColumns(final Map<String, JpaEntity> jpaEntityMap, 
                                              final List<JpaColumn> columns) {
        if (columns == null) {
            return;
        }
        
        for (final JpaColumn jpaColumn : columns) {
            if (jpaColumn.isComposite()) {
                createOrGetEmbeddableEntity(jpaEntityMap, jpaColumn.getReturnType());
            }
        }
    }

    private static void createOrGetEmbeddableEntity(final Map<String, JpaEntity> jpaEntityMap, 
                                                  final String columnType) {
        final String embeddableClassName = ClassNameUtils.getSimpleClassName(columnType);
        
        if (jpaEntityMap.containsKey(embeddableClassName)) {
            return; // Already exists
        }
        
        final JpaEntity embeddableEntity = new JpaEntity();
        embeddableEntity.setName(embeddableClassName);
        embeddableEntity.setEmbeddable(true);
        jpaEntityMap.put(embeddableClassName, embeddableEntity);
        
        LOG.debug("Created embeddable entity: " + embeddableClassName);
    }
}
