package com.devtools.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.devtools.model.hbm.Tags;
import com.devtools.model.jpa.JpaColumn;
import com.devtools.model.jpa.JpaCompositeColumn;
import com.devtools.model.jpa.JpaDiscriminator;
import com.devtools.model.jpa.JpaEntity;
import com.devtools.model.jpa.JpaNamedQuery;
import com.devtools.model.jpa.JpaPrimaryKey;
import com.devtools.model.jpa.JpaRelationship;
import com.devtools.utils.HibernateUtils;
import com.devtools.utils.JavaParserUtils;
import com.devtools.utils.Utils;

public class AnnotationBuilder {

    private static final Log LOG = LogFactory.getLog(AnnotationBuilder.class);

    private static final String PREFIX_GENERATOR = "generator";

    private final String outputFolder;

    public AnnotationBuilder(final String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void build(final JpaEntity entityDef) {

        buildQueries(entityDef);

        buildNativeQueries(entityDef);

        buildEntity(entityDef);

        if (entityDef.getPrimaryKey() != null) {
            // Generate the @Id and @GeneratedValue annotations
            buildPrimaryKey(entityDef);
        }

        // Add properties with @Column annotations
        buildColumns(entityDef);

        buildAttributeOverrides(entityDef);

        // Handle relationships (ManyToOne, OneToMany, OneToOne, ManyToMany)
        buildRelationships(entityDef);

        // Handle Embedded fields and Embeddable classes
        buildEmbedded(entityDef);
    }

    private void buildEntity(final JpaEntity jpaEntity) {

        if (jpaEntity.isEmbeddable()) {
            jpaEntity.addAnnotation("@javax.persistence.Embeddable");
        } else {
            if (StringUtils.isBlank(jpaEntity.getTable()) && jpaEntity.getDiscriminator() == null) {
                jpaEntity.addAnnotation("@javax.persistence.MappedSuperclass");
            } else {
                jpaEntity.addAnnotation("@javax.persistence.Entity");
            }
        }

        if (StringUtils.isNotBlank(jpaEntity.getTable())) {
            final StringBuilder indexes = buildIndexes(jpaEntity);
            final Map<String, StringBuilder> uniqueConstraints = buildUniqueConstraints(jpaEntity);

            final StringBuilder tableAnnotation = new StringBuilder();
            if (jpaEntity.isSecondTable()) {
                tableAnnotation.append("@javax.persistence.SecondaryTable(name = \"");
            } else {
                tableAnnotation.append("@javax.persistence.Table(name = \"");
            }
            tableAnnotation.append(jpaEntity.getTable()).append("\"");

            if (!indexes.isEmpty()) {
                tableAnnotation.append(",\n    indexes = {\n").append(indexes).append("\n    }");
            }
            if (!uniqueConstraints.isEmpty()) {
                tableAnnotation.append(",\n    uniqueConstraints = {\n");
                for (final Map.Entry<String, StringBuilder> entry : uniqueConstraints.entrySet()) {
                    tableAnnotation.append("        @javax.persistence.UniqueConstraint(name = \"");
                    tableAnnotation.append(entry.getKey()).append("\", columnNames = {");
                    tableAnnotation.append(entry.getValue()).append("}),\n");
                }
                tableAnnotation.append("    }\n");
            }
            if (jpaEntity.isSecondTable() && jpaEntity.getSecondTableKeys() != null) {
                tableAnnotation.append(",\n    pkJoinColumns = @javax.persistence.PrimaryKeyJoinColumn(name = \"")
                        .append(jpaEntity.getSecondTableKeys().getColumnName()).append("\")");
                tableAnnotation.append(",\n    foreignKey = @javax.persistence.ForeignKey(name = \"")
                        .append(jpaEntity.getSecondTableKeys().getForeignKey()).append("\")\n");
            }
            tableAnnotation.append(")");
            jpaEntity.addAnnotation(tableAnnotation.toString());
        }

        if (jpaEntity.isSecondTable()) {
            jpaEntity.addAnnotation("@org.hibernate.annotations.Table(appliesTo = \"" +
                    jpaEntity.getTable() + "\", optional = false)");
        }

        if (StringUtils.isNotBlank(jpaEntity.getCacheUsage())) {
            jpaEntity.addAnnotation("@javax.persistence.Cacheable");
            switch (jpaEntity.getCacheUsage()) {
                case "read-only":
                    jpaEntity.addAnnotation("@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_ONLY)");
                    break;
                case "read-write":
                    jpaEntity.addAnnotation("@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)");
                    break;
                case "nonstrict-read-write":
                    jpaEntity.addAnnotation("@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)");
                    break;
                case "transactional":
                    jpaEntity.addAnnotation("@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.TRANSACTIONAL)");
                    break;
                default:
                    break;
            }
        }

        if (!jpaEntity.isMutable()) {
            jpaEntity.addAnnotation("@org.hibernate.annotations.Immutable");
        }

        if (jpaEntity.isDynamicInsert()) {
            jpaEntity.addAnnotation("@org.hibernate.annotations.DynamicInsert");
        }

        if (jpaEntity.isDynamicUpdate()) {
            jpaEntity.addAnnotation("@org.hibernate.annotations.DynamicUpdate");
        }

        if (jpaEntity.getDiscriminator() != null && StringUtils.isNotBlank(jpaEntity.getDiscriminator().getValue())) {
            jpaEntity.addAnnotation("@javax.persistence.DiscriminatorValue(\"" + jpaEntity.getDiscriminator().getValue() + "\")");
        }

        if (StringUtils.isBlank(jpaEntity.getSimpleParentClass())) {
            // Handle Discriminator Column
            if (jpaEntity.getDiscriminator() != null && jpaEntity.getDiscriminator().getColumn() != null) {
                final StringBuilder discriminatorAnnotation = new StringBuilder();
                discriminatorAnnotation.append("@javax.persistence.DiscriminatorColumn(name = \"");
                discriminatorAnnotation.append(jpaEntity.getDiscriminator().getColumn()).append("\"");
                if (jpaEntity.getDiscriminator().getType() != null &&
                        !"string".equals(jpaEntity.getDiscriminator().getType())) {
                    discriminatorAnnotation.append(", type = ").append(
                            HibernateUtils.getDiscriminatorType(jpaEntity.getDiscriminator().getType()));
                }
                if (jpaEntity.getDiscriminator().getLength() != JpaDiscriminator.DEFAULT_DISCRIMINATOR_LENGTH) {
                    discriminatorAnnotation.append(", length = ").append(jpaEntity.getDiscriminator().getLength());
                }
                discriminatorAnnotation.append(")");
                jpaEntity.addAnnotation(discriminatorAnnotation.toString());
            }
        }
        if (jpaEntity.getInheritance() != null && !"SINGLE_TABLE".equalsIgnoreCase(jpaEntity.getInheritance())) {
            jpaEntity.addAnnotation("@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType."
                    + jpaEntity.getInheritance() + ")");
        }
    }

    private StringBuilder buildIndexes(final JpaEntity entityDef) {
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
            indexes.append("        @javax.persistence.Index(name = \"").append(entry.getKey()).append("\", columnList = \"")
                    .append(String.join(",", entry.getValue())).append("\"),\n");
        }
        return indexes;
    }

