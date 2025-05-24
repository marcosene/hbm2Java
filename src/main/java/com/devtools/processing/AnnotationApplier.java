package com.devtools.processing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.devtools.model.jpa.JpaAnnotation;
import com.devtools.model.jpa.JpaEntity;
import com.devtools.model.jpa.JpaPrimaryKey;
import com.devtools.utils.ClassNameUtils;
import com.devtools.utils.FileUtils;
import com.devtools.utils.HibernateUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class AnnotationApplier {

    private static final Log LOG = LogFactory.getLog(AnnotationApplier.class);

    private static final JavaParser JAVA_PARSER = new JavaParser();

    // Initialize cache to track processed classes across the entire entity hierarchy
    private final Map<String, String> processedClasses = new HashMap<>();
    private final Map<String, List<String>> processedFields = new HashMap<>();

    public void replace(final JpaEntity entity, final String outputFolder) throws IOException {

        writeAnnotations(entity, outputFolder, ClassNameUtils.getPackageName(entity.getType()), entity.getName(), false);

        // Check if some element parsed from the hbm.xml has no corresponding field in the class
        validateFieldsNotFound(entity);

        // Add annotations to the embeddable classes
        for (final JpaEntity embeddable : entity.getEmbeddedFields()) {
            replace(embeddable, outputFolder);
        }
    }

    private void writeAnnotations(final JpaEntity entity, final String outputFolder, final String packageName,
            final String className, final boolean isParentClass) throws IOException {
        
        final String fullClassFilename = FileUtils.findClassPath(new File(outputFolder), packageName, className);

        final Path path;
        try {
             path = Paths.get(fullClassFilename);
        } catch (final Exception e) {
            LOG.warn("Java class not found for " + className + (!isParentClass ? ", generating a new one" : ""));
            if (!isParentClass) {
                final EntityGenerator entityGenerator = new EntityGenerator();
                entityGenerator.generate(entity, outputFolder);
            }
            return;
        }

        // Parse the file
        final CompilationUnit cu = parseJava(path);
        final ClassOrInterfaceDeclaration clazz = cu.findAll(ClassOrInterfaceDeclaration.class).get(0);
        final AtomicBoolean hasChanged = new AtomicBoolean(false);

        if (!isParentClass) {
            // Add Entity annotations to the class
            for (final String importString : entity.getImports()) {
                cu.addImport(importString);
            }
            addAnnotations(entity.getAnnotations(), clazz);
            hasChanged.set(true);
        }

        final List<JpaAnnotation> allFields = getAllFields(entity);

        // Add annotations to the fields
        cu.findAll(FieldDeclaration.class).forEach(field -> {
            for (final VariableDeclarator variable : field.getVariables()) {
                // @Id must be defined in the class itself, not in the parent
                if (isParentClass && !clazz.isAnnotationPresent("Entity") && "id".equals(variable.getNameAsString())) {
                    LOG.warn("An 'id' field was found in the parent class " + className + ", you should probably remove it");
                    continue;
                }

                allFields.stream()
                        .filter(jpaAnnotation -> jpaAnnotation.getName() != null && jpaAnnotation.getName().equals(variable.getNameAsString()))
                        .forEach(jpaAnnotation -> processJpaAnnotation(className, field, jpaAnnotation, cu, hasChanged));
            }
        });

        // Check now for all non-standard getters (mapped in the hbm.xml with this different name from the field)
        // Example: getBlaBla() { return bla; } -> in this case "blaBla" was mapped in .hbm.xml, instead of "bla"
        final Map<String, FieldDeclaration> nonStandardGetters = findFieldsWithNonStandardGetters(clazz);
        nonStandardGetters.forEach((nonStandardName, field) -> allFields.stream()
                .filter(jpaAnnotation -> !jpaAnnotation.isProcessed() && jpaAnnotation.getName() != null &&
                        jpaAnnotation.getName().equals(nonStandardName))
                .forEach(jpaAnnotation -> processJpaAnnotation(className, field, jpaAnnotation, cu, hasChanged)));

        // Write the modified file back
        if (hasChanged.get()) {
            if (isParentClass) {
                cu.addImport("javax.persistence.MappedSuperclass");
                addAnnotations(List.of("@MappedSuperclass"), clazz);
            } else {
                // If id field was not found in the class (probably it is in the super class)
                // and it was defined in the mapping, we will create it at the beginning of the class
                final JpaPrimaryKey primaryKey = entity.getPrimaryKey();
                if (primaryKey != null && !primaryKey.isProcessed()) {
                    insertPrimaryKey(primaryKey, clazz, cu);
                }
            }


            LOG.info("Writing " + (isParentClass ? "parent " : "") + "class: " + className);
            Files.write(path, cu.toString().getBytes());
        }

        // Check if some fields are in the parent classes recursively
        final ClassOrInterfaceDeclaration childClass = cu.getClassByName(className).orElseThrow();
        if (!childClass.getExtendedTypes().isEmpty()) {
            final ClassOrInterfaceType resolvedType = childClass.getExtendedTypes().get(0);
            final String parentPackageName = ClassNameUtils.getPackageName(resolvedType.getNameWithScope());
            final String parentClass = resolvedType.getName().asString();

            // Mark this class as processed
            processedClasses.put(className, parentClass);

            if (!allFieldsProcessed(entity)) {
                writeAnnotations(entity, outputFolder, parentPackageName, parentClass, true);
            }
        } else {
            // Mark this class as processed
            processedClasses.put(className, null);
        }
    }

    private boolean allFieldsProcessed(final JpaEntity entity) {
        final List<String> allParentClasses = new ArrayList<>();
        String clazz = entity.getName();
        while (processedClasses.get(clazz) != null) {
            allParentClasses.add(processedClasses.get(clazz));
            clazz = processedClasses.get(clazz);
        }

        final List<String> allProcessedFields = new ArrayList<>();
        allParentClasses.forEach(pc -> {
            if (processedFields.containsKey(pc)) {
                allProcessedFields.addAll(processedFields.get(pc));
            }
        });
        getAllFields(entity).forEach(jpaAnnotation -> {
            if (!jpaAnnotation.isProcessed() &&
                    allProcessedFields.contains(jpaAnnotation.getName())) {
                jpaAnnotation.setProcessed(true);
            }
        });
        return getAllFields(entity).stream().allMatch(JpaAnnotation::isProcessed);
    }

    private static CompilationUnit parseJava(final Path path) throws IOException {
        final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());

        final JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        final ParserConfiguration config = new ParserConfiguration().setSymbolResolver(symbolSolver);
        final JavaParser parser = new JavaParser(config);

        return parser.parse(path).getResult().orElseThrow();
    }

    private void processJpaAnnotation(final String className, final FieldDeclaration field, final JpaAnnotation jpaAnnotation,
            final CompilationUnit cu, final AtomicBoolean hasChanged) {
        if (jpaAnnotation.getType() != null) {
            final String annotationType = ClassNameUtils.getSimpleClassName(
                    HibernateUtils.mapHibernateTypeToJava(jpaAnnotation.getType(), true));
            final String fieldType = ClassNameUtils.getSimpleClassName(extractFullType(field.getVariables().get(0).getType()));

            // Add @Type when the annotation type is different of the field return type
            if (!HibernateUtils.isPrimitiveType(fieldType) && !"Map".equals(fieldType) &&
                    !annotationType.equals(fieldType) &&
                    jpaAnnotation.getAnnotations().stream().noneMatch(ann -> ann.contains("AttributeOverrides"))) {
                final StringBuilder typeAnnotation = new StringBuilder();
                typeAnnotation.append("@org.hibernate.annotations.Type(type = \"").append(jpaAnnotation.getType())
                        .append("\"");
                if (!jpaAnnotation.getTypeParams().isEmpty()) {
                    typeAnnotation.append(",\n        parameters = {\n");
                    for (final Map.Entry<String, String> entry : jpaAnnotation.getTypeParams().entrySet()) {
                        typeAnnotation.append("            @org.hibernate.annotations.Parameter(name = \"").append(
                                entry.getKey());
                        typeAnnotation.append("\", value = \"").append(entry.getValue()).append("\"),\n");
                    }
                    typeAnnotation.append("        }\n    ");
                }
                typeAnnotation.append(")");
                addAnnotations(List.of(typeAnnotation.toString()), field);
            }
        }

        for (final String importString : jpaAnnotation.getImports()) {
            cu.addImport(importString);
        }
        addAnnotations(jpaAnnotation.getAnnotations(), field);
        jpaAnnotation.setProcessed(true);
        hasChanged.set(true);

        // Cache processed fields to improve performance
        final List<String> classProcessedFields;
        if (processedFields.containsKey(className)) {
            classProcessedFields = processedFields.get(className);
        } else {
            classProcessedFields = new ArrayList<>();
            processedFields.put(className, classProcessedFields);
        }
        classProcessedFields.add(jpaAnnotation.getName());
    }

    // This method returns the generic type if present, otherwise the base type
    private static String extractFullType(final Type type) {
        if (type.isClassOrInterfaceType()) {
            final ClassOrInterfaceType cit = type.asClassOrInterfaceType();
            if (cit.getTypeArguments().isPresent()) {
                return cit.getTypeArguments().get().get(0).asString(); // get first generic
            }
        }
        try {
            final ResolvedType resolved = type.resolve();
            return resolved.describe(); // Fully qualified name
        } catch (final Exception ignored) {}
        return type.asString();
    }

    private void insertPrimaryKey(final JpaPrimaryKey primaryKey,
            final ClassOrInterfaceDeclaration clazz, final CompilationUnit cu) {
        final FieldDeclaration newField = StaticJavaParser.parseBodyDeclaration("private " +
                primaryKey.getType() + " " + primaryKey.getName() + ";").asFieldDeclaration();

        // Check the index to insert right after constants and as the first field
        int insertIndex = 0;
        for (int i = 0; i < clazz.getMembers().size(); i++) {
            if (clazz.getMember(i).isFieldDeclaration()) {
                final FieldDeclaration field = clazz.getMember(i).asFieldDeclaration();
                if (field.isStatic() && field.isFinal()) {
                    insertIndex = i + 1; // Insert after this constant
                } else {
                    break; // Stop at the first non-constant field
                }
            } else {
                break; // Stop if first method/class/etc. appears
            }
        }

        clazz.getMembers().add(insertIndex, newField);

        for (final String importString : primaryKey.getImports()) {
            cu.addImport(importString);
        }
        addAnnotations(primaryKey.getAnnotations(), newField);
        primaryKey.setProcessed(true);
    }

    private void addAnnotations(final List<String> newAnnotations, final NodeWithAnnotations<?> node) {
        for (final String annotationString : newAnnotations) {
            final AnnotationExpr annotation = toAnnotationExpr(annotationString);
            final String annotationName = annotation.getNameAsString();

            // Check if the node already has the annotation
            final boolean alreadyAnnotated = node.getAnnotations().stream()
                    .anyMatch(a -> a.getNameAsString().equals(annotationName));

            if (!alreadyAnnotated) {
                node.addAnnotation(annotation);
            }
        }
    }

    private AnnotationExpr toAnnotationExpr(final String annotationString) {
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

    private static Map<String, FieldDeclaration> findFieldsWithNonStandardGetters(final ClassOrInterfaceDeclaration clazz) {
        final Map<String, FieldDeclaration> nonStandardGetters = new HashMap<>();

        final Map<String, FieldDeclaration> fields = clazz.getFields().stream()
                .flatMap(f -> f.getVariables().stream().map(v -> Map.entry(v.getNameAsString(), f)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (final MethodDeclaration method : clazz.getMethods()) {
            if (method.getBody().isEmpty()) continue;

            method.findAll(ReturnStmt.class).stream()
                    .map(ReturnStmt::getExpression)
                    .flatMap(Optional::stream)
                    .filter(Expression::isNameExpr)
                    .map(expr -> ((NameExpr) expr).getNameAsString())
                    .filter(fields::containsKey)
                    .forEach(fieldName -> {
                        final String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                        final String actualMethod = method.getNameAsString();

                        if (!actualMethod.equals(getterName)) {
                            final String syntheticFieldName = uncapitalize(actualMethod.replaceFirst("^get", ""));
                            if (!nonStandardGetters.containsKey(syntheticFieldName)) {
                                nonStandardGetters.put(syntheticFieldName, fields.get(fieldName));
                            }
                        }
                    });
        }
        return nonStandardGetters;
    }

    private static String uncapitalize(final String str) {
        return str.isEmpty() ? str : Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private static void validateFieldsNotFound(final JpaEntity entity) {
        for (final JpaAnnotation field : getAllFields(entity)) {
            if (!field.isProcessed()) {
                LOG.error("Field " + field.getName() + " not found for " + entity.getName());
            }
        }
    }

    private static List<JpaAnnotation> getAllFields(final JpaEntity entity) {
        final List<JpaAnnotation> allFields = new ArrayList<>();
        if (entity.getPrimaryKey() != null) {
            allFields.add(entity.getPrimaryKey());
        }
        allFields.addAll(entity.getColumns());
        allFields.addAll(entity.getRelationships());
        return allFields;
    }
}
