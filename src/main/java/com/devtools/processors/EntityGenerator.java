package com.devtools.processors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.devtools.Utils;
import com.devtools.definition.JpaBase;
import com.devtools.definition.JpaColumn;
import com.devtools.definition.JpaEntity;
import com.devtools.definition.JpaNamedQuery;
import com.devtools.definition.JpaRelationship;
import com.devtools.definition.Tags;

public class EntityGenerator {

    public void generate(final JpaBase jpaBase, final String outputFolder) throws IOException {
        for (final JpaEntity entityDef : jpaBase.getEntities()) {
            generateEntity(outputFolder, entityDef);
        }
    }

    private void generateEntity(final String outputFolder, final JpaEntity entityDef) throws IOException {
        final StringBuilder entityCode = new StringBuilder();

        // Add package declaration and imports
        generateHeaders(entityDef, entityCode);

        generateQueries(entityDef, entityCode);

        generateNativeQueries(entityDef, entityCode);

        generateEntity(entityDef, entityCode);

        if (entityDef.getPrimaryKey() != null) {
            // Generate the @Id and @GeneratedValue annotations
            generatePrimaryKey(entityDef, entityCode);
        }

        // Add properties with @Column annotations
        generateColumns(entityDef, entityCode);

        generateAttributeOverrides(entityDef, entityCode);

        // Handle relationships (ManyToOne, OneToMany, OneToOne, ManyToMany)
        generateRelationships(entityDef, entityCode);

        // Handle Embedded fields and Embeddable classes
        generateEmbedded(entityDef, entityCode, outputFolder);

        // Handle default values
        generatePrePersist(entityDef, entityCode);

        // Close the class definition
        entityCode.append("}\n");

        Utils.writeEntity(outputFolder + File.separator + entityDef.getClassName() + ".java", entityCode);
    }

    private void generateHeaders(final JpaEntity entityDef, final StringBuilder entityCode) {
        entityCode.append("package ").append(entityDef.getPackageName()).append(";\n\n");

        entityCode.append("import javax.persistence.*;\n");
        entityCode.append("import java.util.*;\n");

        for (final JpaRelationship relationship : entityDef.getRelationships()) {
            // Add to imports if it's not a native type
            if (!Utils.isNativeType(relationship.getSimpleClass())) {
                entityCode.append("import ").append(relationship.getTargetEntity()).append(";\n");
            }
        }
    }

