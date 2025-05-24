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
import com.devtools.utils.Utils;

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

    public static void main(final String[] args) throws Exception {

        final String inputFolder = args[0];
        final String outputFolder = args[1];
        final boolean annotateExisting = args.length > 2 && args[2].equals("--annotateExisting");

        if (Utils.createFolder(outputFolder)) {
            return; // Exit the program if folder creation fails
        }

        final File folder = new File(inputFolder);

        if (folder.exists() && folder.isDirectory()) {
            final HbmParser hbmParser = new HbmParser();
            final AnnotationBuilder annotationBuilder = new AnnotationBuilder(outputFolder);
            final AnnotationApplier annotationApplier = new AnnotationApplier();
            final EntityGenerator entityGenerator = new EntityGenerator();

            final File[] files = folder.listFiles((dir, name) -> name.endsWith(".hbm.xml"));

            if (files != null) {
                final Map<String, JpaEntity> jpaEntityMap = new TreeMap<>();

                for (final File hbmFile : files) {
                    final String hbmFilePath = hbmFile.getAbsolutePath();
                    LOG.info("Parsing: " + hbmFilePath);

                    final JpaBase jpaBase = hbmParser.parse(hbmFilePath);

                    if (jpaBase == null) {
                        LOG.error("Failed to parse: " + hbmFilePath);
                    } else {
                        jpaBase.getEntities().forEach(entity -> jpaEntityMap.put(entity.getName(), entity));
                    }
                }

                // Check if something needs to be done in the parent class before building annotations
                // like setting Inheritance or Embeddable
                checkAdditionalSettings(jpaEntityMap);

                for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
                    LOG.info("Building: " + jpaEntity.getName());
                    try {
                        annotationBuilder.build(jpaEntity);

                        if (annotateExisting) {
                            annotationApplier.replace(jpaEntity, outputFolder);
                        } else {
                            entityGenerator.generate(jpaEntity, outputFolder);
                        }
                    } catch (final Exception e) {
                        LOG.error("Error building annotations for: " + jpaEntity.getFullParentClass(), e);
                    }
                }
            } else {
                LOG.warn("No .hbm.xml files found in: " + inputFolder);
            }
        } else {
            LOG.error("Input folder not found or is not a directory: " + inputFolder);
        }
    }

    private static void checkAdditionalSettings(final Map<String, JpaEntity> jpaEntityMap) {
        for (final Map.Entry<String, JpaEntity> entry : new HashSet<>(jpaEntityMap.entrySet())) {
            final JpaEntity jpaEntity = entry.getValue();
            if (jpaEntity.getParentClass() != null) {
                final JpaEntity parentEntity = jpaEntityMap.get(jpaEntity.getParentClass());
                if (parentEntity != null && parentEntity.getDiscriminator() != null) {
                    if (jpaEntity.isSecondTable()) {
                        parentEntity.setInheritance("JOINED");
                    } else if (StringUtils.isNotBlank(jpaEntity.getTable())) {
                        parentEntity.setInheritance("TABLE_PER_CLASS");
                    } else {
                        if (parentEntity.getInheritance() == null) {
                            parentEntity.setInheritance("SINGLE_TABLE");
                        }
                    }
                }
            }

            setCompositeColumnEmbeddable(jpaEntityMap, jpaEntity.getColumns());
            for (final JpaRelationship jpaRelationship : jpaEntity.getRelationships()) {
                setCompositeColumnEmbeddable(jpaEntityMap, jpaRelationship.getReferencedColumns());
            }
        }
    }

    private static void setCompositeColumnEmbeddable(final Map<String, JpaEntity> jpaEntityMap, final List<JpaColumn> columns) {
        for (final JpaColumn jpaColumn : columns) {
            if (jpaColumn.isComposite()) {
                final String embeddableClass = Utils.getSimpleClass(jpaColumn.getType());
                JpaEntity embeddable = jpaEntityMap.get(embeddableClass);
                if (embeddable == null) {
                    embeddable = new JpaEntity();
                    embeddable.setName(embeddableClass);
                    embeddable.setEmbeddable(true);
                    jpaEntityMap.put(embeddableClass, embeddable);
                }
            }
        }
    }
}
