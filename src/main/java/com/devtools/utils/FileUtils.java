package com.devtools.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for file and directory operations.
 * Uses modern Java NIO APIs for better performance and error handling.
 */
public final class FileUtils {

    private static final Log LOG = LogFactory.getLog(FileUtils.class);

    private FileUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Writes content to a file using Java NIO.
     * Creates parent directories if they don't exist.
     * 
     * @param filename the target file path
     * @param content the content to write
     * @throws IOException if an I/O error occurs
     */
    public static void writeFile(final String filename, final String content) throws IOException {
        final Path path = Paths.get(filename);
        
        // Create parent directories if they don't exist
        final Path parentDir = path.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        
        Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOG.info("Successfully generated " + filename);
    }

    /**
     * Creates a directory and all necessary parent directories.
     * Uses Java NIO for better error handling.
     * 
     * @param directoryPath the directory path to create
     * @return true if directory creation failed, false if successful or already exists
     */
    public static boolean createDirectories(final String directoryPath) {
        try {
            final Path path = Paths.get(directoryPath);
            
            if (Files.exists(path)) {
                if (!Files.isDirectory(path)) {
                    LOG.error(directoryPath + " exists but is not a directory.");
                    return true; // Failure - exists but not a directory
                }
                return false; // Success - already exists
            }
            
            Files.createDirectories(path);
            LOG.info("Directory created successfully: " + directoryPath);
            return false; // Success
            
        } catch (final IOException e) {
            LOG.error("Failed to create directory: " + directoryPath, e);
            return true; // Failure
        }
    }

    /**
     * Extracts the filename without any extensions and capitalizes the first letter.
     * Uses Apache Commons IO FilenameUtils for better cross-platform compatibility.
     * 
     * @param absoluteFilename the absolute file path
     * @return the filename without extensions, with first letter capitalized
     */
    public static String getFileNameNoExtensions(final String absoluteFilename) {
        // Use Apache Commons IO to extract filename without extension
        final String fileName = FilenameUtils.getBaseName(absoluteFilename);
        
        // Capitalize the first letter using Apache Commons Lang3
        return StringUtils.capitalize(fileName);
    }

    /**
     * Recursively searches for a file matching the specified class name inside the folder.
     * It looks for either "ClassName.java" or "ClassName.class".
     * Uses Apache Commons IO for more efficient file searching.
     *
     * @param folder    the starting directory to search in
     * @param className the name of the class (without extension)
     * @return the full absolute path of the file if found; otherwise, null.
     */
    public static String findClassPath(final File folder, final String className) {
        if (folder == null || !folder.isDirectory()) {
            return null;
        }

        // Use Apache Commons IO to search for files with specific names
        final NameFileFilter javaFilter = new NameFileFilter(className + ".java");
        final NameFileFilter classFilter = new NameFileFilter(className + ".class");
        
        // Search for .java files first
        Collection<File> javaFiles = org.apache.commons.io.FileUtils.listFiles(folder, javaFilter, TrueFileFilter.INSTANCE);
        if (!javaFiles.isEmpty()) {
            return javaFiles.iterator().next().getAbsolutePath();
        }
        
        // Search for .class files if no .java file found
        Collection<File> classFiles = org.apache.commons.io.FileUtils.listFiles(folder, classFilter, TrueFileFilter.INSTANCE);
        if (!classFiles.isEmpty()) {
            return classFiles.iterator().next().getAbsolutePath();
        }
        
        return null; // no match found
    }
}