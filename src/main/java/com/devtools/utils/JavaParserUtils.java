package com.devtools.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/**
 * Utility class for JavaParser operations.
 * Provides methods to parse Java source files and extract information.
 */
public final class JavaParserUtils {

    private static final Log LOG = LogFactory.getLog(JavaParserUtils.class);

    private static final Set<String> PERSISTENCE_ANNOTATION_PACKAGES = Set.of(
            "javax.persistence",
            "jakarta.persistence",
            "org.hibernate.annotations"
    );

    private static final JavaParser JAVA_PARSER = new JavaParser();

    private JavaParserUtils() {
        // Utility class - prevent instantiation
    }

    public static CompilationUnit parseJava(final String outputFolder, final String fullClassName) throws IOException {
        final String classPath = FileUtils.findClassPath(new File(outputFolder),
                ClassNameUtils.getPackageName(fullClassName), ClassNameUtils.getSimpleClassName(fullClassName));

        final Path path;
        try {
            path = Paths.get(classPath);
        } catch (final Exception e) {
            LOG.warn("Java class not found for " + fullClassName);
            return null;
        }

        final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());

        final JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        final ParserConfiguration config = new ParserConfiguration().setSymbolResolver(symbolSolver);
        final JavaParser parser = new JavaParser(config);

        return parser.parse(path).getResult().orElseThrow();
    }

    /**
     * Searches for a variable name by its type in a Java class file.
     * 
     * @param outputFolder the folder containing the Java source files
     * @param fullClassName the full name of the class to search in
     * @param type the type of the variable to find
     * @return the variable name if found, null otherwise
     */
    public static String findVariableNameByType(final String outputFolder, final String fullClassName,
            final String type) {
        final String classPath = FileUtils.findClassPath(new java.io.File(outputFolder),
                ClassNameUtils.getPackageName(fullClassName), ClassNameUtils.getSimpleClassName(fullClassName));

        final Path path;
        try {
            path = Paths.get(classPath);
        } catch (final Exception e) {
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

        if (fieldDeclaration.isEmpty()) {
            return "";
        }
        return fieldDeclaration.map(declaration ->
                declaration.getVariables().get(0).getNameAsString()).orElse("");
    }

    public static Map<String, FieldDeclaration> findFieldsWithNonStandardGetters(final ClassOrInterfaceDeclaration clazz) {
        final Map<String, FieldDeclaration> nonStandardGetters = new HashMap<>();

        // Extract non-static, non-constant fields
        final Map<String, FieldDeclaration> fields = clazz.getFields().stream()
                .filter(f -> !f.isStatic()) // Ignore static fields
                .flatMap(f -> f.getVariables().stream()
                        .filter(v -> !f.isFinal()) // Ignore constants (static final)
                        .map(v -> Map.entry(v.getNameAsString(), f)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (final MethodDeclaration method : clazz.getMethods()) {
            if (method.getBody().isEmpty() || !method.getParameters().isEmpty()) {
                continue; // Ignore methods without bodies or methods with parameters
            }

            method.findAll(ReturnStmt.class).stream()
                    .map(ReturnStmt::getExpression)
                    .flatMap(Optional::stream)
                    .filter(Expression::isNameExpr)
                    .map(expr -> ((NameExpr) expr).getNameAsString())
                    .filter(fields::containsKey)
                    .forEach(fieldName -> {
                        final FieldDeclaration field = fields.get(fieldName);
                        final boolean isBooleanField = field.getElementType().asString().equals("boolean");

                        // Determine expected getter names
                        final String expectedGetterName = (isBooleanField ? "is" : "get") +
                              Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

                        final String actualMethodName = method.getNameAsString();

                        // Check if method name deviates from expected standard
                        if (!actualMethodName.equals(expectedGetterName)) {
                            final String derivedFieldName = Utils.uncapitalize(
                                    actualMethodName.replaceFirst("^get", "").replaceFirst("^is", ""));

                            if (!nonStandardGetters.containsKey(derivedFieldName)) {
                                nonStandardGetters.put(derivedFieldName, field);
                            }
                        }
                    });
        }
        return nonStandardGetters;
    }

    public static AnnotationExpr toAnnotationExpr(final String annotationString) {
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

    public static boolean addAnnotations(final List<String> newAnnotations, final NodeWithAnnotations<?> node) {
        boolean added = false;
        for (final String annotationString : newAnnotations) {
            final AnnotationExpr annotation = toAnnotationExpr(annotationString);
            final String annotationName = annotation.getNameAsString();

            // Check if the node already has the annotation
            final boolean alreadyAnnotated = node.getAnnotations().stream()
                    .anyMatch(a -> a.getNameAsString().equals(annotationName));

            if (!alreadyAnnotated) {
                node.addAnnotation(annotation);
                added = true;
            }
        }
        return added;
    }

    // This method returns the generic type if present, otherwise the base type
    public static String resolveGenericType(final Type type) {
        if (type.isClassOrInterfaceType()) {
            final ClassOrInterfaceType cit = type.asClassOrInterfaceType();
            final String typeName = cit.getName().asString();
            // If it's a generic type like Set<?> or Map<?,?> etc
            if (cit.getTypeArguments().isPresent()) {
                // get the generic type, for Map get the second generic type
                return cit.getTypeArguments().get().get("Map".equals(typeName) ? 1 : 0).asString();
            } else {
                return switch (typeName) {
                    // In case generic type was not defined, return empty
                    case "List", "Set", "Map" -> "";
                    default -> typeName;
                };
            }
        }
        return type.toString();
    }

    public static void createGetterAndSetter(final ClassOrInterfaceDeclaration clazz, final String fieldName) {
        final int insertIndex;
        Integer indexFirstMethod = null;
        int lastConstructorIndex = -1;

        for (int i = 0; i < clazz.getMembers().size(); i++) {
            final BodyDeclaration<?> member = clazz.getMember(i);

            if (member instanceof ConstructorDeclaration) {
                lastConstructorIndex = i;
            } else if (indexFirstMethod == null && member instanceof MethodDeclaration) {
                indexFirstMethod = i;
            }
        }

        // Insert after last constructor, if any
        if (lastConstructorIndex != -1) {
            insertIndex = lastConstructorIndex + 1;
        } else {
            // Else insert before first method (if any)
            // Fallback: insert at end
            insertIndex = Objects.requireNonNullElseGet(indexFirstMethod, () -> clazz.getMembers().size());
        }

        // Add getter and setter methods for a specific field
        final Type fieldType = clazz.getFieldByName(fieldName).orElseThrow().getVariable(0).getType();

        // Create getter method
        final MethodDeclaration getter = new MethodDeclaration()
                .setModifiers(Modifier.Keyword.PUBLIC)
                .setType(fieldType)
                .setName("get" + Utils.capitalize(fieldName))
                .setBody(new com.github.javaparser.ast.stmt.BlockStmt().addStatement(
                        new ReturnStmt(new NameExpr(fieldName))
                ));

        // Create setter method
        final MethodDeclaration setter = new MethodDeclaration()
                .setModifiers(Modifier.Keyword.PUBLIC)
                .setType(new VoidType())
                .setName("set" + Utils.capitalize(fieldName))
                .addParameter(new Parameter()
                        .setType(fieldType)
                        .setName(fieldName)
                        .setFinal(true))
                .setBody(new com.github.javaparser.ast.stmt.BlockStmt().addStatement(
                        new AssignExpr(
                                new FieldAccessExpr(new ThisExpr(), fieldName),
                                new NameExpr(fieldName),
                                AssignExpr.Operator.ASSIGN
                        )
                ));

        // Insert methods at the correct position
        clazz.getMembers().add(insertIndex, getter);
        clazz.getMembers().add(insertIndex + 1, setter);
    }

    public static void addDefaultConstructorIfNeeded(final ClassOrInterfaceDeclaration clazz) {
        // If no constructors, Java provides default no-arg, so no action needed
        if (clazz.getConstructors().isEmpty()) {
            return;
        }

        // Check if no-arg constructor exists
        final boolean hasNoArgConstructor = clazz.getConstructors().stream()
                .anyMatch(ctor -> ctor.getParameters().isEmpty());

        if (hasNoArgConstructor) {
            return;
        }

        LOG.warn("Default constructor (no args) not found in " + clazz.getNameAsString() +
                ". Creating one for reflection instantiation required by hibernate");

        // Manually create the constructor declaration (not added yet)
        final ConstructorDeclaration defaultConstructor = new ConstructorDeclaration();
        defaultConstructor.setName(clazz.getName());
        defaultConstructor.setModifiers(Modifier.Keyword.PUBLIC);

        // Create constructor body with comment
        final BlockStmt body = new BlockStmt();
        body.addOrphanComment(new LineComment(" required for hibernate reflection instantiation"));
        defaultConstructor.setBody(body);

        // Find the insertion index after last field declaration
        int insertIndex = 0;
        for (int i = 0; i < clazz.getMembers().size(); i++) {
            if (clazz.getMember(i).isFieldDeclaration()) {
                insertIndex = i + 1;
            }
        }

        // Insert constructor at the right position
        clazz.getMembers().add(insertIndex, defaultConstructor);
    }

    public static List<FieldDeclaration> getNonPersistentFields(final CompilationUnit cu) {
        return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(clazz -> !clazz.isStatic() && isJpaClass(clazz))
                .flatMap(clazz -> clazz.getFields().stream())
                .filter(field -> !field.isStatic() && !field.isTransient())
                .filter(field -> !hasPersistenceAnnotation(field))
                .toList();
    }

    private static boolean isJpaClass(final ClassOrInterfaceDeclaration clazz) {
        if (clazz.isStatic()) {
            return false;
        }
        for (final AnnotationExpr annotation : clazz.getAnnotations()) {
            try {
                // Try resolving fully
                final ResolvedAnnotationDeclaration resolved = annotation.resolve();
                final String packageName = resolved.getPackageName();
                final String name = resolved.getName();
                if ((packageName.equals("javax.persistence") || packageName.equals("jakarta.persistence")) &&
                        StringUtils.equals("Entity", name)) {
                    return true;
                }
            } catch (final Exception e) {
                // Fallback: match raw annotation names
                final String name = annotation.getNameAsString();
                if (StringUtils.equals("Entity", name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasPersistenceAnnotation(final FieldDeclaration field) {
        for (final AnnotationExpr annotation : field.getAnnotations()) {
            if (isPersistenceAnnotationRecursive(annotation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPersistenceAnnotationRecursive(final AnnotationExpr annotation) {
        // Try resolving the annotation itself
        if (resolvesToPersistenceAnnotation(annotation)) {
            return true;
        }

        // Check nested values (e.g., arrays, nested annotations)
        if (annotation.isNormalAnnotationExpr()) {
            for (final MemberValuePair pair : annotation.asNormalAnnotationExpr().getPairs()) {
                final Expression value = pair.getValue();

                if (value.isAnnotationExpr()) {
                    if (isPersistenceAnnotationRecursive(value.asAnnotationExpr())) {
                        return true;
                    }

                } else if (value.isArrayInitializerExpr()) {
                    for (final Expression expr : value.asArrayInitializerExpr().getValues()) {
                        if (expr.isAnnotationExpr()) {
                            if (isPersistenceAnnotationRecursive(expr.asAnnotationExpr())) {
                                return true;
                            }
                        }
                    }

                } else {
                    // Try to parse value as an annotation (e.g. @Column inside @Columns)
                    try {
                        final var result = JAVA_PARSER.parseAnnotation(value.toString());
                        if (result.isSuccessful() && result.getResult().isPresent()) {
                            final AnnotationExpr maybeAnnotation = result.getResult().get();
                            if (isPersistenceAnnotationRecursive(maybeAnnotation)) {
                                return true;
                            }
                        }
                    } catch (final Exception ignored) {}
                }
            }
        }

        return false;
    }

    private static boolean resolvesToPersistenceAnnotation(final AnnotationExpr annotation) {
        try {
            final ResolvedAnnotationDeclaration resolved = annotation.resolve();
            final String packageName = resolved.getPackageName();
            return PERSISTENCE_ANNOTATION_PACKAGES.stream()
                    .anyMatch(packageName::startsWith);
        } catch (final Exception e) {
            // Can't resolve: assume not persistence-related
            return false;
        }
    }

}