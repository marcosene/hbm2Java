package com.devtools.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;

/**
 * Utility class for JavaParser operations.
 * Provides methods to parse Java source files and extract information.
 */
public final class JavaParserUtils {

    private static final Log LOG = LogFactory.getLog(JavaParserUtils.class);

    private JavaParserUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Searches for a variable name by its type in a Java class file.
     * 
     * @param outputFolder the folder containing the Java source files
     * @param fullClassName the full name of the class to search in
     * @param type the type of the variable to find
     * @return the variable name if found, null otherwise
     */
    public static String searchVariableNameByType(final String outputFolder, final String fullClassName, final String type) {
        final String fullClassFilename = FileUtils.findClassPath(new java.io.File(outputFolder),
                ClassNameUtils.getPackageName(fullClassName), ClassNameUtils.getSimpleClassName(fullClassName));

        final Path path;
        try {
            path = Paths.get(fullClassFilename);
        } catch (final Exception e) {
            LOG.error("Invalid path for class: " + fullClassName, e);
            return "";
        }

        // Parse the file
        final CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(path);
        } catch (final IOException e) {
            LOG.error("Class " + fullClassName + " not found", e);
            return "";
        }
        
        final Optional<FieldDeclaration> fieldDeclaration = cu.findAll(FieldDeclaration.class).stream()
                .filter(field -> ClassNameUtils.getSimpleClassName(field.getElementType().asString())
                        .equals(ClassNameUtils.getSimpleClassName(type)))
                .findFirst();
                
        return fieldDeclaration.map(declaration ->
                declaration.getVariables().get(0).getNameAsString()).orElse(null);
    }
}