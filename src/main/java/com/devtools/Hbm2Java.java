package com.devtools;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.devtools.processing.ConversionProcessor;

/**
 * The {@code Hbm2Java} class serves as the main entry point for converting Hibernate HBM XML files 
 * into JPA-annotated Java entities.
 *
 * <p>This class handles command-line argument parsing, validation, and delegates the actual 
 * conversion process to the {@link ConversionProcessor}. It supports two modes of operation:</p>
 * <ul>
 *   <li><strong>Generation mode</strong>: Creates new Java entity files with JPA annotations</li>
 *   <li><strong>Annotation mode</strong>: Adds JPA annotations to existing Java entity files</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 *   java Hbm2Java /path/to/inputFolder /path/to/baseOutputFolder [--annotateExisting]
 * </pre>
 * <p>Where:</p>
 * <ul>
 *   <li>{@code inputFolder} - Directory containing Hibernate {@code *.hbm.xml} files</li>
 *   <li>{@code baseOutputFolder} - Target directory for generated files or base search folder for existing files</li>
 *   <li>{@code --annotateExisting} - Optional flag to annotate existing Java files instead of generating new ones</li>
 * </ul>
 *
 * <p>The conversion process is handled by {@link ConversionProcessor}, which orchestrates
 * the parsing, annotation generation, and file output operations.</p>
 *
 * <p>Comprehensive logging tracks the conversion progress, and robust error handling ensures
 * graceful failure recovery for invalid input files or processing errors.</p>
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

            final ConversionProcessor processor = new ConversionProcessor();
            processor.processConversion(inputFolder, outputFolder, annotateExisting);
            
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


}