    private Map<String, StringBuilder> buildUniqueConstraints(final JpaEntity entityDef) {
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

    private void buildPrimaryKey(final JpaEntity entityDef) {
        final JpaPrimaryKey jpaPrimaryKey = entityDef.getPrimaryKey();

        if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getGeneratorType())) {
            jpaPrimaryKey.addAnnotation("@javax.persistence.Id");

            switch (entityDef.getPrimaryKey().getGeneratorType()) {
            case "SEQUENCE":
                buildSequenceGenerator(entityDef);
                break;
            case "SEQHILO":
                buildSeqHiloGenerator(entityDef);
                break;
            case "IDENTITY":
                jpaPrimaryKey.addAnnotation("@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)");
                break;
            case "FOREIGN":
                buildForeignGenerator(entityDef);
                break; // it will be defined in the OneToOne
            case "GENERATOR":
                if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getGeneratorName())) {
                    jpaPrimaryKey.addAnnotation(
                            "@javax.persistence.GeneratedValue(generator = \"" + PREFIX_GENERATOR +
                                    entityDef.getPrimaryKey().getGeneratorName() + "\")");
                }
                break;
            default:
                jpaPrimaryKey.addAnnotation("@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.AUTO)");
                break;
            }
        }

        jpaPrimaryKey.addAnnotation("@javax.persistence.Column(name = \"" + jpaPrimaryKey.getColumnName() + "\")");
    }

    private static void buildSequenceGenerator(final JpaEntity entityDef) {
        final JpaPrimaryKey jpaPrimaryKey = entityDef.getPrimaryKey();
        final StringBuilder sequenceAnnotation = new StringBuilder();
        final StringBuilder generatorAnnotation = new StringBuilder();

        sequenceAnnotation.append("@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.SEQUENCE");
        if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getGeneratorName())) {
            final String generatorName = entityDef.getPrimaryKey().getGeneratorName();
            final String initialValue = entityDef.getPrimaryKey().getInitialValue();
            final String allocationSize = entityDef.getPrimaryKey().getAllocationSize();

            sequenceAnnotation.append(", generator = \"").append(PREFIX_GENERATOR).append(entityDef.getSimpleName()).append("\"");
            // Add the @SequenceGenerator
            sequenceAnnotation.append(")");

            generatorAnnotation.append("@javax.persistence.SequenceGenerator(name = \"" + PREFIX_GENERATOR).append(entityDef.getSimpleName())
                    .append("\", sequenceName = \"").append(generatorName).append("\"")
                    .append(StringUtils.isNotBlank(allocationSize) ? ", allocationSize = " + allocationSize : "")
                    .append(StringUtils.isNotBlank(initialValue) ? ", initialValue = " + initialValue : "")
                    .append(")");
        } else {
            sequenceAnnotation.append(")");
        }
        jpaPrimaryKey.addAnnotation(sequenceAnnotation.toString());
        if (!generatorAnnotation.isEmpty()) {
            jpaPrimaryKey.addAnnotation(generatorAnnotation.toString());
        }
    }

    private static void buildSeqHiloGenerator(final JpaEntity entityDef) {
        final JpaPrimaryKey jpaPrimaryKey = entityDef.getPrimaryKey();

        final String sequenceAnnotation =
                "@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.SEQUENCE"
                + ", generator = \"" + PREFIX_GENERATOR + entityDef.getSimpleName() + "\""
                + ")";
        jpaPrimaryKey.addAnnotation(sequenceAnnotation);

        final StringBuilder generatorAnnotation = new StringBuilder();
        generatorAnnotation.append("@org.hibernate.annotations.GenericGenerator(name = \"" + PREFIX_GENERATOR)
                .append(entityDef.getSimpleName())
                .append("\",\n    strategy = \"org.hibernate.id.enhanced.SequenceStyleGenerator\"")
                .append(",\n    parameters = {\n");
        if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getGeneratorName())) {
            generatorAnnotation.append("        @org.hibernate.annotations.Parameter(name = \"sequence_name\"");
            generatorAnnotation.append(", value = \"").append(entityDef.getPrimaryKey().getGeneratorName());
            generatorAnnotation.append("\"),\n");
        }
        if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getInitialValue())) {
            generatorAnnotation.append("        @org.hibernate.annotations.Parameter(name = \"initial_value\"");
            generatorAnnotation.append(", value = \"").append(entityDef.getPrimaryKey().getInitialValue());
            generatorAnnotation.append("\"),\n");
        }
        if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getIncrementSize())) {
            generatorAnnotation.append("        @org.hibernate.annotations.Parameter(name = \"increment_size\"");
            generatorAnnotation.append(", value = \"").append(entityDef.getPrimaryKey().getIncrementSize());
            generatorAnnotation.append("\"),\n");
        }
        generatorAnnotation.append("        @org.hibernate.annotations.Parameter(name = \"optimizer\"");
        generatorAnnotation.append(", value = \"hilo\")\n");
        generatorAnnotation.append("    }\n)");
        jpaPrimaryKey.addAnnotation(generatorAnnotation.toString());
    }

    private static void buildForeignGenerator(final JpaEntity entityDef) {
        final JpaPrimaryKey jpaPrimaryKey = entityDef.getPrimaryKey();

        final String generatorAnnotation = "@org.hibernate.annotations.GenericGenerator(name = \""
                                           + PREFIX_GENERATOR + entityDef.getSimpleName() + "\""
                                           + ",\n    strategy = \"foreign\""
                                           + ",\n    parameters = "
                                           + "@org.hibernate.annotations.Parameter(name = \"property\""
                                           + ", value = \"" + jpaPrimaryKey.getProperty() + "\")\n"
                                           + ")";
        jpaPrimaryKey.addAnnotation(generatorAnnotation);
    }

    private void buildColumns(final JpaEntity entityDef) {
        for (final JpaColumn col : entityDef.getColumns()) {
            if (col.isEmbedded()) {
                col.addAnnotation("@javax.persistence.Embedded");
                continue;
            }

            if (col.isVersion()) {
                col.addAnnotation("@javax.persistence.Version");
            }

            if (col.getNaturalId() != JpaColumn.NaturalId.NONE) {
                col.addAnnotation("@org.hibernate.annotations.NaturalId" +
                        (col.getNaturalId() == JpaColumn.NaturalId.MUTABLE ? "(mutable = true)" : ""));
            }

            if (col.isLazy()) {
                col.addAnnotation("@javax.persistence.Basic(fetch = javax.persistence.FetchType.LAZY)");
            }

            col.addAnnotation(buildColumn(col));

            if (!col.isOptimisticLock()) {
                col.addAnnotation("@org.hibernate.annotations.OptimisticLock(excluded = true)");
            }
        }
    }

    private String buildColumn(final JpaColumn col) {
        final StringBuilder columnAnnotation = new StringBuilder();
        columnAnnotation.append("@javax.persistence.Column(");
        columnAnnotation.append("name = \"").append(col.getColumnName()).append("\"");

        if (col.getLength() != null && col.getLength() != JpaColumn.DEFAULT_COLUMN_LENGTH) {
            columnAnnotation.append(", length = ").append(col.getLength());
        }
        if (!col.isNullable()) {
            columnAnnotation.append(", nullable = false");
        }
        if (!col.isUpdatable()) {
            columnAnnotation.append(", updatable = false");
        }
        if (col.isUnique()) {
            columnAnnotation.append(", unique = true");
        }
        if (StringUtils.isNotBlank(col.getColumnDefinition())) {
            columnAnnotation.append(", columnDefinition = \"").append(col.getColumnDefinition()).append("\"");
        }
        if (col.getPrecision() != null && col.getPrecision() != 0) {
            columnAnnotation.append(", precision = \"").append(col.getPrecision()).append("\"");
        }
        if (col.getScale() != null && col.getScale() != 0) {
            columnAnnotation.append(", scale = \"").append(col.getScale()).append("\"");
        }

        columnAnnotation.append(")");
        return columnAnnotation.toString();
    }

    private void buildAttributeOverrides(final JpaEntity entityDef) {
        for (final JpaCompositeColumn compositeColumn : entityDef.getCompositeColumns()) {
            final StringBuilder compositeAnnotation = new StringBuilder();
            compositeAnnotation.append("@javax.persistence.AttributeOverrides({\n");

            for (final JpaColumn column : compositeColumn.getColumns()) {
                compositeAnnotation.append("        @javax.persistence.AttributeOverride(name = \"");
                compositeAnnotation.append(Utils.toCamelCase(column.getColumnName()));
                compositeAnnotation.append("\", column = ");
                compositeAnnotation.append(buildColumn(column));
                compositeAnnotation.append("),\n");
            }
            compositeAnnotation.append("    })");

            if (!compositeColumn.isOptimisticLock()) {
                compositeColumn.addAnnotation("@org.hibernate.annotations.OptimisticLock(excluded = true)");
            }
            compositeColumn.addAnnotation(compositeAnnotation.toString());
        }
    }

    private void buildRelationships(final JpaEntity entityDef) {
        for (final JpaRelationship relationship : entityDef.getRelationships()) {
            if (Tags.TAG_MAP.equals(relationship.getCollectionType()) && !relationship.getReferencedColumns().isEmpty()) {
                if (StringUtils.isNotBlank(relationship.getCompositeMapKey())) {
                    relationship.addAnnotation("@javax.persistence.MapKeyClass(" +
                                               relationship.getCompositeMapKey() + ".class)");
                } else {
                    relationship.addAnnotation("@javax.persistence.MapKey(name = \"" +
                            relationship.getReferencedColumns().get(0).getName() + "\")");
                }
            }

            // Fetch Type (Default to LAZY if not specified)
            final String fetchType = ("eager".equals(relationship.getFetch()) || "join".equals(relationship.getFetch())) ?
                    "EAGER" : "LAZY";

            // Cascade Types
            final String cascadeTypes = HibernateUtils.convertCascadeTypes(relationship.getCascade());
            final StringBuilder cascade = new StringBuilder();
            if (!cascadeTypes.isEmpty()) {
                cascade.append("cascade = {").append(cascadeTypes).append("}, ");
                if (relationship.getCascade().contains("delete-orphan")) {
                    cascade.append("orphanRemoval = true, ");
                }
            }

            final StringBuilder joinColumn = new StringBuilder();
            if (relationship.getReferencedColumns() != null && !relationship.getReferencedColumns().isEmpty()) {
                final JpaColumn referencedColumn = relationship.getReferencedColumns().get(0);
                joinColumn.append("@javax.persistence.JoinColumn(");
                if (StringUtils.isBlank(entityDef.getTable()) &&
                    StringUtils.isNotBlank(entityDef.getParentTable())) {
                    joinColumn.append("table = \"").append(entityDef.getParentTable()).append("\", ");
                }
                joinColumn.append("name = \"").append(referencedColumn.getColumnName()).append("\"")
                        .append(!referencedColumn.isUpdatable() ? ", updatable = false" : "")
                        .append(!referencedColumn.isNullable() ? ", nullable = false" : "")
                        .append(referencedColumn.isUnique() ? ", unique = true" : "")
                        .append(StringUtils.isNotBlank(referencedColumn.getForeignKey()) ?
                                ", foreignKey = @javax.persistence.ForeignKey(name = \"" +
                                referencedColumn.getForeignKey() + "\")" : "")
                        .append(")");
            }

            final StringBuilder relationshipAnnotation = new StringBuilder();

            // Generate the appropriate relationship annotation
            switch (relationship.getRelationshipType()) {
                case ManyToOne:
                    if (StringUtils.isNotBlank(relationship.getAccess())) {
                        relationship.addAnnotation("@javax.persistence.Access(javax.persistence.AccessType." +
                                relationship.getAccess().toUpperCase() + ")");
                    }
                    relationshipAnnotation.append("@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.").append(fetchType).append(", ");
                    if (!cascade.isEmpty()) {
                        relationshipAnnotation.append(cascade);
                    }
                    if (relationshipAnnotation.charAt(relationshipAnnotation.length()-2) == ',') {
                        relationshipAnnotation.deleteCharAt(relationshipAnnotation.length() - 2); // remove last comma
                        relationshipAnnotation.deleteCharAt(relationshipAnnotation.length() - 1); // remove last space
                    }
                    relationshipAnnotation.append(")");
                    relationship.addAnnotation(relationshipAnnotation.toString());

                    if (!joinColumn.isEmpty()) {
                        relationship.addAnnotation(joinColumn.toString());
                    }
                    break;

                case OneToMany:
                    relationshipAnnotation.append("@javax.persistence.OneToMany(");
                    // LAZY fetch is default for OneToMany
                    if (!"LAZY".equals(fetchType)) {
                        relationshipAnnotation.append("fetch = javax.persistence.FetchType.").append(fetchType).append(", ");
                    }
                    if (!cascade.isEmpty()) {
                        relationshipAnnotation.append(cascade);
                    }
                    if (relationship.isInverse()) {
                        String mappedBy = relationship.getMappedBy();
                        if (StringUtils.isBlank(mappedBy)) {
                            mappedBy = JavaParserUtils.findVariableNameByType(outputFolder,
                                    relationship.getReturnType(), entityDef.getSimpleName());
                            if (StringUtils.isBlank(mappedBy)) {
                                mappedBy = Utils.toCamelCase(entityDef.getSimpleName());
                                LOG.warn(String.format(
                                        "Please check the correct name of (mappedBy = \"%s\") in the field '%s' of %s",
                                        mappedBy, relationship.getName(), entityDef.getSimpleName()));
                            }
                        }
                        relationshipAnnotation.append("mappedBy = \"").append(mappedBy).append("\", ");
                    }
                    if (relationshipAnnotation.charAt(relationshipAnnotation.length()-2) == ',') {
                        relationshipAnnotation.deleteCharAt(relationshipAnnotation.length() - 2); // remove last comma
                        relationshipAnnotation.deleteCharAt(relationshipAnnotation.length() - 1); // remove last space
                    }
                    relationshipAnnotation.append(")");
                    relationship.addAnnotation(relationshipAnnotation.toString());
                    break;

                case OneToOne:
                    if (entityDef.getPrimaryKey() != null &&
                            "FOREIGN".equals(entityDef.getPrimaryKey().getGeneratorType()) &&
                            relationship.getName().equals(entityDef.getPrimaryKey().getProperty())) {
                        relationship.addAnnotation("@javax.persistence.MapsId");
                        relationship.addAnnotation("@javax.persistence.JoinColumn(name = \"" +
                                                   entityDef.getPrimaryKey().getColumnName() + "\")");
                    }
                    relationshipAnnotation.append("@javax.persistence.OneToOne(");
                    if ("LAZY".equals(fetchType)) {
                        relationshipAnnotation.append("fetch = javax.persistence.FetchType.").append(fetchType).append(", ");
                    }
                    if (!relationship.isOptional()) {
                        relationshipAnnotation.append("optional = false, ");
                    }
                    if (StringUtils.isNotBlank(relationship.getMappedBy())) {
                        relationshipAnnotation.append("mappedBy = \"").append(relationship.getMappedBy()).append("\", ");
                    }
                    if (!cascade.isEmpty()) {
                        relationshipAnnotation.append(cascade);
                    }
                    if (relationshipAnnotation.charAt(relationshipAnnotation.length()-2) == ',') {
                        relationshipAnnotation.deleteCharAt(relationshipAnnotation.length() - 2); // remove last comma
                        relationshipAnnotation.deleteCharAt(relationshipAnnotation.length() - 1); // remove last space
                    }
                    relationshipAnnotation.append(")");
                    relationship.addAnnotation(relationshipAnnotation.toString());

                    if (!joinColumn.isEmpty()) {
                        relationship.addAnnotation(joinColumn.toString());
                    }
                    break;

                case ManyToMany:
                    relationshipAnnotation.append("@javax.persistence.ManyToMany(fetch = javax.persistence.FetchType.").append(fetchType).append(", ");
                    relationshipAnnotation.append(StringUtils.isNotBlank(cascade) ? cascade : "");
                    if (relationshipAnnotation.charAt(relationshipAnnotation.length()-2) == ',') {
                        relationshipAnnotation.deleteCharAt(relationshipAnnotation.length() - 2); // remove last comma
                        relationshipAnnotation.deleteCharAt(relationshipAnnotation.length() - 1); // remove last space
                    }
                    relationshipAnnotation.append(")");
                    relationship.addAnnotation(relationshipAnnotation.toString());

                    if (relationship.getReferencedColumns() != null && !relationship.getReferencedColumns().isEmpty()) {
                        final StringBuilder joinAnnotation = new StringBuilder();
                        joinAnnotation.append("@javax.persistence.JoinTable(\n");
                        joinAnnotation.append("        name = \"").append(relationship.getTable()).append("\",\n");

                        if (relationship.getReferencedColumns().stream().anyMatch(jpaColumn -> !jpaColumn.isInverseJoin())) {
                            joinAnnotation.append("        joinColumns = {\n");
                            for (final JpaColumn column : relationship.getReferencedColumns()) {
                                if (!column.isInverseJoin()) {
                                    joinAnnotation.append("            @javax.persistence.JoinColumn(name = \"").append(
                                                    column.getColumnName())
                                            .append("\"");
                                    joinAnnotation.append(!column.isUpdatable() ? ", updatable = false" : "")
                                            .append(!column.isNullable() ? ", nullable = false" : "")
                                            .append(StringUtils.isNotBlank(column.getForeignKey()) ?
                                                    ", foreignKey = @javax.persistence.ForeignKey(name = \"" + column.getForeignKey()
                                                            + "\")" : "");
                                    joinAnnotation.append(")\n");
                                }
                            }
                            joinAnnotation.append("        },\n");
                        }

                        if (relationship.getReferencedColumns().stream().anyMatch(JpaColumn::isInverseJoin)) {
                            joinAnnotation.append("        inverseJoinColumns = {\n");
                            for (final JpaColumn column : relationship.getReferencedColumns()) {
                                if (column.isInverseJoin()) {
                                    joinAnnotation.append("            @javax.persistence.JoinColumn(name = \"").append(
                                                    column.getColumnName())
                                            .append("\"");
                                    joinAnnotation.append(!column.isUpdatable() ? ", updatable = false" : "")
                                            .append(!column.isNullable() ? ", nullable = false" : "")
                                            .append(StringUtils.isNotBlank(column.getForeignKey()) ?
                                                    ", foreignKey = @javax.persistence.ForeignKey(name = \"" + column.getForeignKey()
                                                            + "\")" : "");
                                    joinAnnotation.append(")\n");
                                }
                            }
                            joinAnnotation.append("        }\n");
                        }
                        joinAnnotation.append(")");
                        relationship.addAnnotation(joinAnnotation.toString());
                    }
                    break;
            }

            if (StringUtils.isNotBlank(relationship.getOrderBy())) {
                String orderBy = relationship.getOrderBy();
                // Try to convert DB column name to field name
                for (final JpaColumn refColumn : relationship.getReferencedColumns()) {
                    if (refColumn.getColumnName().equals(orderBy)) {
                        orderBy = refColumn.getName();
                        break;
                    }
                }
                relationship.addAnnotation("@javax.persistence.OrderBy(\"" + orderBy + "\")");
            }

            if (StringUtils.isNotBlank(relationship.getListIndex())) {
                relationship.addAnnotation("@javax.persistence.OrderColumn(name = \"" + relationship.getListIndex() + "\")");
            }
        }
    }

    private void buildEmbedded(final JpaEntity entityDef) {
        for (final JpaEntity embeddedEntity : entityDef.getEmbeddedEntities()) {
            build(embeddedEntity);
        }
    }

    private void buildQueries(final JpaEntity jpaEntity) {
        if (!jpaEntity.getNamedQueries().stream().filter(jpaNamedQuery -> !jpaNamedQuery.isNativeQuery()).toList().isEmpty()) {
            final StringBuilder queryAnnotation = new StringBuilder();
            queryAnnotation.append("@javax.persistence.NamedQueries({\n");
            for (final JpaNamedQuery namedQuery : jpaEntity.getNamedQueries()) {
                if (!namedQuery.isNativeQuery()) {
                    queryAnnotation.append("    @javax.persistence.NamedQuery(name = \"").append(namedQuery.getName()).append("\",\n");
                    queryAnnotation.append("        query = \"\"\"").append(namedQuery.getQuery().trim()).append(
                            "\"\"\"),\n\n");
                }
            }
            queryAnnotation.append("})\n");
            jpaEntity.addAnnotation(queryAnnotation.toString());
        }
    }

    private void buildNativeQueries(final JpaEntity jpaEntity) {
        if (!jpaEntity.getNamedQueries().stream().filter(JpaNamedQuery::isNativeQuery).toList().isEmpty()) {
            StringBuilder annotation = new StringBuilder();
            annotation.append("@javax.persistence.NamedNativeQueries({\n");
            for (final JpaNamedQuery namedQuery : jpaEntity.getNamedQueries()) {
                if (namedQuery.isNativeQuery()) {
                    annotation.append("    @javax.persistence.NamedNativeQuery(name = \"").append(namedQuery.getName()).append("\",\n");
                    annotation.append("        query = \"\"\"").append(namedQuery.getQuery().trim()).append(
                            "\"\"\",\n");
                    annotation.append("        resultSetMapping = \"").append(namedQuery.getName()).append("\"),\n\n");
                }
            }
            annotation.append("})\n");
            jpaEntity.addAnnotation(annotation.toString());

            if (!jpaEntity.getNamedQueries().stream().filter(JpaNamedQuery::isNativeQuery).toList().isEmpty()) {
                annotation = new StringBuilder();
                annotation.append("@javax.persistence.SqlResultSetMappings({\n");
                for (final JpaNamedQuery namedQuery : jpaEntity.getNamedQueries()) {
                    if (namedQuery.isNativeQuery()) {
                        annotation.append("    @javax.persistence.SqlResultSetMapping(name = \"").append(namedQuery.getName()).append(
                                "\",\n");
                        annotation.append("        columns = {\n");

                        for (final JpaColumn column : namedQuery.getReturnColumns()) {
                            annotation.append("            @javax.persistence.ColumnResult(name = \"").append(column.getColumnName())
                                    .append(
                                            "\",");
                            annotation.append(" type = ").append(HibernateUtils.mapHibernateTypeToJava(column.getType())).append(
                                    ".class),\n");
                        }
                        annotation.append("        }),\n");
                    }
                }
                annotation.append("})\n");
                jpaEntity.addAnnotation(annotation.toString());
            }
        }
    }
}
