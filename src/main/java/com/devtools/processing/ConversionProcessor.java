package com.devtools.processing;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.persistence.InheritanceType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.devtools.model.jpa.JpaColumn;
import com.devtools.model.jpa.JpaCompositeColumn;
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

        checkInconsistencies(jpaEntityMap);

        generateOrAnnotateEntities(jpaEntityMap, outputFolder, annotateExisting);
    }

    private void validateAndCreateOutputDirectory(final String outputFolder) {
        if (!FileUtils.createDirectories(outputFolder)) {
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
        try (final Stream<Path> paths = Files.walk(Paths.get(inputDir.getAbsolutePath()))) {
            final List<File> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(HBM_FILE_EXTENSION))
                    .filter(path -> !path.toString().contains(File.separator + "build" + File.separator))
                    .filter(path -> !path.toString().contains(File.separator + "target" + File.separator))
                    .map(Path::toFile)
                    .toList();

            return files.toArray(new File[0]);
        } catch (final Exception e) {
            throw new RuntimeException("Unable to list files recursively in directory: " + inputFolder, e);
        }
    }

    private Map<String, JpaEntity> parseHbmFiles(final File[] hbmFiles) {
        final Map<String, JpaEntity> jpaEntityMap = new TreeMap<>();
        final HbmParser hbmParser = new HbmParser();
        
        for (final File hbmFile : hbmFiles) {
            final String hbmFilePath = hbmFile.getAbsolutePath();
            LOG.info("Parsing HBM file: " + hbmFilePath);

            try {
                final List<JpaEntity> entities = hbmParser.parse(hbmFilePath);

                if (entities == null) {
                    LOG.error("Failed to parse HBM file: " + hbmFilePath);
                    continue;
                }

                final int entitiesCount = entities.size();
                entities.forEach(entity -> jpaEntityMap.put(entity.getSimpleName(), entity));
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
        configureEmbeddable(jpaEntityMap);
    }

    private void configureInheritanceSettings(final Map<String, JpaEntity> jpaEntityMap) {
        for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
            String parentClassName = jpaEntity.getSimpleParentClass();
            
            if (parentClassName == null) {
                continue;
            }

            JpaEntity parentEntity = jpaEntityMap.get(parentClassName);
            while (parentEntity != null && StringUtils.isBlank(parentEntity.getTable()) &&
                   jpaEntityMap.get(parentClassName) != null) {
                parentEntity = jpaEntityMap.get(parentClassName);
                parentClassName = parentEntity.getSimpleParentClass();
            }

            if (parentEntity == null) {
                continue;
            }

            if (StringUtils.isNotBlank(parentEntity.getTable())) {
                jpaEntity.setParentTable(parentEntity.getTable());
            }

            final InheritanceType inheritanceStrategy = determineInheritanceStrategy(jpaEntity, parentEntity);
            if (inheritanceStrategy != null) {
                if (parentEntity.getInheritance() == null) {
                    parentEntity.setInheritance(inheritanceStrategy);
                } else {
                    if (inheritanceStrategy != parentEntity.getInheritance()) {
                        LOG.error(String.format("Inconsistency found on %s mapping: inheritance strategy on parent %s "
                                        + "was previsously set as %s, now it's trying to change to %s",
                                jpaEntity.getSimpleName(), parentEntity.getSimpleName(),
                                parentEntity.getInheritance(), inheritanceStrategy));
                    }
                }
            }
        }
    }

    private InheritanceType determineInheritanceStrategy(final JpaEntity childEntity, final JpaEntity parentEntity) {
        // if both parent and child has a table defined in the class, it's a joined strategy
        if (StringUtils.isNotBlank(parentEntity.getTable()) &&
                StringUtils.isNotBlank(childEntity.getTable())) {
            return InheritanceType.JOINED;

        } else if (StringUtils.isNotBlank(childEntity.getTable())) {
            // if only the child has a table set, so it's a table per class strategy
            return InheritanceType.TABLE_PER_CLASS;
        }

        // Default to SINGLE_TABLE
        return InheritanceType.SINGLE_TABLE;
    }

    private void configureForeignKeyRelationships(final Map<String, JpaEntity> jpaEntityMap) {
        for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
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

    private void configureEmbeddable(final Map<String, JpaEntity> jpaEntityMap) {
        // Create a copy of the values to avoid ConcurrentModificationException
        for (final JpaEntity jpaEntity : new HashSet<>(jpaEntityMap.values())) {
            // Set embeddable to composite-columns class
            for (final JpaCompositeColumn compositeColumn : jpaEntity.getCompositeColumns()) {
                final String embeddableClassName = ClassNameUtils.getSimpleClassName(compositeColumn.getReturnType());
                processCompositeColumns(jpaEntityMap, embeddableClassName, compositeColumn.getColumns(), false);
            }

            // Set embeddable to composite-map-key class and annotate their fields
            for (final JpaRelationship jpaRelationship : jpaEntity.getRelationships()) {
                if (StringUtils.isNotBlank(jpaRelationship.getCompositeMapKey()) &&
                    jpaRelationship.getReferencedColumns().size() > 1) {
                    processCompositeColumns(jpaEntityMap, jpaRelationship.getCompositeMapKey(),
                            jpaRelationship.getReferencedColumns(), true);
                }
            }

            // Add annotations to the embeddable classes
            for (final JpaEntity embeddable : jpaEntity.getEmbeddedEntities()) {
                jpaEntityMap.put(embeddable.getSimpleName(), embeddable);
            }
        }
    }

    private void checkInconsistencies(final Map<String, JpaEntity> jpaEntityMap) {
        for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
            if (InheritanceType.TABLE_PER_CLASS.equals(jpaEntity.getInheritance()) && jpaEntity.getDiscriminator() != null) {
                LOG.warn(String.format("Inconsistency found on %s mapping: discriminator defined along a "
                        + "table per class definition. Discriminator annotations will be skipped", jpaEntity.getSimpleName()));
                jpaEntity.setDiscriminator(null);
            }
        }
    }

    private void processCompositeColumns(final Map<String, JpaEntity> jpaEntityMap, final String embeddableClassName,
            final List<JpaColumn> columns, final boolean annotateColumns) {

        if (jpaEntityMap.containsKey(embeddableClassName)) {
            return;
        }

        final JpaEntity embeddableEntity = new JpaEntity();
        embeddableEntity.setName(embeddableClassName);
        embeddableEntity.setEmbeddable(true);

        if (annotateColumns) {
            columns.stream().filter(JpaColumn::isEmbedded).forEach(column -> {
                column.setEmbedded(false);
                embeddableEntity.addColumn(column);
            });
        }

        jpaEntityMap.put(embeddableClassName, embeddableEntity);
        LOG.debug("Created embeddable entity: " + embeddableClassName);
    }

    private void generateOrAnnotateEntities(final Map<String, JpaEntity> jpaEntityMap,
            final String outputFolder, final boolean annotateExisting) {

        final AnnotationBuilder annotationBuilder = new AnnotationBuilder(outputFolder);
        final AnnotationApplier annotationApplier = new AnnotationApplier(outputFolder);
        final EntityGenerator entityGenerator = new EntityGenerator();

        int successCount = 0;
        int errorCount = 0;

        for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
            final String entityName = jpaEntity.getSimpleName();
            LOG.info("Processing entity: " + entityName);

            try {
                annotationBuilder.build(jpaEntity);

                if (annotateExisting) {
                    annotationApplier.applyAnnotations(jpaEntity);
                    LOG.debug("Successfully annotated existing entity: " + entityName);
                } else {
                    entityGenerator.generate(jpaEntity, outputFolder);
                    LOG.debug("Successfully generated new entity: " + entityName);
                }

                successCount++;

            } catch (final Exception e) {
                LOG.error("Error processing entity '" + entityName + "' (class: " +
                          jpaEntity.getParentClass() + ")", e);
                errorCount++;
            }
        }

        LOG.info("Entity processing completed. Success: " + successCount + ", Errors: " + errorCount);
    }
}