    private void generateEntity(final JpaEntity entityDef, final StringBuilder entityCode) {
        if (entityDef.isAbstractClass() && StringUtils.isNotBlank(entityDef.getParentClass())) {
            entityCode.append("\n@MappedSuperclass");
        }

        if (entityDef.isEmbeddable()) {
            entityCode.append("\n@Embeddable\n");
        } else {
            entityCode.append("\n@Entity\n");
        }

        if (StringUtils.isNotBlank(entityDef.getTable())) {
            final StringBuilder indexes = generateIndexes(entityDef);
            final Map<String, StringBuilder> uniqueConstraints = generateUniqueConstraints(entityDef);

            if (entityDef.isSecondTable()) {
                entityCode.append("@SecondaryTable(name = \"");
            } else {
                entityCode.append("@Table(name = \"");
            }
            entityCode.append(entityDef.getTable()).append("\"");

            if (!indexes.isEmpty()) {
                entityCode.append(",\n    indexes = {\n").append(indexes).append("\n    }");
            }
            if (!uniqueConstraints.isEmpty()) {
                entityCode.append(",\n    uniqueConstraints = {\n");
                for (final Map.Entry<String, StringBuilder> entry : uniqueConstraints.entrySet()) {
                    entityCode.append("        @UniqueConstraint(name = \"");
                    entityCode.append(entry.getKey()).append("\", columnNames = {");
                    entityCode.append(entry.getValue()).append("})\n");
                }
                entityCode.append("    }\n");
            }
            entityCode.append(")\n");
        }

        if (entityDef.isSecondTable()) {
            entityCode.append("@org.hibernate.annotations.Table(appliesTo = \"");
            entityCode.append(entityDef.getTable()).append("\", optional = false)\n");
        }

        if (StringUtils.isNotBlank(entityDef.getCacheUsage())) {
            entityCode.append("@Cacheable\n");
            switch (entityDef.getCacheUsage()) {
                case "read-only":
                    entityCode.append("@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)\n");
                    break;
                case "read-write":
                    entityCode.append("@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)\n");
                    break;
                case "nonstrict-read-write":
                    entityCode.append("@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)\n");
                    break;
                case "transactional":
                    entityCode.append("@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)\n");
                    break;
                default:
                    break;
            }
        }

        if (entityDef.isImmutable()) {
            entityCode.append("@org.hibernate.annotations.Immutable\n");
        }

        if (entityDef.isDynamicInsert()) {
            entityCode.append("@org.hibernate.annotations.DynamicInsert\n");
        }

        if (entityDef.isDynamicUpdate()) {
            entityCode.append("@org.hibernate.annotations.DynamicUpdate\n");
        }

        if (entityDef.getDiscriminator() != null && StringUtils.isNotBlank(entityDef.getDiscriminator().getValue())) {
            entityCode.append("@DiscriminatorValue(\"").append(entityDef.getDiscriminator().getValue()).append("\")\n");
        }

        if (StringUtils.isNotBlank(entityDef.getParentClass())) {
            // Class Declaration
            entityCode.append("public ");
            if (entityDef.isAbstractClass()) {
                entityCode.append("abstract ");
            }

            if (entityDef.isEmbeddable()) {
                entityCode.append("class ").append(entityDef.getClassName()).append(" {\n\n");
            } else {
                entityCode.append("class ").append(entityDef.getClassName())
                        .append(" extends ").append(entityDef.getParentClass()).append(" {\n\n");
            }
        } else {
            // Handle Discriminator Column
            if (entityDef.getDiscriminator() != null && entityDef.getDiscriminator().getColumn() != null) {
                entityCode.append("@DiscriminatorColumn(name = \"").append(entityDef.getDiscriminator().getColumn()).append("\"");
                if (entityDef.getDiscriminator().getType() != null &&
                        !"string".equals(entityDef.getDiscriminator().getType())) {
                    entityCode.append(", type = ").append(Utils.getDiscriminatorType(entityDef.getDiscriminator().getType()));
                }
                if (entityDef.getDiscriminator().getLength() != 31) {
                    entityCode.append(", length = ").append(entityDef.getDiscriminator().getLength());
                }
                entityCode.append(")\n");
                entityCode.append("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)\n");
            }

            entityCode.append("public ");
            if (entityDef.isAbstractClass()) {
                entityCode.append("abstract ");
            }

            entityCode.append("class ").append(entityDef.getClassName()).append(" {\n\n");
        }
    }

    private StringBuilder generateIndexes(final JpaEntity entityDef) {
        final Map<String, List<String>> indexesMap = new HashMap<>();

        for (final JpaRelationship relationship : entityDef.getRelationships()) {
            for (final JpaColumn column : relationship.getReferencedColumns()) {
                if (StringUtils.isNotBlank(column.getIndex())) {
                    for (final String index : column.getIndex().split(",")) {
                        final List<String> columnList;
                        if (indexesMap.containsKey(index)) {
                            columnList = indexesMap.get(index);
                        } else {
                            columnList = new ArrayList<>();
                            indexesMap.put(index, columnList);
                        }
                        columnList.add(column.getColumnName());
                    }
                }
            }
        }

        for (final JpaColumn column : entityDef.getColumns()) {
            if (StringUtils.isNotBlank(column.getIndex())) {
                for (final String index : column.getIndex().split(",")) {
                    final List<String> columnList;
                    if (indexesMap.containsKey(index)) {
                        columnList = indexesMap.get(index);
                    } else {
                        columnList = new ArrayList<>();
                        indexesMap.put(index, columnList);
                    }
                    columnList.add(column.getColumnName());
                }
            }
        }

        final StringBuilder indexes = new StringBuilder();
        for(final Map.Entry<String, List<String>> entry : indexesMap.entrySet()) {
            indexes.append("        @Index(name = \"").append(entry.getKey()).append("\", columnList = \"")
                    .append(String.join(",", entry.getValue())).append("\"),\n");
        }
        return indexes;
    }

