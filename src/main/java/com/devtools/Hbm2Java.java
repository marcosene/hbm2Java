package com.devtools;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.devtools.definition.JpaBase;
import com.devtools.definition.JpaEntity;
import com.devtools.processors.AnnotationBuilder;
import com.devtools.processors.EntityReplacer;
import com.devtools.processors.HbmParser;

public class Hbm2Java {

    private static final Log LOG = LogFactory.getLog(Hbm2Java.class);

    public static void main(final String[] args) throws Exception {

        final String inputFolder = args[0];
        final String outputFolder = args[1];

        if (Utils.createFolder(outputFolder)) {
            return; // Exit the program if folder creation fails
        }

        final File folder = new File(inputFolder);

        if (folder.exists() && folder.isDirectory()) {
            final File[] files = folder.listFiles((dir, name) -> name.endsWith(".hbm.xml"));

            if (files != null) {
                final Map<String, JpaEntity> jpaEntityMap = new TreeMap<>();

                for (final File hbmFile : files) {
                    final String hbmFilePath = hbmFile.getAbsolutePath();
                    LOG.info("Parsing: " + hbmFilePath);

                    final HbmParser hbmParser = new HbmParser();
                    final JpaBase jpaBase = hbmParser.parse(hbmFilePath);

                    if (jpaBase == null) {
                        LOG.error("Failed to parse: " + hbmFilePath);
                    } else {
                        jpaBase.getEntities().forEach(entity -> jpaEntityMap.put(entity.getClassName(), entity));
                    }
                }

                // Check if something needs to be done in the parent class before building annotations
                // like setting Inheritance
                for (final Map.Entry<String, JpaEntity> entry : jpaEntityMap.entrySet()) {
                    final JpaEntity jpaEntity = entry.getValue();
                    if (jpaEntity.getParentClass() != null) {
                        final JpaEntity parentEntity = jpaEntityMap.get(jpaEntity.getParentClass());
                        if (parentEntity != null) {
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
                }

                for (final JpaEntity jpaEntity : jpaEntityMap.values()) {
                    LOG.info("Building: " + jpaEntity.getClassName());
                    try {
                        final AnnotationBuilder annotationBuilder = new AnnotationBuilder();
                        annotationBuilder.build(jpaEntity);

                        final EntityReplacer entityReplacer = new EntityReplacer();
                        entityReplacer.replace(jpaEntity, "D:\\dev-samba\\core-frameworks\\core\\lms\\conreqi\\conreqi.core\\src");
                        //entityReplacer.replace(jpaEntity, outputFolder);

                        //final EntityGenerator entityGenerator = new EntityGenerator();
                        //entityGenerator.generate(jpaEntity, outputFolder);
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
}
