package com.devtools.processors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.devtools.definition.JpaEntity;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

public class EntityReplacer {

    private static final Log LOG = LogFactory.getLog(EntityReplacer.class);

    private static final JavaParser JAVA_PARSER = new JavaParser();

    public void replace(final JpaEntity entity, final String outputFolder) throws IOException {

        final String fullClassFilename = findClassPath(
                new File(outputFolder),
                entity.getClassName());

        final Path path;
        try {
             path = Paths.get(fullClassFilename);
        } catch (final Exception e) {
            LOG.warn("Java class not found for " + fullClassFilename + ", generating a new one");
            final EntityGenerator entityGenerator = new EntityGenerator();
            entityGenerator.generate(entity, outputFolder);
            return;
        }

        // Parse the file
        final CompilationUnit cu = StaticJavaParser.parse(path);

        // Add Entity annotations to the class
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            for (final String importString : entity.getImports()) {
                cu.addImport(importString);
            }
            addAnnotations(entity.getAnnotations(), clazz);
        });

        // Add annotations to the fields
        cu.findAll(FieldDeclaration.class).forEach(field -> {
            for (final VariableDeclarator variable : field.getVariables()) {
                if (entity.getPrimaryKey() != null && entity.getPrimaryKey().getName().equals(variable.getNameAsString())) {
                    for (final String importString : entity.getPrimaryKey().getImports()) {
                        cu.addImport(importString);
                    }
                    addAnnotations(entity.getPrimaryKey().getAnnotations(), field);
                }

                entity.getColumns().stream()
                        .filter(jpaColumn -> jpaColumn.getName() != null && jpaColumn.getName().equals(variable.getNameAsString()))
                        .findFirst().ifPresent(jpaColumn -> {
                            for (final String importString : jpaColumn.getImports()) {
                                cu.addImport(importString);
                            }
                            addAnnotations(jpaColumn.getAnnotations(), field);
                        });

                entity.getRelationships().stream()
                        .filter(jpaRelationship -> jpaRelationship.getName() != null &&
                                jpaRelationship.getName().equals(variable.getNameAsString()))
                        .findFirst().ifPresent(jpaRelationship -> {
                            for (final String importString : jpaRelationship.getImports()) {
                                cu.addImport(importString);
                            }
                            for (final String annotationString : jpaRelationship.getAnnotations()) {
                                final AnnotationExpr annotation = getAnnotation(annotationString);
                                field.addAnnotation(annotation);
                            }
                        });
            }
        });

        // Write the modified file back
        Files.write(path, cu.toString().getBytes());
    }

    private void addAnnotations(final List<String> newAnnotations, final NodeWithAnnotations<?> node) {
        for (final String annotationString : newAnnotations) {
            final AnnotationExpr annotation = getAnnotation(annotationString);
            final String annotationName = annotation.getNameAsString();

            // Check if the node already has the annotation
            final boolean alreadyAnnotated = node.getAnnotations().stream()
                    .anyMatch(a -> a.getNameAsString().equals(annotationName));

            if (!alreadyAnnotated) {
                node.addAnnotation(annotation);
            }
        }
    }

    public AnnotationExpr getAnnotation(final String annotationString) {
        final String dummyClass = annotationString + "\npublic class Dummy {}";

        final ParseResult<CompilationUnit> parseResult = JAVA_PARSER.parse(dummyClass);

        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            throw new IllegalArgumentException("Parsing failed: " + parseResult.getProblems());
        }

        final CompilationUnit cu = parseResult.getResult().get();

        return cu.getClassByName("Dummy")
                .orElseThrow(() -> new IllegalStateException("Dummy class not found"))
                .getAnnotation(0);
    }

    /**
     * Recursively searches for a file matching the specified class name inside the folder.
     * It looks for either “ClassName.java” or “ClassName.class”.
     *
     * @param folder    the starting directory to search in
     * @param className the name of the class (without extension)
     * @return the full absolute path of the file if found; otherwise, null.
     */
    public static String findClassPath(final File folder, final String className) {
        if (folder == null || !folder.isDirectory()) {
            return null;
        }

        final File[] files = folder.listFiles();
        if (files != null) { // check that folder is readable
            for (final File file : files) {
                if (file.isDirectory()) {
                    // Explore subdirectory recursively.
                    final String result = findClassPath(file, className);
                    if (result != null) {
                        return result;
                    }
                } else {
                    // Check for match against either .java or .class file.
                    final String fileName = file.getName();
                    if (fileName.equals(className + ".java") || fileName.equals(className + ".class")) {
                        return file.getAbsolutePath();
                    }
                }
            }
        }
        return null; // no match found in this folder/subfolders
    }
}