    private Map<String, StringBuilder> generateUniqueConstraints(final JpaEntity entityDef) {
        final Map<String, List<JpaColumn>> columnsMap = new HashMap<>();
        final List<JpaColumn> allColumns = new ArrayList<>(entityDef.getColumns());
        for (final JpaRelationship relationship : entityDef.getRelationships()) {
            allColumns.addAll(relationship.getReferencedColumns());
        }
        for (final JpaColumn column : allColumns) {
            if (StringUtils.isBlank(column.getUniqueConstraint())) {
                continue;
            }

            for (final String uniqueConstraint : column.getUniqueConstraint().split(",")) {
                final List<JpaColumn> jpaColumns;
                if (columnsMap.containsKey(uniqueConstraint)) {
                    jpaColumns = columnsMap.get(uniqueConstraint);
                } else {
                    jpaColumns = new ArrayList<>();
                    columnsMap.put(uniqueConstraint, jpaColumns);
                }
                jpaColumns.add(column);
            }
        }

        final Map<String, StringBuilder> constraintsMap = new HashMap<>();
        for (final Map.Entry<String, List<JpaColumn>> entry : columnsMap.entrySet()) {
            final StringBuilder uniqueConstraints = new StringBuilder();
            for (final JpaColumn column : entry.getValue()) {
                uniqueConstraints.append("\"").append(column.getColumnName()).append("\", ");
            }
            constraintsMap.put(entry.getKey(), uniqueConstraints);
        }
        return constraintsMap;
    }

