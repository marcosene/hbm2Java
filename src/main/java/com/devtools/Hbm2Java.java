package com.devtools;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.devtools.definition.JpaEntity;
import com.devtools.processors.EntityGenerator;
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
                for (final File hbmFile : files) {
                    final String hbmFilePath = hbmFile.getAbsolutePath();
                    final HbmParser hbmParser = new HbmParser();

                    LOG.info("Parsing: " + hbmFilePath);
                    final JpaEntity entity = hbmParser.parse(hbmFilePath);

                    if (entity != null) {
                        try {
                            final EntityGenerator entityGenerator = new EntityGenerator();
                            entityGenerator.generate(entity, outputFolder);
                            LOG.info("Entity successfully written to " + outputFolder + "\\" + entity.getClassName()
                                    + ".java");
                        } catch (final IOException e) {
                            LOG.error("Error writing to file: " + e.getMessage());
                        }
                    } else {
                        LOG.error("Failed to parse: " + hbmFilePath);
                    }
                }
            } else {
                LOG.warn("No .hbm.xml files found in: " + inputFolder);
            }
        } else {
            LOG.error("Folder not found or is not a directory: " + inputFolder);
        }
    }
}
