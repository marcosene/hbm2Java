package com.devtools.processors;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.devtools.Utils;
import com.devtools.definition.Attributes;
import com.devtools.definition.JpaBase;
import com.devtools.definition.JpaColumn;
import com.devtools.definition.JpaEntity;
import com.devtools.definition.JpaNamedQuery;
import com.devtools.definition.JpaPrimaryKey;
import com.devtools.definition.JpaRelationship;
import com.devtools.definition.Tags;

public class HbmParser {

    private static final Log LOG = LogFactory.getLog(HbmParser.class);

    public JpaBase parse(final String filePath) throws Exception {
        final JpaBase jpaBase = new JpaBase();
        final Element root = getRootElement(filePath);
        final String defaultCascade = root.getAttribute(Attributes.ATTR_DEFAULT_CASCADE);

        parseClasses(root, jpaBase, defaultCascade);

        final JpaEntity queriesEntity = new JpaEntity();
        parseQueries(root, queriesEntity);
        if (!queriesEntity.getNamedQueries().isEmpty()) {
            final String fileName = Utils.getFileNameNoExtensions(filePath);
            queriesEntity.setClassName(fileName);
            jpaBase.addEntity(queriesEntity);
        }

        checkMissingTagAttributeImplementations(root);

        return jpaBase;
    }

