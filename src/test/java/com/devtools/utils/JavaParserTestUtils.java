package com.devtools.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for parsing Java code and making precise assertions on fields and annotations.
 */
public class JavaParserTestUtils {
    
    private static final JavaParser javaParser = new JavaParser();
    
    /**
     * Parse a Java file and return the compilation unit.
     */
    public static CompilationUnit parseJavaFile(Path javaFilePath) throws IOException {
        String content = Files.readString(javaFilePath);
        return javaParser.parse(content).getResult()
                .orElseThrow(() -> new RuntimeException("Failed to parse Java file: " + javaFilePath));
    }
    
    /**
     * Get the main class declaration from a compilation unit.
     */
    public static ClassOrInterfaceDeclaration getMainClass(CompilationUnit cu) {
        return cu.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new RuntimeException("No class found in compilation unit"));
    }
    
    /**
     * Find a field by name in a class.
     */
    public static FieldDeclaration findField(ClassOrInterfaceDeclaration clazz, String fieldName) {
        return clazz.getFields().stream()
                .filter(field -> field.getVariables().stream()
                        .anyMatch(var -> var.getNameAsString().equals(fieldName)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Field not found: " + fieldName));
    }
    
    /**
     * Check if a field has a specific annotation.
     */
    public static boolean hasAnnotation(FieldDeclaration field, String annotationName) {
        return field.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals(annotationName) || 
                               ann.getNameAsString().endsWith("." + annotationName));
    }
    
    /**
     * Check if a class has a specific annotation.
     */
    public static boolean hasClassAnnotation(ClassOrInterfaceDeclaration clazz, String annotationName) {
        return clazz.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals(annotationName) || 
                               ann.getNameAsString().endsWith("." + annotationName));
    }
    
    /**
     * Get an annotation from a field.
     */
    public static Optional<AnnotationExpr> getAnnotation(FieldDeclaration field, String annotationName) {
        return field.getAnnotations().stream()
                .filter(ann -> ann.getNameAsString().equals(annotationName) || 
                             ann.getNameAsString().endsWith("." + annotationName))
                .findFirst();
    }
    
    /**
     * Get an annotation from a class.
     */
    public static Optional<AnnotationExpr> getClassAnnotation(ClassOrInterfaceDeclaration clazz, String annotationName) {
        return clazz.getAnnotations().stream()
                .filter(ann -> ann.getNameAsString().equals(annotationName) || 
                             ann.getNameAsString().endsWith("." + annotationName))
                .findFirst();
    }
    
    /**
     * Get the value of an annotation attribute.
     */
    public static Optional<String> getAnnotationAttribute(AnnotationExpr annotation, String attributeName) {
        if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
            return normalAnnotation.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals(attributeName))
                    .map(MemberValuePair::getValue)
                    .map(value -> value.toString().replaceAll("\"", ""))
                    .findFirst();
        } else if (annotation instanceof SingleMemberAnnotationExpr && "value".equals(attributeName)) {
            SingleMemberAnnotationExpr singleMemberAnnotation = (SingleMemberAnnotationExpr) annotation;
            return Optional.of(singleMemberAnnotation.getMemberValue().toString().replaceAll("\"", ""));
        }
        return Optional.empty();
    }
    
    /**
     * Check if an annotation has a specific attribute (regardless of value).
     */
    public static boolean hasAnnotationAttribute(AnnotationExpr annotation, String attributeName) {
        return getAnnotationAttribute(annotation, attributeName).isPresent();
    }
    
    /**
     * Check if an annotation has a specific attribute with a specific value.
     */
    public static boolean hasAnnotationAttribute(AnnotationExpr annotation, String attributeName, String expectedValue) {
        return getAnnotationAttribute(annotation, attributeName)
                .map(value -> value.equals(expectedValue))
                .orElse(false);
    }
    
    /**
     * Get the field type as string.
     */
    public static String getFieldType(FieldDeclaration field) {
        return field.getCommonType().toString();
    }
    
    /**
     * Get all annotation names on a field.
     */
    public static List<String> getFieldAnnotationNames(FieldDeclaration field) {
        return field.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();
    }
    
    /**
     * Get all annotation names on a class.
     */
    public static List<String> getClassAnnotationNames(ClassOrInterfaceDeclaration clazz) {
        return clazz.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();
    }
    
    /**
     * Check if a field has multiple annotations.
     */
    public static boolean hasAllAnnotations(FieldDeclaration field, String... annotationNames) {
        List<String> fieldAnnotations = getFieldAnnotationNames(field);
        for (String annotationName : annotationNames) {
            if (fieldAnnotations.stream().noneMatch(ann -> 
                    ann.equals(annotationName) || ann.endsWith("." + annotationName))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if a class has multiple annotations.
     */
    public static boolean hasAllClassAnnotations(ClassOrInterfaceDeclaration clazz, String... annotationNames) {
        List<String> classAnnotations = getClassAnnotationNames(clazz);
        for (String annotationName : annotationNames) {
            if (classAnnotations.stream().noneMatch(ann -> 
                    ann.equals(annotationName) || ann.endsWith("." + annotationName))) {
                return false;
            }
        }
        return true;
    }
}