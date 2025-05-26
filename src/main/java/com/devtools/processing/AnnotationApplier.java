package com.devtools.processing;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.devtools.model.jpa.JpaAbstract;
import com.devtools.model.jpa.JpaEntity;
import com.devtools.model.jpa.JpaPrimaryKey;
import com.devtools.utils.ClassNameUtils;
import com.devtools.utils.FileUtils;
import com.devtools.utils.HibernateUtils;
import com.devtools.utils.JavaParserUtils;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public class AnnotationApplier {

    private static final Log LOG = LogFactory.getLog(AnnotationApplier.class);

    private static final String IGNORE_PROPERTIES = "ignore.properties";

    private final String outputFolder;

    // Initialize cache to track processed classes across the entire entity hierarchy
    private static final Map<String, String> PROCESSED_CLASSES = new HashMap<>();
    private static final Map<String, Set<String>> PROCESSED_FIELDS = new HashMap<>();
    private static final Map<String, Set<String>> IGNORED_FIELDS = new HashMap<>();

    public AnnotationApplier(final String outputFolder) {
        this.outputFolder = outputFolder;

        if (PROCESSED_CLASSES.isEmpty()) {
            final Map<String, Set<String>> ignoreFields = FileUtils.readPropertiesFile(IGNORE_PROPERTIES);
            IGNORED_FIELDS.putAll(ignoreFields);
        }
    }

    public void applyAnnotations(final JpaEntity entity) throws IOException {

        writeAnnotations(entity, entity.getType(), false);

        // Check if some element parsed from the hbm.xml has no corresponding field in the class
        validateFieldsNotFound(entity);
    }

    private void writeAnnotations(final JpaEntity entity, final String fullClassName, final boolean isParentClass)
            throws IOException {
        final String simpleClassName = ClassNameUtils.getSimpleClassName(fullClassName);

        // Parse the file
        final CompilationUnit cu = JavaParserUtils.parseJava(outputFolder, fullClassName);
        if (cu == null) {
            // Add this failure as a processed class, so we don't check it again later
            PROCESSED_CLASSES.put(simpleClassName, null);
            if (!isParentClass && !entity.isEmbeddable()) {
                LOG.warn("Generating a new one in " + outputFolder);
                final EntityGenerator entityGenerator = new EntityGenerator();
                entityGenerator.generate(entity, outputFolder);
            }
            return;
        }

        final ClassOrInterfaceDeclaration clazz = cu.findAll(ClassOrInterfaceDeclaration.class).get(0);
        final AtomicBoolean entityChanged = new AtomicBoolean(false);

        if (!isParentClass) {
            // Add imports to the class
            entity.getImports().forEach(cu::addImport);

            // Add Entity annotations to the class
            JavaParserUtils.addAnnotations(entity.getAnnotations(), clazz);
            entityChanged.set(true);
        }

        final List<JpaAbstract> allFields = getAllFields(entity);

        // Add annotations to the fields
        cu.findAll(FieldDeclaration.class).forEach(field -> {
            for (final VariableDeclarator variable : field.getVariables()) {
                // If there is a sequence, the @Id must be defined in the class itself, not in the parent
                if (isParentClass && entity.getPrimaryKey() != null &&
                    entity.getPrimaryKey().getName().equalsIgnoreCase(variable.getNameAsString()) &&
                     StringUtils.isNotBlank(entity.getPrimaryKey().getGeneratorType())) {
                    LOG.warn("An 'id' field was found in the parent class " + simpleClassName + ", you should probably remove it");
                    continue;
                }

                allFields.stream()
                        .filter(jpaElement -> !jpaElement.isProcessed() && jpaElement.getName() != null &&
                                              jpaElement.getName().equals(variable.getNameAsString()))
                        .forEach(jpaElement -> annotateField(simpleClassName, field, jpaElement, cu, entityChanged));
            }
        });

        if (hasPendingFields(entity)) {
            // Check for all non-standard getters (mapped in the hbm.xml with a different name, instead of the real field name)
            // Example: getBlaBla() { return bla; } -> in this case "blaBla" was mapped in .hbm.xml, instead of "bla"
            final Map<String, FieldDeclaration> nonStandardGetters = JavaParserUtils.findFieldsWithNonStandardGetters(
                    clazz);
            nonStandardGetters.forEach((nonStandardName, field) -> allFields.stream()
                    .filter(jpaElement -> !jpaElement.isProcessed() && jpaElement.getName() != null &&
                                          jpaElement.getName().equals(nonStandardName))
                    .forEach(jpaElement -> annotateField(simpleClassName, field, jpaElement, cu, entityChanged)));
        }

        // Check if some fields are in the parent classes recursively
        final Optional<ClassOrInterfaceDeclaration> childClass = cu.getClassByName(simpleClassName);
        if (childClass.isPresent() && !childClass.get().getExtendedTypes().isEmpty()) {
            final ClassOrInterfaceType resolvedType = childClass.get().getExtendedTypes().get(0);
            final String parentSimpleClassName = resolvedType.getName().asString();
            final String parentFullClassName = resolvedType.getNameWithScope();

            // Mark this class as processed
            PROCESSED_CLASSES.put(simpleClassName, parentSimpleClassName);

            if (hasPendingFields(entity) && !PROCESSED_CLASSES.containsKey(parentSimpleClassName)) {
                writeAnnotations(entity, parentFullClassName, true);
            }

        } else {
            // Mark this class as processed
            PROCESSED_CLASSES.put(simpleClassName, null);
        }

        // Write the modified file back
        if (entityChanged.get()) {
            LOG.info("Writing " + (isParentClass ? "parent " : "") + "class: " + simpleClassName);
            writeFileClass(entity, isParentClass, cu, clazz);
        }
    }

    private void writeFileClass(final JpaEntity entity, final boolean isParentClass,
            final CompilationUnit cu, final ClassOrInterfaceDeclaration clazz) throws IOException {
        if (isParentClass) {
            cu.addImport("javax.persistence.MappedSuperclass");
            JavaParserUtils.addAnnotations(List.of("@MappedSuperclass"), clazz);
        } else {
            // If id field was not found in the class (probably it is in the super class)
            // and it was defined in the mapping, we will create it at the beginning of the class
            final JpaPrimaryKey primaryKey = entity.getPrimaryKey();
            if (primaryKey != null && !primaryKey.isProcessed()) {
                insertPrimaryKey(primaryKey, clazz, cu);
            }
        }

        Files.write(cu.getStorage().orElseThrow().getPath(), cu.toString().getBytes());
    }

    private static List<JpaAbstract> getAllFields(final JpaEntity entity) {
        final List<JpaAbstract> allFields = new ArrayList<>();
        if (entity.getPrimaryKey() != null) {
            allFields.add(entity.getPrimaryKey());
        }
        allFields.addAll(entity.getColumns());
        allFields.addAll(entity.getCompositeColumns());
        allFields.addAll(entity.getRelationships());
        return allFields;
    }

    private boolean hasPendingFields(final JpaEntity entity) {
        String clazz = entity.getSimpleName();
        final Set<String> allParentClasses = new HashSet<>();
        do {
            allParentClasses.add(clazz);
        } while ((clazz = PROCESSED_CLASSES.get(clazz)) != null);

        final Set<String> allProcessedFields = new HashSet<>();
        IGNORED_FIELDS.values().forEach(allProcessedFields::addAll);

        allParentClasses.forEach(pc -> {
            if (PROCESSED_FIELDS.containsKey(pc)) {
                allProcessedFields.addAll(PROCESSED_FIELDS.get(pc));
            }
        });

        getAllFields(entity).forEach(jpaElement -> {
            if (!jpaElement.isProcessed() &&
                    allProcessedFields.contains(jpaElement.getName())) {
                jpaElement.setProcessed(true);
            }
        });
        return !getAllFields(entity).stream().allMatch(JpaAbstract::isProcessed);
    }

    private void annotateField(final String simpleClassName, final FieldDeclaration field, final JpaAbstract jpaElement,
            final CompilationUnit cu, final AtomicBoolean entityChanged) {

        addTypeAnnotationIfNeeded(field, jpaElement);

        for (final String importString : jpaElement.getImports()) {
            cu.addImport(importString);
        }
        JavaParserUtils.addAnnotations(jpaElement.getAnnotations(), field);
        jpaElement.setProcessed(true);
        entityChanged.set(true);

        // Cache processed fields to improve performance
        final Set<String> classProcessedFields;
        if (PROCESSED_FIELDS.containsKey(simpleClassName)) {
            classProcessedFields = PROCESSED_FIELDS.get(simpleClassName);
        } else {
            classProcessedFields = new HashSet<>();
            PROCESSED_FIELDS.put(simpleClassName, classProcessedFields);
        }
        classProcessedFields.add(jpaElement.getName());
    }

    private static void addTypeAnnotationIfNeeded(final FieldDeclaration field, final JpaAbstract jpaElement) {
        if (jpaElement.getType() != null) {
            final String annotationType = HibernateUtils.mapHibernateTypeToJava(jpaElement.getType());
            final String fieldType = ClassNameUtils.getSimpleClassName(
                    JavaParserUtils.resolveGenericType(field.getVariables().get(0).getType()));

            // Add @Type when the annotation type is different of the field return type
            // except when it's a component (with @AttributeOverrides annotation)
            if (StringUtils.isNotBlank(fieldType) &&
                !HibernateUtils.isPrimitiveType(fieldType) && !annotationType.equals(fieldType) &&
                jpaElement.getAnnotations().stream().noneMatch(ann -> ann.contains("AttributeOverrides"))) {
                final StringBuilder typeAnnotation = new StringBuilder();
                typeAnnotation.append("@org.hibernate.annotations.Type(type = \"").append(jpaElement.getType())
                        .append("\"");
                if (!jpaElement.getTypeParams().isEmpty()) {
                    typeAnnotation.append(",\n        parameters = {\n");
                    for (final Map.Entry<String, String> entry : jpaElement.getTypeParams().entrySet()) {
                        typeAnnotation.append("            @org.hibernate.annotations.Parameter(name = \"").append(
                                entry.getKey());
                        typeAnnotation.append("\", value = \"").append(entry.getValue()).append("\"),\n");
                    }
                    typeAnnotation.append("        }\n    ");
                }
                typeAnnotation.append(")");
                JavaParserUtils.addAnnotations(List.of(typeAnnotation.toString()), field);
            }
        }
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
        JavaParserUtils.createGetterAndSetter(clazz, primaryKey.getName());

        for (final String importString : primaryKey.getImports()) {
            cu.addImport(importString);
        }
        JavaParserUtils.addAnnotations(primaryKey.getAnnotations(), newField);
        primaryKey.setProcessed(true);
    }

    private static void validateFieldsNotFound(final JpaEntity entity) {
        getAllFields(entity).stream().filter(jpaElement -> !jpaElement.isProcessed()).forEach(jpaElement ->
                LOG.error("Mapping \"" + jpaElement.getName() + "\" not found for " + entity.getSimpleName()));
    }
}