    private Element getRootElement(final String filePath)
            throws ParserConfigurationException, SAXException, IOException {
        // Prepare the DocumentBuilderFactory
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setFeature("http://xml.org/sax/features/namespaces", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        // Build the Document
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final FileInputStream inputStream = new FileInputStream(filePath);
        final Document document = builder.parse(inputStream);
        return document.getDocumentElement();
    }

    private void parseClasses(final Element root, final JpaBase jpaBase, final String defaultCascade) {
        final List<Element> classElements = Utils.getChildrenByTag(root, Tags.TAG_CLASS);
        for (final Element classElement : classElements) {
            jpaBase.addEntity(parseEntity(classElement, defaultCascade));
            parseClasses(classElement, jpaBase, defaultCascade);
        }

        final List<Element> subclassElements = Utils.getChildrenByTag(root, Tags.TAG_SUBCLASS);
        for (final Element subclassElement : subclassElements) {
            jpaBase.addEntity(parseEntity(subclassElement, defaultCascade));
            parseClasses(subclassElement, jpaBase, defaultCascade);
        }

        final List<Element> unionSubclassElements = Utils.getChildrenByTag(root, Tags.TAG_UNION_SUBCLASS);
        for (final Element unionSubclassElement : unionSubclassElements) {
            jpaBase.addEntity(parseEntity(unionSubclassElement, defaultCascade));
            parseClasses(unionSubclassElement, jpaBase, defaultCascade);
        }
    }

    private JpaEntity parseEntity(final Element classElement, final String defaultCascade) {
        final JpaEntity entityDef = new JpaEntity();

        entityDef.setDefaultCascade(defaultCascade);
        entityDef.setClassName(classElement.getAttribute(Attributes.ATTR_NAME));
        entityDef.setTable(classElement.getAttribute(Attributes.ATTR_TABLE));
        entityDef.setDynamicInsert(classElement.getAttribute(Attributes.ATTR_DYNAMIC_INSERT));
        entityDef.setDynamicUpdate(classElement.getAttribute(Attributes.ATTR_DYNAMIC_UPDATE));
        entityDef.setAbstractClass(classElement.getAttribute(Attributes.ATTR_ABSTRACT));
        entityDef.setMutable(classElement.getAttribute(Attributes.ATTR_MUTABLE));

        // Only for subclasses
        entityDef.setParentClass(classElement.getAttribute(Attributes.ATTR_EXTENDS));
        entityDef.getDiscriminator(true).setValue(classElement.getAttribute(Attributes.ATTR_DISCRIMINATOR_VALUE));

        final Element cacheElement = Utils.getFirstChildByTag(classElement, Tags.TAG_CACHE);
        if (cacheElement != null) {
            entityDef.setCacheUsage(cacheElement.getAttribute(Attributes.ATTR_USAGE));
        }

        final Element joinElement = Utils.getFirstChildByTag(classElement, Tags.TAG_JOIN);
        if (joinElement != null) {
            entityDef.setTable(joinElement.getAttribute(Attributes.ATTR_TABLE));
            entityDef.setSecondTable(true);

            parseProperty(joinElement, entityDef);
        }

        parseDiscriminator(classElement, entityDef);

        parseIdAndGenerator(classElement, entityDef);

        parseNaturalIds(classElement, entityDef);

        parseVersions(classElement, entityDef);

        parseProperties(classElement, entityDef);

        parseProperty(classElement, entityDef, true);

        parseRelationships(classElement, entityDef);

        parseCollections(classElement, entityDef);

        parseEmbeddedFields(classElement, entityDef);

        return entityDef;
    }

    private void parseDiscriminator(final Element element, final JpaEntity entityDef) {
        final Element discriminatorElement = Utils.getFirstChildByTag(element, Tags.TAG_DISCRIMINATOR);
        if (discriminatorElement != null) {
            entityDef.getDiscriminator(true).setType(discriminatorElement.getAttribute(Attributes.ATTR_TYPE));

            final Element columnElement = Utils.getFirstChildByTag(discriminatorElement, Tags.TAG_COLUMN);
            if (columnElement != null) {
                entityDef.getDiscriminator(true).setColumn(columnElement.getAttribute(Attributes.ATTR_NAME));
                entityDef.getDiscriminator(true).setLength(columnElement.getAttribute(Attributes.ATTR_LENGTH));
            }
        }
    }

    private void parseIdAndGenerator(final Element element, final JpaEntity entityDef) {
        final Element idElement = Utils.getFirstChildByTag(element, Tags.TAG_ID);
        if (idElement != null) {
            final JpaPrimaryKey primaryKey = new JpaPrimaryKey();

            primaryKey.setName(idElement.getAttribute(Attributes.ATTR_NAME));
            primaryKey.setColumnName(idElement.getAttribute(Attributes.ATTR_COLUMN));
            primaryKey.setType(Utils.mapHibernateTypeToJava(idElement.getAttribute(Attributes.ATTR_TYPE)));

            final Element columnElement = Utils.getFirstChildByTag(idElement, Tags.TAG_COLUMN);
            if (columnElement != null) {
                primaryKey.setColumnName(columnElement.getAttribute(Attributes.ATTR_NAME));
            }

            // Handle the generator
            final Element generatorElement = Utils.getFirstChildByTag(idElement, Tags.TAG_GENERATOR);
            if (generatorElement != null) {
                final String generatorClass = generatorElement.getAttribute(Attributes.ATTR_CLASS);
                final Map<String, String> params = new HashMap<>();

                // Collect all params
                final List<Element> paramElements = Utils.getChildrenByTag(generatorElement, Tags.TAG_PARAM);
                for (final Element paramElement : paramElements) {
                    final String paramName = paramElement.getAttribute(Attributes.ATTR_NAME);
                    final String paramValue = paramElement.getTextContent().trim();
                    params.put(paramName, paramValue);
                }

                // Map generator to JPA annotations
                switch (generatorClass) {
                    case "sequence":
                        primaryKey.setGeneratorType("SEQUENCE");
                        primaryKey.setGeneratorName(params.get("sequence"));
                        parseGeneratorParameters(params.get("parameters"), primaryKey);
                        break;
                    case "seqhilo":
                        primaryKey.setGeneratorType("SEQUENCE");
                        primaryKey.setGeneratorName(params.get("sequence"));
                        parseGeneratorParameters(params.get("parameters"), primaryKey);
                        if (StringUtils.isNotBlank(params.get("max_lo"))) {
                            primaryKey.setAllocationSize(Integer.parseInt(params.get("max_lo")));
                        }
                        break;
                    case "increment":
                        primaryKey.setGeneratorType("TABLE");
                        break;
                    case "identity":
                        primaryKey.setGeneratorType("IDENTITY");
                        break;
                    case "foreign":
                        primaryKey.setGeneratorType("FOREIGN");
                        primaryKey.setGeneratorName(params.get("property"));
                        break;
                    default:
                        // Default to a general generator if not a common type
                        primaryKey.setGeneratorType("GENERATOR");
                        primaryKey.setGeneratorName(generatorClass);
                        break;
                }
            }

            // Add the ID attribute
            entityDef.setPrimaryKey(primaryKey);
        }
    }

    private void parseVersions(final Element element, final JpaEntity entityDef) {
        final List<Element> versionElements = Utils.getChildrenByTag(element, Tags.TAG_VERSION);
        for(final Element versionElement : versionElements) {
            final List<JpaColumn> jpaColumns = parseColumns(versionElement, null);
            for (final JpaColumn jpaColumn : jpaColumns) {
                jpaColumn.setVersion(true);
                entityDef.addColumn(jpaColumn);
            }
        }
    }

    private void parseProperties(final Element element, final JpaEntity entityDef) {
        final List<Element> propertiesElements = Utils.getChildrenByTag(element, Tags.TAG_PROPERTIES);
        for (final Element propertiesElement : propertiesElements) {
            final boolean unique = Boolean.parseBoolean(propertiesElement.getAttribute(Attributes.ATTR_UNIQUE));
            final String uniqueConstraintName = unique ? propertiesElement.getAttribute(Attributes.ATTR_NAME) : "";

            parseProperty(propertiesElement, entityDef, uniqueConstraintName);

            parseRelationships(propertiesElement, entityDef, uniqueConstraintName);
        }
    }

    private void parseProperty(final Element element, final JpaEntity entityDef) {
        parseProperty(element, entityDef, false);
    }

    private void parseProperty(final Element element, final JpaEntity entityDef, final String uniqueConstraint) {
        parseProperty(element, entityDef, uniqueConstraint, JpaColumn.NaturalId.NONE, false);
    }

    private void parseProperty(final Element element, final JpaEntity entityDef, final JpaColumn.NaturalId naturalId) {
        parseProperty(element, entityDef, null, naturalId, false);
    }

    private void parseProperty(final Element element, final JpaEntity entityDef, final boolean checkComposite) {
        parseProperty(element, entityDef, null, JpaColumn.NaturalId.NONE, checkComposite);
    }

    private void parseProperty(final Element element, final JpaEntity entityDef,
            final String uniqueConstraint, final JpaColumn.NaturalId naturalId, final boolean checkComposite) {
        final List<Element> propertyElements = Utils.getChildrenByTag(element, Tags.TAG_PROPERTY);
        for (final Element propertyElement : propertyElements) {
            final List<JpaColumn> jpaColumns = parseColumns(propertyElement, uniqueConstraint);
            if (jpaColumns.isEmpty()) {
                final JpaColumn jpaColumn = parseProperty(propertyElement, uniqueConstraint);
                jpaColumns.add(jpaColumn);
            }
            for (final JpaColumn jpaColumn : jpaColumns) {
                if (checkComposite && jpaColumns.size() > 1) {
                    jpaColumn.setComposite(true);
                }
                jpaColumn.setNaturalId(naturalId);
                entityDef.addColumn(jpaColumn);
            }
        }
    }

    private JpaColumn parseProperty(final Element parentElement, final String uniqueConstraint) {
        final String update = parentElement.getAttribute(Attributes.ATTR_UPDATE);
        final String optimisticLock = parentElement.getAttribute(Attributes.ATTR_OPTIMISTIC_LOCK);

        final JpaColumn jpaColumn = new JpaColumn();
        jpaColumn.setType(parentElement.getAttribute(Attributes.ATTR_TYPE));
        jpaColumn.setName(parentElement.getAttribute(Attributes.ATTR_NAME));
        jpaColumn.setColumnName(parentElement.getAttribute(Attributes.ATTR_COLUMN));
        jpaColumn.setLength(parentElement.getAttribute(Attributes.ATTR_LENGTH));
        jpaColumn.setUpdatable(StringUtils.isBlank(update) || Boolean.parseBoolean(update));
        jpaColumn.setLazy(Boolean.parseBoolean(parentElement.getAttribute(Attributes.ATTR_LAZY)));
        jpaColumn.setOptimisticLock(StringUtils.isBlank(optimisticLock) || Boolean.parseBoolean(optimisticLock));
        jpaColumn.setUniqueConstraint(uniqueConstraint);

        final Element typeElement = Utils.getFirstChildByTag(parentElement, Tags.TAG_TYPE);
        if (typeElement != null) {
            jpaColumn.setType(typeElement.getAttribute(Attributes.ATTR_NAME));

            final List<Element> typeParams = Utils.getChildrenByTag(typeElement, Tags.TAG_PARAM);
            for (final Element typeParamElement : typeParams) {
                final String typeName = typeParamElement.getAttribute(Attributes.ATTR_NAME);
                final String typeValue = typeParamElement.getTextContent().trim();
                jpaColumn.addTypeParam(typeName, typeValue);
            }
        }
        return jpaColumn;
    }

    private List<JpaColumn> parseColumns(final Element parentElement, final String uniqueConstraint) {
        final List<JpaColumn> jpaColumns = new ArrayList<>();

        final List<Element> columns = Utils.getChildrenByTag(parentElement, Tags.TAG_COLUMN);
        for (final Element columnElement : columns) {
            final JpaColumn jpaColumn = parseProperty(parentElement, uniqueConstraint);

            if (Tags.TAG_MANY_TO_MANY.equals(parentElement.getTagName())) {
                jpaColumn.setInverseJoin(true);
            }

            parseColumn(columnElement, jpaColumn);
            jpaColumns.add(jpaColumn);
        }
        return jpaColumns;
    }

    private void parseColumn(final Element columnElement, final JpaColumn jpaColumn) {
        jpaColumn.setColumnName(columnElement.getAttribute(Attributes.ATTR_NAME));
        jpaColumn.setLength(columnElement.getAttribute(Attributes.ATTR_LENGTH));
        jpaColumn.setNullable(!Boolean.parseBoolean(columnElement.getAttribute(Attributes.ATTR_NOT_NULL)));
        jpaColumn.setIndex(columnElement.getAttribute(Attributes.ATTR_INDEX));
        jpaColumn.setUnique(Boolean.parseBoolean(columnElement.getAttribute(Attributes.ATTR_UNIQUE)));
        jpaColumn.setDefaultValue(columnElement.getAttribute(Attributes.ATTR_DEFAULT));
        if (StringUtils.isBlank(jpaColumn.getUniqueConstraint())) {
            jpaColumn.setUniqueConstraint(columnElement.getAttribute(Attributes.ATTR_UNIQUE_KEY));
        }
        jpaColumn.setColumnDefinition(columnElement.getAttribute(Attributes.ATTR_SQL_TYPE));
        jpaColumn.setPrecision(NumberUtils.toInt(columnElement.getAttribute(Attributes.ATTR_PRECISION)));
        jpaColumn.setScale(NumberUtils.toInt(columnElement.getAttribute(Attributes.ATTR_SCALE)));
    }

    private void parseNaturalIds(final Element element, final JpaEntity entityDef) {
        final List<Element> naturalIdElements = Utils.getChildrenByTag(element, Tags.TAG_NATURAL_ID);
        for (final Element naturalIdElement : naturalIdElements) {
            final String mutable = naturalIdElement.getAttribute(Attributes.ATTR_MUTABLE);
            final JpaColumn.NaturalId naturalId = "true".equals(mutable) ?
                    JpaColumn.NaturalId.MUTABLE : JpaColumn.NaturalId.IMMUTABLE;
            parseProperty(naturalIdElement, entityDef, naturalId);
            parseRelationships(naturalIdElement, entityDef);
        }
    }

    private void parseRelationships(final Element element, final JpaEntity entityDef) {
        parseRelationships(null, element, entityDef, null);
    }

    private void parseRelationships(final Element element, final JpaEntity entityDef, final String uniqueConstraintName) {
        parseRelationships(null, element, entityDef, uniqueConstraintName);
    }

    private void parseRelationships(final JpaRelationship collectionRelationship, final Element element,
            final JpaEntity entityDef, final String uniqueConstraintName) {
        final List<Element> manyToOneElements = Utils.getChildrenByTag(element, Tags.TAG_MANY_TO_ONE);
        for (final Element relationshipElement : manyToOneElements) {
            final JpaRelationship relationship = new JpaRelationship();
            relationship.setType(JpaRelationship.Type.ManyToOne);
            relationship.setFetch("eager");
            parseRelationship(relationship, relationshipElement, entityDef, uniqueConstraintName);
        }

        final List<Element> oneToOneElements = Utils.getChildrenByTag(element, Tags.TAG_ONE_TO_ONE);
        for (final Element relationshipElement : oneToOneElements) {
            final JpaRelationship relationship = new JpaRelationship();
            relationship.setType(JpaRelationship.Type.OneToOne);
            relationship.setFetch("eager");
            parseRelationship(relationship, relationshipElement, entityDef, uniqueConstraintName);

            if (entityDef.getPrimaryKey() != null && "FOREIGN".equals(entityDef.getPrimaryKey().getGeneratorType())) {
                final JpaColumn keyColumn = new JpaColumn();
                keyColumn.setName(relationship.getName());
                keyColumn.setColumnName(entityDef.getPrimaryKey().getColumnName());
                relationship.addReferencedColumn(keyColumn);
            }
        }

        final List<Element> oneToManyElements = Utils.getChildrenByTag(element, Tags.TAG_ONE_TO_MANY);
        for (final Element relationshipElement : oneToManyElements) {
            collectionRelationship.setType(JpaRelationship.Type.OneToMany);
            parseRelationship(collectionRelationship, relationshipElement, entityDef, uniqueConstraintName);
        }

        final List<Element> manyToManyElements = Utils.getChildrenByTag(element, Tags.TAG_MANY_TO_MANY);
        for (final Element relationshipElement : manyToManyElements) {
            collectionRelationship.setType(JpaRelationship.Type.ManyToMany);
            parseRelationship(collectionRelationship, relationshipElement, entityDef, uniqueConstraintName);
        }
    }

    private void parseRelationship(final JpaRelationship relationship, final Element relationshipElement,
            final JpaEntity entityDef, final String uniqueConstraintName) {
        final String name = relationshipElement.getAttribute(Attributes.ATTR_NAME);
        final String targetEntity = relationshipElement.getAttribute(Attributes.ATTR_CLASS);
        final String cascade = relationshipElement.getAttribute(Attributes.ATTR_CASCADE);
        final String access = relationshipElement.getAttribute(Attributes.ATTR_ACCESS);
        final String index = relationshipElement.getAttribute(Attributes.ATTR_INDEX);
        final String update = relationshipElement.getAttribute(Attributes.ATTR_UPDATE);
        final String notNull = relationshipElement.getAttribute(Attributes.ATTR_NOT_NULL);
        final String foreignKey = relationshipElement.getAttribute(Attributes.ATTR_FOREIGN_KEY);
        final boolean unique = Boolean.parseBoolean(relationshipElement.getAttribute(Attributes.ATTR_UNIQUE));
        final boolean optional = !Boolean.parseBoolean(relationshipElement.getAttribute(Attributes.ATTR_CONSTRAINED));
        final String mappedBy = relationshipElement.getAttribute(Attributes.ATTR_PROPERTY_REF);

        if (StringUtils.isBlank(relationship.getName())) {
            relationship.setName(name);
        }
        relationship.setTargetEntity(targetEntity);

        if (StringUtils.isNotBlank(relationshipElement.getAttribute(Attributes.ATTR_LAZY))) {
            relationship.setFetch("false".equals(relationshipElement.getAttribute(Attributes.ATTR_LAZY)) ? "eager" : "lazy");
        }
        relationship.setCascade(cascade, entityDef.getDefaultCascade());
        relationship.setAccess(access);
        relationship.setOptional(optional);
        relationship.setMappedBy(mappedBy);

        final List<JpaColumn> jpaColumns = parseColumns(relationshipElement, null);
        if (!jpaColumns.isEmpty()) {
            for (final JpaColumn jpaColumn : jpaColumns) {
                if (StringUtils.isNotBlank(update)) {
                    jpaColumn.setUpdatable(StringUtils.isBlank(update) || Boolean.parseBoolean(update));
                }
                if (StringUtils.isNotBlank(notNull)) {
                    jpaColumn.setNullable(!Boolean.parseBoolean(notNull));
                }
                jpaColumn.setForeignKey(foreignKey);
                jpaColumn.setIndex(index);
                if (!jpaColumn.isUnique()) {
                    jpaColumn.setUnique(unique);
                }
                jpaColumn.setUniqueConstraint(uniqueConstraintName);
            }

            relationship.setReferencedColumns(jpaColumns);
        } else {
            final String columnName = relationshipElement.getAttribute(Attributes.ATTR_COLUMN);
            if (StringUtils.isNotBlank(columnName)) {
                final JpaColumn keyColumn = new JpaColumn();
                keyColumn.setName(relationship.getName());
                keyColumn.setColumnName(columnName);
                keyColumn.setInverseJoin(true);
                relationship.addReferencedColumn(keyColumn);
            }
        }

        entityDef.addRelationship(relationship);
    }

    private void parseCollections(final Element element, final JpaEntity entityDef) {
        List<Element> collectionElements = Utils.getChildrenByTag(element, Tags.TAG_SET);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_SET);
        }

