package com.devtools.processing;

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
import com.devtools.utils.ClassNameUtils;
import com.devtools.utils.FileUtils;

/**
 * The {@code ConversionProcessor} class handles the core conversion logic for transforming
 * Hibernate HBM XML files into JPA-annotated Java entities.
 * 
 * <p>This class orchestrates the entire conversion process including:</p>
 * <ul>
 *   <li>File discovery and validation</li>
 *   <li>HBM parsing coordination</li>
 *   <li>Entity configuration (inheritance, relationships, embeddables)</li>
 *   <li>Generation or annotation of Java entities</li>
 * </ul>
 * 
 * <p>The processor supports two modes of operation:</p>
 * <ul>
 *   <li><strong>Generation mode</strong>: Creates new Java entity files</li>
 *   <li><strong>Annotation mode</strong>: Adds JPA annotations to existing Java files</li>
 * </ul>
 */
public class ConversionProcessor {

    private static final Log LOG = LogFactory.getLog(ConversionProcessor.class);
    
    private static final String HBM_FILE_EXTENSION = ".hbm.xml";

    /**
     * Processes the conversion of HBM files to JPA entities.
     * 
     * @param inputFolder the directory containing HBM files
     * @param outputFolder the target directory for output
     * @param annotateExisting whether to annotate existing files or generate new ones
     * @throws RuntimeException if the conversion process fails
     */
    public void processConversion(final String inputFolder, final String outputFolder, 
            final boolean annotateExisting) {
        
        validateAndCreateOutputDirectory(outputFolder);
        
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

    private void validateAndCreateOutputDirectory(final String outputFolder) {
        if (FileUtils.createDirectories(outputFolder)) {
            throw new RuntimeException("Failed to create or validate output folder: " + outputFolder);
        }
    }

    private File validateInputDirectory(final String inputFolder) {
        final File inputDir = new File(inputFolder);
        
        if (!inputDir.exists()) {
            throw new IllegalArgumentException("Input folder does not exist: " + inputFolder);
        }
        
        if (!inputDir.isDirectory()) {
            throw new IllegalArgumentException("Input path is not a directory: " + inputFolder);
        }
        
        return inputDir;
    }

    private File[] findHbmFiles(final File inputDir, final String inputFolder) {
        final File[] files = inputDir.listFiles((dir, name) -> name.endsWith(HBM_FILE_EXTENSION));
        
        if (files == null) {
            throw new RuntimeException("Unable to list files in directory: " + inputFolder);
        }
        
        return files;
    }

    private Map<String, JpaEntity> parseHbmFiles(final File[] hbmFiles) {
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

    private void configureEntitySettings(final Map<String, JpaEntity> jpaEntityMap) {
        LOG.info("Configuring entity inheritance, relationships, and embeddable settings...");
        
        // Process inheritance settings for entities with parent classes
        configureInheritanceSettings(jpaEntityMap);
        
        // Process foreign key inverse relationships
        configureForeignKeyRelationships(jpaEntityMap);
        
        // Process embeddable settings for composite columns
        configureEmbeddableSettings(jpaEntityMap);
    }

    private void generateOrAnnotateEntities(final Map<String, JpaEntity> jpaEntityMap, 
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

    private void configureInheritanceSettings(final Map<String, JpaEntity> jpaEntityMap) {
        for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
            final String parentClassName = jpaEntity.getParentClass();
            
            if (parentClassName == null) {
                continue;
            }
            
            setInheritanceOnParentClass(jpaEntityMap, jpaEntity);
        }
    }

    private void configureForeignKeyRelationships(final Map<String, JpaEntity> jpaEntityMap) {
        for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
            setForeignKeyInverseRelationship(jpaEntityMap, jpaEntity);
        }
    }

    private void configureEmbeddableSettings(final Map<String, JpaEntity> jpaEntityMap) {
        // Create a copy of the values to avoid ConcurrentModificationException
        for (final JpaEntity jpaEntity : new HashSet<>(jpaEntityMap.values())) {
            processCompositeColumns(jpaEntityMap, jpaEntity.getColumns());
            
            for (final JpaRelationship jpaRelationship : jpaEntity.getRelationships()) {
                processCompositeColumns(jpaEntityMap, jpaRelationship.getReferencedColumns());
            }
        }
    }

    private void setInheritanceOnParentClass(final Map<String, JpaEntity> jpaEntityMap, final JpaEntity jpaEntity) {
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

    private String determineInheritanceStrategy(final JpaEntity childEntity, final JpaEntity parentEntity) {
        if (childEntity.isSecondTable()) {
            return "JOINED";
        }
        
        if (StringUtils.isNotBlank(childEntity.getTable())) {
            return "TABLE_PER_CLASS";
        }
        
        // Default to SINGLE_TABLE if no inheritance strategy is already set
        return parentEntity.getInheritance() == null ? "SINGLE_TABLE" : null;
    }

    private void setForeignKeyInverseRelationship(final Map<String, JpaEntity> jpaEntityMap, final JpaEntity jpaEntity) {
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

    private boolean isOneToManyWithForeignKey(final JpaRelationship jpaRelationship) {
        return JpaRelationship.Type.OneToMany.equals(jpaRelationship.getRelationshipType()) &&
               !jpaRelationship.getReferencedColumns().isEmpty() &&
               StringUtils.isNotBlank(jpaRelationship.getReferencedColumns().get(0).getForeignKey());
    }

    private void updateInverseRelationshipForeignKey(final JpaEntity inverseEntity, final JpaEntity jpaEntity, 
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

    private void processCompositeColumns(final Map<String, JpaEntity> jpaEntityMap, 
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

    private void createOrGetEmbeddableEntity(final Map<String, JpaEntity> jpaEntityMap, 
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