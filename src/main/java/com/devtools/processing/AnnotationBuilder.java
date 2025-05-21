package com.devtools.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.devtools.utils.Utils;
import com.devtools.model.jpa.JpaColumn;
import com.devtools.model.jpa.JpaEntity;
import com.devtools.model.jpa.JpaNamedQuery;
import com.devtools.model.jpa.JpaPrimaryKey;
import com.devtools.model.jpa.JpaRelationship;
import com.devtools.model.hbm.Tags;
import com.devtools.utils.HibernateUtils;

public class AnnotationBuilder {

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
        if (jpaEntity.isAbstractClass() && StringUtils.isNotBlank(jpaEntity.getParentClass())) {
            jpaEntity.addAnnotation("@javax.persistence.MappedSuperclass");
        }

        if (jpaEntity.isEmbeddable()) {
            jpaEntity.addAnnotation("@javax.persistence.Embeddable");
        } else {
            jpaEntity.addAnnotation("@javax.persistence.Entity");
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
                    tableAnnotation.append(entry.getValue()).append("})\n");
                }
                tableAnnotation.append("    }\n");
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
                    jpaEntity.addAnnotation("@javax.persistence.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_ONLY)");
                    break;
                case "read-write":
                    jpaEntity.addAnnotation("@javax.persistence.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)");
                    break;
                case "nonstrict-read-write":
                    jpaEntity.addAnnotation("@javax.persistence.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)");
                    break;
                case "transactional":
                    jpaEntity.addAnnotation("@javax.persistence.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.TRANSACTIONAL)");
                    break;
                default:
                    break;
            }
        }

        if (jpaEntity.isImmutable()) {
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

        if (StringUtils.isBlank(jpaEntity.getParentClass())) {
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
                if (jpaEntity.getDiscriminator().getLength() != 31) {
                    discriminatorAnnotation.append(", length = ").append(jpaEntity.getDiscriminator().getLength());
                }
                discriminatorAnnotation.append(")");
                jpaEntity.addAnnotation(discriminatorAnnotation.toString());
            }
        }
        if (jpaEntity.getInheritance() != null) {
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
                final StringBuilder sequenceAnnotation = new StringBuilder();
                String generatorAnnotation = null;
                sequenceAnnotation.append("@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.SEQUENCE");
                if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getGeneratorName())) {
                    final String generatorName = entityDef.getPrimaryKey().getGeneratorName();
                    final Integer initialValue = entityDef.getPrimaryKey().getInitialValue();
                    final Integer allocationSize = entityDef.getPrimaryKey().getAllocationSize();

                    sequenceAnnotation.append(", generator = \"gen").append(entityDef.getClassName()).append("\"");
                    // Add the @SequenceGenerator
                    sequenceAnnotation.append(")");

                    generatorAnnotation = "@javax.persistence.SequenceGenerator(name = \"gen" + entityDef.getClassName()
                            + "\", sequenceName = \"" + generatorName + "\""
                            + (initialValue != null ? ", allocationSize = " + allocationSize
                                + ", initialValue = " + initialValue : "")
                            + ")";
                } else {
                    sequenceAnnotation.append(")");
                }
                jpaPrimaryKey.addAnnotation(sequenceAnnotation.toString());
                if (generatorAnnotation != null) {
                    jpaPrimaryKey.addAnnotation(generatorAnnotation);
                }
                break;
            case "IDENTITY":
                jpaPrimaryKey.addAnnotation("@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)");
                break;
            case "TABLE":
                jpaPrimaryKey.addAnnotation("@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.TABLE)");
                break;
            case "FOREIGN":
                break; // it will be defined in the OneToOne
            case "GENERATOR":
                if (StringUtils.isNotBlank(entityDef.getPrimaryKey().getGeneratorName())) {
                    jpaPrimaryKey.addAnnotation("@javax.persistence.GeneratedValue(generator = \"" +
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

    private void buildColumns(final JpaEntity entityDef) {
        for (final JpaColumn col : entityDef.getColumns()) {
            if (col.isComposite()) {
                continue;
            }
            if (col.isVersion()) {
                col.addAnnotation("@javax.persistence.Version");
            }
            if (col.getNaturalId() != JpaColumn.NaturalId.NONE) {
                col.addAnnotation("@org.hibernate.annotations.NaturalId" +
                        (col.getNaturalId() == JpaColumn.NaturalId.MUTABLE ? "(mutable = true)" : ""));
            }
            if (col.getType() != null && col.getType(false).endsWith("Type")) {
                final StringBuilder typeAnnotation = new StringBuilder();
                typeAnnotation.append("@org.hibernate.annotations.Type(type = \"").append(col.getType(false)).append("\"");
                if (!col.getTypeParams().isEmpty()) {
                    typeAnnotation.append(",\n        parameters = {\n");
                    for (final Map.Entry<String, String> entry : col.getTypeParams().entrySet()) {
                        typeAnnotation.append("            @org.hibernate.annotations.Parameter(name = \"").append(entry.getKey());
                        typeAnnotation.append("\", value = \"").append(entry.getValue()).append("\"),\n");
                    }
                    typeAnnotation.append("        }\n    ");
                }
                typeAnnotation.append(")");
                col.addAnnotation(typeAnnotation.toString());
            }
            if (col.isLazy()) {
                col.addAnnotation("@javax.persistence.Basic(fetch = javax.persistence.FetchType.LAZY)");
            }

            col.addAnnotation(buildColumn(entityDef, col));

            if (!col.isOptimisticLock()) {
                col.addAnnotation("@org.hibernate.annotations.OptimisticLock(excluded = true)");
            }
        }
    }

    private String buildColumn(final JpaEntity entityDef, final JpaColumn col) {
        final StringBuilder columnAnnotation = new StringBuilder();
        columnAnnotation.append("@javax.persistence.Column(");
        if (entityDef.isSecondTable()) {
            columnAnnotation.append("table = \"").append(entityDef.getTable()).append("\", ");
        }
        columnAnnotation.append("name = \"").append(col.getColumnName()).append("\"");

        if (col.getLength() != null && col.getLength() != 255) {
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
            final StringBuilder columnAnnotation = new StringBuilder();
            columnAnnotation.append("@javax.persistence.AttributeOverrides({\n");
            JpaColumn varColumn = null;
            for (final JpaColumn column : entry.getValue()) {
                columnAnnotation.append("        @javax.persistence.AttributeOverride(name = \"");
                columnAnnotation.append(Utils.toCamelCase(column.getColumnName()));
                columnAnnotation.append("\", column = ");
                columnAnnotation.append(buildColumn(entityDef, column));
                columnAnnotation.append("),\n");
                varColumn = column;
            }
            columnAnnotation.append("    })");
            assert varColumn != null;
            varColumn.addAnnotation(columnAnnotation.toString());
        }
    }

    private void buildRelationships(final JpaEntity entityDef) {
        for (final JpaRelationship relationship : entityDef.getRelationships()) {
            if (Tags.TAG_MAP.equals(relationship.getCollectionType()) && relationship.getReferencedColumns() != null) {
                if (relationship.getReferencedColumns().size() > 1) {
                    relationship.addAnnotation("@javax.persistence.MapKeyClass(" +
                            relationship.getReferencedColumns().get(0).getType() + ".class)");
                    relationship.addAnnotation("@javax.persistence.MapKeyEmbedded");
                } else {
                    relationship.addAnnotation("@javax.persistence.MapKeyColumn(name = \"" +
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

            String joinColumn = "";
            if (relationship.getReferencedColumns() != null && !relationship.getReferencedColumns().isEmpty()) {
                final JpaColumn referencedColumn = relationship.getReferencedColumns().get(0);
                joinColumn = "@javax.persistence.JoinColumn(name = \"" + referencedColumn.getColumnName() + "\"" +
                        (!referencedColumn.isUpdatable() ? ", updatable = false" : "") +
                        (!referencedColumn.isNullable() ? ", nullable = false" : "") +
                        (referencedColumn.isUnique() ? ", unique = true" : "") +
                        (StringUtils.isNotBlank(referencedColumn.getForeignKey()) ?
                                ", foreignKey = @javax.persistence.ForeignKey(name = \"" +
                                        referencedColumn.getForeignKey() + "\")" : "") +
                        ")";
            }

            final StringBuilder relationshipAnnotation = new StringBuilder();

            // Generate the appropriate relationship annotation
            switch (relationship.getType()) {
                case ManyToOne:
                    if (StringUtils.isNotBlank(relationship.getAccess())) {
                        relationship.addAnnotation("@javax.persistence.Access(javax.persistence.AccessType." +
                                relationship.getAccess().toUpperCase() + ")");
                    }
                    relationshipAnnotation.append("@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.").append(fetchType).append(", ");
                    if (!cascade.isEmpty()) {
                        relationshipAnnotation.append(cascade);
                    }
                    relationshipAnnotation.deleteCharAt(relationshipAnnotation.length()-2); // remove last comma
                    relationshipAnnotation.deleteCharAt(relationshipAnnotation.length()-1); // remove last space
                    relationshipAnnotation.append(")");
                    relationship.addAnnotation(relationshipAnnotation.toString());

                    if (!joinColumn.isEmpty()) {
                        relationship.addAnnotation(joinColumn);
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
                            mappedBy = Utils.lowercaseUntilLastUpper(entityDef.getClassName());
                        }
                        relationshipAnnotation.append("mappedBy = \"").append(mappedBy).append("\", ");
                    }
                    relationshipAnnotation.deleteCharAt(relationshipAnnotation.length()-2); // remove last comma
                    relationshipAnnotation.deleteCharAt(relationshipAnnotation.length()-1); // remove last space
                    relationshipAnnotation.append(")");
                    relationship.addAnnotation(relationshipAnnotation.toString());

                    if (!joinColumn.isEmpty()) {
                        relationship.addAnnotation(joinColumn);
                    }
                    break;

                case OneToOne:
                    if (entityDef.getPrimaryKey() != null &&
                            "FOREIGN".equals(entityDef.getPrimaryKey().getGeneratorType()) &&
                            relationship.getName().equals(entityDef.getPrimaryKey().getGeneratorName())) {
                        relationship.addAnnotation("@javax.persistence.MapsId");
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
                    relationshipAnnotation.deleteCharAt(relationshipAnnotation.length()-2); // remove last comma
                    relationshipAnnotation.deleteCharAt(relationshipAnnotation.length()-1); // remove last space
                    relationshipAnnotation.append(")");
                    relationship.addAnnotation(relationshipAnnotation.toString());

                    if (!joinColumn.isEmpty()) {
                        relationship.addAnnotation(joinColumn);
                    }
                    break;

                case ManyToMany:
                    relationshipAnnotation.append("@javax.persistence.ManyToMany(fetch = javax.persistence.FetchType.").append(fetchType).append(", ");
                    relationshipAnnotation.append(StringUtils.isNotBlank(cascade) ? cascade : "");
                    relationshipAnnotation.deleteCharAt(relationshipAnnotation.length()-2); // remove last comma
                    relationshipAnnotation.deleteCharAt(relationshipAnnotation.length()-1); // remove last space
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

            if (StringUtils.isNotBlank(relationship.getOrderColumn())) {
                relationship.addAnnotation("@javax.persistence.OrderBy(\"" + relationship.getOrderColumn() + "\")");
            }
        }
    }

    private void buildEmbedded(final JpaEntity entityDef) {
        for (final JpaEntity embeddedField : entityDef.getEmbeddedFields()) {
            embeddedField.addAnnotation("@javax.persistence.Embedded");

            buildEntity(embeddedField);
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