        collectionElements = Utils.getChildrenByTag(element, Tags.TAG_LIST);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_LIST);
        }

        collectionElements = Utils.getChildrenByTag(element, Tags.TAG_BAG);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_BAG);
        }

        collectionElements = Utils.getChildrenByTag(element, Tags.TAG_MAP);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_MAP);
        }
    }

    private void parseCollection(final Element collectionElement, final JpaEntity entityDef, final String collectionType) {
        final JpaRelationship relationship = new JpaRelationship();
        relationship.setName(collectionElement.getAttribute(Attributes.ATTR_NAME));
        relationship.setTable(collectionElement.getAttribute(Attributes.ATTR_TABLE));

        // Find the <one-to-many> or <many-to-many> within the collection
        parseRelationships(relationship, collectionElement, entityDef, null);

        relationship.setInverse(Boolean.parseBoolean(collectionElement.getAttribute(Attributes.ATTR_INVERSE)));
        relationship.setCascade(collectionElement.getAttribute(Attributes.ATTR_CASCADE), entityDef.getDefaultCascade());
        relationship.setCollectionType(collectionType);

        if (StringUtils.isNotBlank(collectionElement.getAttribute(Attributes.ATTR_LAZY))) {
            relationship.setFetch("false".equals(collectionElement.getAttribute(Attributes.ATTR_LAZY)) ? "eager" : "lazy");
        }

        final Element keyElement = Utils.getFirstChildByTag(collectionElement, Tags.TAG_KEY);
        if (keyElement != null) {
            final JpaColumn keyColumn;

            if (StringUtils.isNotBlank(keyElement.getAttribute(Attributes.ATTR_COLUMN))) {
                keyColumn = new JpaColumn();
                keyColumn.setName(relationship.getName());
                keyColumn.setColumnName(keyElement.getAttribute(Attributes.ATTR_COLUMN));
            } else {
                final List<JpaColumn> jpaColumns = parseColumns(keyElement, null);
                keyColumn = jpaColumns.getFirst();
            }
            if (StringUtils.isNotBlank(keyElement.getAttribute(Attributes.ATTR_FOREIGN_KEY))) {
                keyColumn.setForeignKey(keyElement.getAttribute(Attributes.ATTR_FOREIGN_KEY));
            }
            relationship.addReferencedColumn(keyColumn);
        }

        final Element mapKeyElement = Utils.getFirstChildByTag(collectionElement, Tags.TAG_MAP_KEY);
        if (mapKeyElement != null) {
            final JpaColumn keyColumn = relationship.getReferencedColumns().getFirst();
            keyColumn.setType(Utils.mapHibernateTypeToJava(mapKeyElement.getAttribute(Attributes.ATTR_TYPE)));
            keyColumn.setName(mapKeyElement.getAttribute(Attributes.ATTR_COLUMN));
        }

        final Element compositeMapkeyElement = Utils.getFirstChildByTag(collectionElement, Tags.TAG_COMPOSITE_MAP_KEY);
        if (compositeMapkeyElement != null) {
            final List<Element> keyProperties = Utils.getChildrenByTag(compositeMapkeyElement, Tags.TAG_KEY_PROPERTY);
            for (final Element keyProperty : keyProperties) {
                final JpaColumn keyColumn = new JpaColumn();
                keyColumn.setType(compositeMapkeyElement.getAttribute(Attributes.ATTR_CLASS));
                if (StringUtils.isBlank(keyColumn.getType())) {
                    keyColumn.setType(keyProperty.getAttribute(Attributes.ATTR_TYPE));
                }
                keyColumn.setName(keyProperty.getAttribute(Attributes.ATTR_NAME));
                keyColumn.setColumnName(keyProperty.getAttribute(Attributes.ATTR_COLUMN));
                relationship.addReferencedColumn(keyColumn);
            }
        }

        relationship.setOrderColumn(collectionElement.getAttribute(Attributes.ATTR_ORDER_BY));

        final Element indexElement = Utils.getFirstChildByTag(collectionElement, Tags.TAG_LIST_INDEX);
        if (indexElement != null) {
            relationship.setOrderColumn(indexElement.getAttribute(Attributes.ATTR_COLUMN));
        }
    }

    private void parseEmbeddedFields(final Element element, final JpaEntity entityDef) {
        final List<Element> componentElements = Utils.getChildrenByTag(element, Tags.TAG_COMPONENT);
        for (final Element componentElement : componentElements) {
            final JpaEntity embeddedField = new JpaEntity();
            embeddedField.setParentClass(componentElement.getAttribute(Attributes.ATTR_NAME));
            embeddedField.setClassName(componentElement.getAttribute(Attributes.ATTR_CLASS));
            embeddedField.setEmbeddable(true);
            parseProperty(componentElement, embeddedField);

            entityDef.addEmbeddedField(embeddedField);
        }
    }

    private void parseQueries(final Element root, final JpaEntity jpaEntity) {
        final List<Element> queries = Utils.getChildrenByTag(root, Tags.TAG_QUERY);
        for (final Element query : queries) {
            final JpaNamedQuery namedQuery = new JpaNamedQuery();
            namedQuery.setName(query.getAttribute(Attributes.ATTR_NAME));
            namedQuery.setQuery(query.getTextContent().trim());
            jpaEntity.addNamedQuery(namedQuery);
        }

        final List<Element> sqlQueries = Utils.getChildrenByTag(root, Tags.TAG_SQL_QUERY);
        for (final Element sqlQuery : sqlQueries) {
            final JpaNamedQuery namedQuery = new JpaNamedQuery();
            namedQuery.setName(sqlQuery.getAttribute(Attributes.ATTR_NAME));
            namedQuery.setQuery(sqlQuery.getTextContent().trim());
            namedQuery.setNativeQuery(true);
            parseQueryReturnColumns(sqlQuery, namedQuery);
            jpaEntity.addNamedQuery(namedQuery);
        }
    }

    private void parseQueryReturnColumns(final Element element, final JpaNamedQuery namedQuery) {
        final List<Element> returnScalarElements = Utils.getChildrenByTag(element, Tags.TAG_RETURN_SCALAR);
        for (final Element returnScalarElement : returnScalarElements) {
            final JpaColumn returnColumn = new JpaColumn();
            returnColumn.setColumnName(returnScalarElement.getAttribute(Attributes.ATTR_COLUMN));
            returnColumn.setType(returnScalarElement.getAttribute(Attributes.ATTR_TYPE));
            namedQuery.addReturnColumn(returnColumn);
        }
    }

    private void parseGeneratorParameters(final String paramValue, final JpaPrimaryKey primaryKey) {
        final Pattern PARAMETERS_PATTERN = Pattern.compile("^START WITH\\s+(\\d+)(?:\\s+CACHE\\s+(\\d+))?$",
                Pattern.CASE_INSENSITIVE);
        final Matcher matcher = PARAMETERS_PATTERN.matcher(paramValue);

        if (matcher.matches()) {
            try {
                // Convert the captured strings to integers
                primaryKey.setInitialValue(Integer.parseInt(matcher.group(1)));
                if (StringUtils.isNotBlank(matcher.group(2))) {
                    primaryKey.setAllocationSize(Integer.parseInt(matcher.group(2)));
                }
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Could not parse generator parameters within the string: " + paramValue, e);
            }
        } else {
            throw new IllegalArgumentException(
                    "Generator Parameter string does not match expected format 'START WITH <number> CACHE <number>': " + paramValue);
        }
    }

    private void checkMissingTagAttributeImplementations(final Element parent) {
        if (!Tags.TAGS.contains(parent.getTagName())) {
            LOG.error("ATTENTION: No implementation for tag " + parent.getTagName());
        }

        for (int j = 0; j < parent.getAttributes().getLength(); j++) {
            final Node attrElement = parent.getAttributes().item(j);
            if (Attributes.ATTRIBUTES.get(parent.getTagName()) == null ||
                    !Attributes.ATTRIBUTES.get(parent.getTagName()).contains(attrElement.getNodeName())) {
                LOG.error("ATTENTION: No implementation for attribute " + attrElement.getNodeName() +
                        " of tag " + parent.getTagName());
            }
        }

        final List<Element> children = Utils.getChildrenByTag(parent, null);
        for (final Element child : children) {
            checkMissingTagAttributeImplementations(child);
        }
    }

}