    private void generatePrimaryKey(final JpaEntity entityDef, final StringBuilder entityCode) {
        if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getGeneratorType())) {
            entityCode.append("    @Id\n");

            switch (entityDef.getPrimaryKey().getGeneratorType()) {
            case "SEQUENCE":
                entityCode.append("    @GeneratedValue(strategy = GenerationType.SEQUENCE");
                if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getGeneratorName())) {
                    final String generatorName = entityDef.getPrimaryKey().getGeneratorName();
                    final Integer initialValue = entityDef.getPrimaryKey().getInitialValue();
                    final Integer allocationSize = entityDef.getPrimaryKey().getAllocationSize();

                    entityCode.append(", generator = \"gen").append(entityDef.getClassName()).append("\"");
                    // Add the @SequenceGenerator
                    entityCode.append(")\n");
                    entityCode.append("    @SequenceGenerator(name = \"gen").append(entityDef.getClassName())
                            .append("\", sequenceName = \"").append(generatorName).append("\"")
                            .append(initialValue != null ?
                                    ", allocationSize = " + allocationSize + ", initialValue = " + initialValue :
                                    "")
                            .append(")\n");
                } else {
                    entityCode.append(")\n");
                }
                break;
            case "IDENTITY":
                entityCode.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
                break;
            case "TABLE":
                entityCode.append("    @GeneratedValue(strategy = GenerationType.TABLE)\n");
                break;
            case "FOREIGN":
                break; // it will be defined in the OneToOne
            case "GENERATOR":
                if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getGeneratorName())) {
                    entityCode.append("    @GeneratedValue(generator = \"")
                            .append(entityDef.getPrimaryKey().getGeneratorName())
                            .append("\")\n");
                }
                break;
            default:
                entityCode.append("    @GeneratedValue(strategy = GenerationType.AUTO)\n");
                break;
            }
        }

        entityCode.append("    @Column(name = \"").append(entityDef.getPrimaryKey().getColumnName()).append("\")\n");
        entityCode.append("    private ").append(entityDef.getPrimaryKey().getType())
                .append(" ").append(entityDef.getPrimaryKey().getName())
                .append(";\n\n");
    }

    private void generateColumns(final JpaEntity entityDef, final StringBuilder entityCode) {
        for (final JpaColumn col : entityDef.getColumns()) {
            if (col.isComposite()) {
                continue;
            }
            if (col.isVersion()) {
                entityCode.append("    @Version\n");
            }
            if (col.getNaturalId() != JpaColumn.NaturalId.NONE) {
                entityCode.append("    @org.hibernate.annotations.NaturalId");
                entityCode.append(col.getNaturalId() == JpaColumn.NaturalId.MUTABLE ? "(mutable = true)" : "");
                entityCode.append("\n");
            }
            if (col.getType() != null && col.getType(false).endsWith("Type")) {
                entityCode.append("    @org.hibernate.annotations.Type(type = \"").append(col.getType(false)).append("\"");
                if (!col.getTypeParams().isEmpty()) {
                    entityCode.append(",\n        parameters = {\n");
                    for (final Map.Entry<String, String> entry : col.getTypeParams().entrySet()) {
                        entityCode.append("            @org.hibernate.annotations.Parameter(name = \"").append(entry.getKey());
                        entityCode.append("\", value = \"").append(entry.getValue()).append("\"),\n");
                    }
                    entityCode.append("        }\n    ");
                }
                entityCode.append(")\n");
            }
            if (col.isLazy()) {
                entityCode.append("    @Basic(fetch = FetchType.LAZY)\n");
            }
            entityCode.append("    ");
            generateColumn(entityDef, entityCode, col);

            if (!col.isOptimisticLock()) {
                entityCode.append("\n    @org.hibernate.annotations.OptimisticLock(excluded = true)");
            }
            entityCode.append("\n    private ").append(Utils.mapHibernateTypeToJava(col.getType()))
                    .append(" ").append(col.getName()).append(";\n\n");
        }
    }

    private void generateColumn(final JpaEntity entityDef, final StringBuilder entityCode, final JpaColumn col) {
        entityCode.append("@Column(");
        if (entityDef.isSecondTable()) {
            entityCode.append("table = \"").append(entityDef.getTable()).append("\", ");
        }
        entityCode.append("name = \"").append(col.getColumnName()).append("\"");

        if (col.getLength() != null && col.getLength() != 255) {
            entityCode.append(", length = ").append(col.getLength());
        }
        if (!col.isNullable()) {
            entityCode.append(", nullable = false");
        }
        if (!col.isUpdatable()) {
            entityCode.append(", updatable = false");
        }
        if (col.isUnique()) {
            entityCode.append(", unique = true");
        }
        if (StringUtils.isNotBlank(col.getColumnDefinition())) {
            entityCode.append(", columnDefinition = \"").append(col.getColumnDefinition()).append("\"");
        }
        if (col.getPrecision() != null && col.getPrecision() != 0) {
            entityCode.append(", precision = \"").append(col.getPrecision()).append("\"");
        }
        if (col.getScale() != null && col.getScale() != 0) {
            entityCode.append(", scale = \"").append(col.getScale()).append("\"");
        }

        entityCode.append(")");
    }

    private void generateAttributeOverrides(final JpaEntity entityDef, final StringBuilder entityCode) {
        final Map<String, List<JpaColumn>> attributeOverrides = new HashMap<>();
        // First group all columns that should be treated as AttributeOverrides
        for (final JpaColumn col : entityDef.getColumns()) {
            if (!col.isComposite()) {
                continue;
            }
            final List<JpaColumn> jpaColumns;
            if (attributeOverrides.containsKey(col.getName())) {
                jpaColumns = attributeOverrides.get(col.getName());
            } else {
                jpaColumns = new ArrayList<>();
                attributeOverrides.put(col.getName(), jpaColumns);
            }
            jpaColumns.add(col);
        }

        for (final Map.Entry<String, List<JpaColumn>> entry : attributeOverrides.entrySet()) {
            entityCode.append("    // TODO check correct name for each field in the class ");
            entityCode.append(entry.getValue().getFirst().getType()).append("\n");
            entityCode.append("    // TODO probably need to replace UserType class by the @Embeddable class (that must be manually adapted)\n");
            entityCode.append("    @AttributeOverrides({\n");
            JpaColumn varColumn = null;
            for (final JpaColumn column : entry.getValue()) {
                entityCode.append("        @AttributeOverride(name = \"");
                entityCode.append(Utils.toCamelCase(column.getColumnName()));
                entityCode.append("\", column = ");
                generateColumn(entityDef, entityCode, column);
                entityCode.append("),\n");
                varColumn = column;
            }
            entityCode.append("    })\n");
            assert varColumn != null;
            entityCode.append("    private ").append(varColumn.getType())
                    .append(" ").append(varColumn.getName()).append(";\n\n");
        }
    }

    private void generateRelationships(final JpaEntity entityDef, final StringBuilder entityCode) {
        for (final JpaRelationship relationship : entityDef.getRelationships()) {
            final StringBuilder annotations = new StringBuilder();

            // Fetch Type (Default to LAZY if not specified)
            final String fetchType = ("eager".equals(relationship.getFetch()) || "join".equals(relationship.getFetch())) ?
                    "EAGER" : "LAZY";

            // Cascade Types
            final String cascadeTypes = Utils.convertCascadeTypes(relationship.getCascade());
            final StringBuilder cascadeAnnotation = new StringBuilder();
            if (!cascadeTypes.isEmpty()) {
                cascadeAnnotation.append("cascade = {").append(cascadeTypes).append("}");
                if (relationship.getCascade().contains("delete-orphan")) {
                    cascadeAnnotation.append(", orphanRemoval = true");
                }
            }

            String joinColumn = "";
            if (relationship.getReferencedColumns() != null && !relationship.getReferencedColumns().isEmpty()) {
                final JpaColumn referencedColumn = relationship.getReferencedColumns().getFirst();
                joinColumn = "@JoinColumn(name = \"" + referencedColumn.getColumnName() + "\"" +
                        (!referencedColumn.isUpdatable() ? ", updatable = false" : "") +
                        (!referencedColumn.isNullable() ? ", nullable = false" : "") +
                        (referencedColumn.isUnique() ? ", unique = true" : "") +
                        (StringUtils.isNotBlank(referencedColumn.getForeignKey()) ?
                                ", foreignKey = @ForeignKey(name = \"" +
                                        referencedColumn.getForeignKey() + "\")" : "") +
                        ")\n";
            }

            // Generate the appropriate relationship annotation
            switch (relationship.getType()) {
                case ManyToOne:
                    if (StringUtils.isNotBlank(relationship.getAccess())) {
                        annotations.append("    @Access(AccessType.")
                                .append(relationship.getAccess().toUpperCase()).append(")\n");
                    }
                    annotations.append("    @ManyToOne(fetch = FetchType.").append(fetchType).append(", ");
                    annotations.append(cascadeAnnotation).append(")\n");
                    if (!joinColumn.isEmpty()) {
                        annotations.append("    ").append(joinColumn);
                    }
                    break;

                case OneToMany:
                    if (relationship.isInverse()) {
                        annotations.append("    // TODO check correct name for 'mappedBy' in the class ");
                        annotations.append(relationship.getTargetEntity()).append("\n");
                    }
                    annotations.append("    @OneToMany(");
                    // LAZY fetch is default for OneToMany
                    if (!"LAZY".equals(fetchType)) {
                        annotations.append("fetch = FetchType.").append(fetchType).append(", ");
                    }
                    annotations.append(cascadeAnnotation);
                    if (relationship.isInverse()) {
                        String mappedBy = relationship.getMappedBy();
                        if (StringUtils.isBlank(mappedBy)) {
                            mappedBy = Utils.lowercaseUntilLastUpper(entityDef.getClassName());
                        }
                        annotations.append(", mappedBy = \"").append(mappedBy).append("\"");
                    }
                    annotations.append(")\n");
                    if (!joinColumn.isEmpty()) {
                        annotations.append("    ").append(joinColumn);
                    }
                    if (StringUtils.isNotBlank(relationship.getOrderColumn())) {
                        annotations.append("    @OrderBy(\"").append(relationship.getOrderColumn()).append("\")\n");
                    }
                    break;

                case OneToOne:
                    if (entityDef.getPrimaryKey() != null &&
                            "FOREIGN".equals(entityDef.getPrimaryKey().getGeneratorType()) &&
                            relationship.getName().equals(entityDef.getPrimaryKey().getGeneratorName())) {
                        annotations.append("    @MapsId\n");
                    }
                    annotations.append("    @OneToOne(");
                    if ("LAZY".equals(fetchType)) {
                        annotations.append("fetch = FetchType.").append(fetchType).append(", ");
                    }
                    if (!relationship.isOptional()) {
                        annotations.append("optional = false, ");
                    }
                    if (StringUtils.isNotBlank(relationship.getMappedBy())) {
                        annotations.append("mappedBy = \"").append(relationship.getMappedBy()).append("\", ");
                    }
                    annotations.append(cascadeAnnotation).append(")\n");
                    if (!joinColumn.isEmpty()) {
                        annotations.append("    ").append(joinColumn);
                    }
                    break;

                case ManyToMany:
                    annotations.append("    @ManyToMany(fetch = FetchType.").append(fetchType);
                    annotations.append(StringUtils.isNotBlank(cascadeAnnotation) ? ", " + cascadeAnnotation : "").append(")\n");
                    if (relationship.getReferencedColumns() != null && !relationship.getReferencedColumns().isEmpty()) {
                        annotations.append("    @JoinTable(\n");
                        annotations.append("        name = \"").append(relationship.getTable()).append("\",\n");

                        if (relationship.getReferencedColumns().stream().anyMatch(jpaColumn -> !jpaColumn.isInverseJoin())) {
                            annotations.append("        joinColumns = {\n");
                            for (final JpaColumn column : relationship.getReferencedColumns()) {
                                if (!column.isInverseJoin()) {
                                    annotations.append("            @JoinColumn(name = \"").append(
                                                    column.getColumnName())
                                            .append("\"");
                                    annotations.append(!column.isUpdatable() ? ", updatable = false" : "")
                                            .append(!column.isNullable() ? ", nullable = false" : "")
                                            .append(StringUtils.isNotBlank(column.getForeignKey()) ?
                                                    ", foreignKey = @ForeignKey(name = \"" + column.getForeignKey()
                                                            + "\")" : "");
                                    annotations.append(")\n");
                                }
                            }
                            annotations.append("        },\n");
                        }

                        if (relationship.getReferencedColumns().stream().anyMatch(JpaColumn::isInverseJoin)) {
                            annotations.append("        inverseJoinColumns = {\n");
                            for (final JpaColumn column : relationship.getReferencedColumns()) {
                                if (column.isInverseJoin()) {
                                    annotations.append("            @JoinColumn(name = \"").append(
                                                    column.getColumnName())
                                            .append("\"");
                                    annotations.append(!column.isUpdatable() ? ", updatable = false" : "")
                                            .append(!column.isNullable() ? ", nullable = false" : "")
                                            .append(StringUtils.isNotBlank(column.getForeignKey()) ?
                                                    ", foreignKey = @ForeignKey(name = \"" + column.getForeignKey()
                                                            + "\")" : "");
                                    annotations.append(")\n");
                                }
                            }
                            annotations.append("        }\n");
                        }
                        annotations.append("    )\n");
                    }
                    break;
            }

            if (Tags.TAG_MAP.equals(relationship.getCollectionType()) && relationship.getReferencedColumns() != null) {
                if (relationship.getReferencedColumns().size() > 1) {
                    entityCode.append("    @MapKeyClass(").append(relationship.getReferencedColumns().getFirst().getType())
                            .append(".class)\n");
                    entityCode.append("    @MapKeyEmbedded\n");
                } else {
                    entityCode.append("    @MapKeyColumn(name = \"");
                    entityCode.append(relationship.getReferencedColumns().getFirst().getName()).append("\")\n");
                }
            }

            // Add the generated relationship field
            entityCode.append(annotations);
            entityCode.append("    private ");
            if (relationship.getType().equals(JpaRelationship.Type.OneToMany) ||
                    relationship.getType().equals(JpaRelationship.Type.ManyToMany)) {
                switch (relationship.getCollectionType()) {
                    case Tags.TAG_SET:
                        entityCode.append("Set<");
                        break;
                    case Tags.TAG_MAP:
                        entityCode.append("Map<");
                        break;
                    default:
                        entityCode.append("List<");
                        break;
                }
            }

            if (Tags.TAG_MAP.equals(relationship.getCollectionType())) {
                entityCode.append(relationship.getReferencedColumns().getFirst().getType()).append(", ");
                entityCode.append(relationship.getTargetEntity());
            } else {
                entityCode.append(relationship.getSimpleClass());
            }

            if (relationship.getType().equals(JpaRelationship.Type.OneToMany) ||
                    relationship.getType().equals(JpaRelationship.Type.ManyToMany)) {
                entityCode.append(">");
            }
            entityCode.append(" ").append(relationship.getName()).append(";\n\n");
        }
    }

    private void generateEmbedded(final JpaEntity entityDef, final StringBuilder entityCode, final String outputFolder)
            throws IOException {
        for (final JpaEntity embeddedField : entityDef.getEmbeddedFields()) {
            entityCode.append("    @Embedded\n");
            entityCode.append("    private ").append(embeddedField.getClassName())
                    .append(" ").append(embeddedField.getParentClass()).append(";\n\n");

            generateEntity(outputFolder, embeddedField);
        }
    }

    private void generatePrePersist(final JpaEntity entityDef, final StringBuilder entityCode) {
        final StringBuilder prePersistCode = new StringBuilder();

        for (final JpaColumn jpaColumn : entityDef.getColumns()) {
            if (StringUtils.isNotBlank(jpaColumn.getDefaultValue())) {
                if (prePersistCode.isEmpty()) {
                    prePersistCode.append("    @PrePersist\n");
                    prePersistCode.append("    protected void onPrePersist() {\n");
                }

                final String columnType = Utils.mapHibernateTypeToJava(jpaColumn.getType());
                prePersistCode.append("        if (this.").append(jpaColumn.getName()).append(" == null) {\n");
                prePersistCode.append("            this.").append(jpaColumn.getName()).append(" = ");
                if ("Boolean".equals(columnType)) {
                    prePersistCode.append("BooleanUtils.toBooleanObject(\"");
                } else {
                    prePersistCode.append(columnType).append(".valueOf(\"");
                }
                prePersistCode.append(jpaColumn.getDefaultValue().replace("'", "")).append("\");\n");
                prePersistCode.append("        }\n");
            }
        }

        if (!prePersistCode.isEmpty()) {
            prePersistCode.append("    }\n");
            entityCode.append(prePersistCode);
        }
    }

    private void generateQueries(final JpaEntity jpaEntity, final StringBuilder entityCode) {
        if (!jpaEntity.getNamedQueries().stream().filter(jpaNamedQuery -> !jpaNamedQuery.isNativeQuery()).toList().isEmpty()) {
            entityCode.append("\n@NamedQueries({\n");
            for (final JpaNamedQuery namedQuery : jpaEntity.getNamedQueries()) {
                if (!namedQuery.isNativeQuery()) {
                    entityCode.append("    @NamedQuery(name = \"").append(namedQuery.getName()).append("\",\n");
                    entityCode.append("        query = \"\"\"").append(namedQuery.getQuery().trim()).append(
                            "\"\"\"),\n\n");
                }
            }
            entityCode.append("})\n");
        }
    }

    private void generateNativeQueries(final JpaEntity jpaEntity, final StringBuilder entityCode) {
        if (!jpaEntity.getNamedQueries().stream().filter(JpaNamedQuery::isNativeQuery).toList().isEmpty()) {
            entityCode.append("\n@NamedNativeQueries({\n");
            for (final JpaNamedQuery namedQuery : jpaEntity.getNamedQueries()) {
                if (namedQuery.isNativeQuery()) {
                    entityCode.append("    @NamedNativeQuery(name = \"").append(namedQuery.getName()).append("\",\n");
                    entityCode.append("        query = \"\"\"").append(namedQuery.getQuery().trim()).append(
                            "\"\"\",\n");
                    entityCode.append("        resultSetMapping = \"").append(namedQuery.getName()).append("\"),\n\n");
                }
            }
            entityCode.append("})\n");

            if (!jpaEntity.getNamedQueries().stream().filter(JpaNamedQuery::isNativeQuery).toList().isEmpty()) {
                entityCode.append("@SqlResultSetMappings({\n");
                for (final JpaNamedQuery namedQuery : jpaEntity.getNamedQueries()) {
                    if (namedQuery.isNativeQuery()) {
                        entityCode.append("    @SqlResultSetMapping(name = \"").append(namedQuery.getName()).append(
                                "\",\n");
                        entityCode.append("        columns = {\n");

                        for (final JpaColumn column : namedQuery.getReturnColumns()) {
                            entityCode.append("            @ColumnResult(name = \"").append(column.getColumnName())
                                    .append(
                                            "\",");
                            entityCode.append(" type = ").append(Utils.mapHibernateTypeToJava(column.getType())).append(
                                    ".class),\n");
                        }
                        entityCode.append("        }),\n");
                    }
                }
                entityCode.append("})\n");
            }
        }
    }
}
