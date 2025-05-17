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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.devtools.Utils;
import com.devtools.definition.Attributes;
import com.devtools.definition.JpaColumn;
import com.devtools.definition.JpaEntity;
import com.devtools.definition.JpaNamedQuery;
import com.devtools.definition.JpaPrimaryKey;
import com.devtools.definition.JpaRelationship;
import com.devtools.definition.Tags;

public class HbmParser {

    private static final Log LOG = LogFactory.getLog(HbmParser.class);

    public JpaEntity parse(final String filePath) throws Exception {
        final JpaEntity entityDef = new JpaEntity();

        final Element root = getRootElement(filePath);

        final Element classElement = parseEntityDefinition(root, entityDef);

        if (classElement != null) {
            parseIdAndGenerator(classElement, entityDef);

            parseNaturalIds(classElement, entityDef);

            parseVersions(classElement, entityDef);

            parseProperties(classElement, entityDef);

            parseProperty(classElement, entityDef, null, JpaColumn.NaturalId.NONE, true);

            parseRelationships(classElement, entityDef);

            parseCollections(classElement, entityDef);

            parseEmbeddedFields(classElement, entityDef);
        }

        parseQueries(root, entityDef);

        // in case of *.hbm.xml file containing only queries
        verifyUtilitiesFile(filePath, entityDef);

        validateMissingImplementations(root);

        return entityDef;
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

    private Element parseEntityDefinition(final Element root, final JpaEntity entityDef) {
        Element classElement = null;

        entityDef.setDefaultCascade(root.getAttribute(Attributes.ATTR_HIBERNATE_MAPPING_DEFAULT_CASCADE));

        // Extract the class element (or subclass if present)
        final List<Element> classElements = Utils.getChildrenByTag(root, Tags.TAG_CLASS);
        if (!classElements.isEmpty()) {
            classElement = classElements.getFirst();
        } else {
            // Handle subclass or joined-subclass if needed
            List<Element> subclassElements = Utils.getChildrenByTag(root, Tags.TAG_SUBCLASS);
            if (subclassElements.isEmpty()) {
                subclassElements = Utils.getChildrenByTag(root, Tags.TAG_UNION_SUBCLASS);
            }
            if (!subclassElements.isEmpty()) {
                classElement = subclassElements.getFirst();
            }
        }

        if (classElement != null) {
            entityDef.setClassName(classElement.getAttribute(Attributes.ATTR_ENTITY_NAME));
            entityDef.setTable(classElement.getAttribute(Attributes.ATTR_ENTITY_TABLE));
            entityDef.setDynamicInsert(classElement.getAttribute(Attributes.ATTR_ENTITY_DYNAMIC_INSERT));
            entityDef.setDynamicUpdate(classElement.getAttribute(Attributes.ATTR_ENTITY_DYNAMIC_UPDATE));
            entityDef.setAbstractClass(classElement.getAttribute(Attributes.ATTR_ENTITY_ABSTRACT));
            entityDef.setMutable(classElement.getAttribute(Attributes.ATTR_ENTITY_MUTABLE));

            // Only for subclasses
            entityDef.setParentClass(classElement.getAttribute(Attributes.ATTR_ENTITY_EXTENDS));
            entityDef.setDiscriminatorValue(classElement.getAttribute(Attributes.ATTR_ENTITY_DISCRIMINATOR_VALUE));

            final List<Element> cacheElements = Utils.getChildrenByTag(classElement, Tags.TAG_CACHE);
            if (!cacheElements.isEmpty()) {
                final Element cacheElement = cacheElements.getFirst();
                entityDef.setCacheUsage(cacheElement.getAttribute(Attributes.ATTR_CACHE_USAGE));
            }

            final List<Element> joinElements = Utils.getChildrenByTag(classElement, Tags.TAG_JOIN);
            if (!joinElements.isEmpty()) {
                final Element joinElement = joinElements.getFirst();
                entityDef.setTable(joinElement.getAttribute(Attributes.ATTR_ENTITY_TABLE));
                entityDef.setSecondTable(true);

                parseProperty(joinElement, entityDef);
            }
        }

        // Parse Discriminator Details
        parseDiscriminator(root, entityDef);

        return classElement;
    }

    private void parseDiscriminator(final Element root, final JpaEntity entityDef) {
        final List<Element> discriminatorNodes = Utils.getChildrenByTag(root, Tags.TAG_DISCRIMINATOR);
        if (!discriminatorNodes.isEmpty()) {
            final Element discriminatorElement = discriminatorNodes.getFirst();

            // Extract column details if present
            final List<Element> columnNodes = Utils.getChildrenByTag(discriminatorElement, Tags.TAG_COLUMN);
            if (!columnNodes.isEmpty()) {
                final Element columnElement = columnNodes.getFirst();

                // Set discriminator column name
                final String columnName = columnElement.getAttribute(Attributes.ATTR_DISCRIMINATOR_NAME);
                if (!columnName.isEmpty()) {
                    entityDef.setDiscriminatorColumn(columnName);
                }

                // Set discriminator column length
                final String length = columnElement.getAttribute(Attributes.ATTR_DISCRIMINATOR_LENGTH);
                if (!length.isEmpty()) {
                    entityDef.setDiscriminatorLength(Integer.parseInt(length));
                }
            }
        }
    }

    private void parseIdAndGenerator(final Element element, final JpaEntity entityDef) {
        final List<Element> idElements = Utils.getChildrenByTag(element, Tags.TAG_ID);
        if (!idElements.isEmpty()) {
            final Element idElement = idElements.getFirst();
            final JpaPrimaryKey primaryKey = new JpaPrimaryKey();

            // Set the attribute name and type
            primaryKey.setName(idElement.getAttribute(Attributes.ATTR_ID_NAME));
            primaryKey.setType(Utils.mapHibernateTypeToJava(idElement.getAttribute(Attributes.ATTR_ID_TYPE)));

            // Handle the column name
            final List<Element> columnElements = Utils.getChildrenByTag(idElement, Tags.TAG_COLUMN);
            if (!columnElements.isEmpty()) {
                final Element columnElement = columnElements.getFirst();
                primaryKey.setColumnName(columnElement.getAttribute(Attributes.ATTR_COLUMN_NAME));
            }

            // Handle the generator
            final List<Element> generatorElements = Utils.getChildrenByTag(idElement, Tags.TAG_GENERATOR);
            if (!generatorElements.isEmpty()) {
                final Element generatorElement = generatorElements.getFirst();
                final String generatorClass = generatorElement.getAttribute(Attributes.ATTR_GENERATOR_CLASS);
                final Map<String, String> params = new HashMap<>();

                // Collect all params
                final List<Element> paramElements = Utils.getChildrenByTag(generatorElement, Tags.TAG_PARAM);
                for (final Element paramElement : paramElements) {
                    final String paramName = paramElement.getAttribute(Attributes.ATTR_PARAM_NAME);
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
            final List<JpaColumn> jpaColumns = parseColumns(versionElement, true, null,
                    JpaColumn.NaturalId.NONE);
            for (final JpaColumn jpaColumn : jpaColumns) {
                entityDef.addColumn(jpaColumn);
            }
        }
    }

    private void parseProperties(final Element element, final JpaEntity entityDef) {
        final List<Element> propertiesElements = Utils.getChildrenByTag(element, Tags.TAG_PROPERTIES);
        for (final Element propertiesElement : propertiesElements) {
            final String uniqueConstraintName = propertiesElement.getAttribute(Attributes.ATTR_PROPERTIES_NAME);
            parseProperty(propertiesElement, entityDef, uniqueConstraintName, JpaColumn.NaturalId.NONE, false);
        }
    }

    private void parseProperty(final Element root, final JpaEntity entityDef) {
        parseProperty(root, entityDef, null, JpaColumn.NaturalId.NONE, false);
    }

    private void parseProperty(final Element element, final JpaEntity entityDef,
            final String uniqueConstraint, final JpaColumn.NaturalId naturalId, final boolean checkComposite) {
        final List<Element> propertyElements = Utils.getChildrenByTag(element, Tags.TAG_PROPERTY);
        for (final Element propertyElement : propertyElements) {
            final List<JpaColumn> jpaColumns = parseColumns(propertyElement, false, uniqueConstraint,
                    naturalId);
            for (final JpaColumn jpaColumn : jpaColumns) {
                if (checkComposite && jpaColumns.size() > 1) {
                    jpaColumn.setComposite(true);
                }
                entityDef.addColumn(jpaColumn);
            }

            final List<Element> types = Utils.getChildrenByTag(propertyElement, Tags.TAG_TYPE);
            if (!types.isEmpty()) {
                final Element typeElement = types.getFirst();
                jpaColumns.getFirst().setType(typeElement.getAttribute(Attributes.ATTR_TYPE_NAME));

                final List<Element> typeParams = Utils.getChildrenByTag(typeElement, Tags.TAG_PARAM);
                for (final Element typeParamElement : typeParams) {
                    final String typeName = typeParamElement.getAttribute(Attributes.ATTR_PARAM_NAME);
                    final String typeValue = typeParamElement.getTextContent().trim();
                    jpaColumns.getFirst().addTypeParam(typeName, typeValue);
                }
            }
        }
    }

    private List<JpaColumn> parseColumns(final Element element, final boolean version, final String uniqueConstraint,
            final JpaColumn.NaturalId naturalId) {
        final List<JpaColumn> jpaColumns = new ArrayList<>();
        final String propertyName = element.getAttribute(Attributes.ATTR_PROPERTY_NAME);
        final String propertyType = element.getAttribute(Attributes.ATTR_PROPERTY_TYPE);
        final String updatable = element.getAttribute(Attributes.ATTR_PROPERTY_UPDATE);
        final String optimisticLock = element.getAttribute(Attributes.ATTR_PROPERTY_OPTIMISTIC_LOCK);

        final List<Element> columns = Utils.getChildrenByTag(element, Tags.TAG_COLUMN);
        for (final Element columnElement : columns) {
            final JpaColumn jpaColumn = new JpaColumn(propertyType, propertyName);
            jpaColumn.setVersion(version);
            jpaColumn.setNaturalId(naturalId);
            jpaColumn.setUpdatable(StringUtils.isBlank(updatable) || Boolean.parseBoolean(updatable));
            jpaColumn.setOptimisticLock(StringUtils.isBlank(optimisticLock) || Boolean.parseBoolean(optimisticLock));

            if (Tags.TAG_MANY_TO_MANY.equals(element.getTagName())) {
                jpaColumn.setInverseJoin(true);
            }

            jpaColumns.add(parseColumn(columnElement, jpaColumn, uniqueConstraint));
        }
        return jpaColumns;
    }

    private JpaColumn parseColumn(final Element columnElement, final JpaColumn jpaColumn, String uniqueConstraint) {
        final String strLen = columnElement.getAttribute(Attributes.ATTR_COLUMN_LENGTH);
        final Integer length = !strLen.isEmpty() ? Integer.parseInt(strLen) : null;

        if (StringUtils.isBlank(uniqueConstraint)) {
            uniqueConstraint = columnElement.getAttribute(Attributes.ATTR_COLUMN_UNIQUE_KEY);
            if (StringUtils.isBlank(uniqueConstraint)) {
                uniqueConstraint = null;
            }
        }

        jpaColumn.setColumnName(columnElement.getAttribute(Attributes.ATTR_COLUMN_NAME));
        jpaColumn.setLength(length);
        jpaColumn.setNullable(!Boolean.parseBoolean(columnElement.getAttribute(Attributes.ATTR_COLUMN_NOT_NULL)));
        jpaColumn.setIndex(columnElement.getAttribute(Attributes.ATTR_COLUMN_INDEX));
        jpaColumn.setUnique(Boolean.parseBoolean(columnElement.getAttribute(Attributes.ATTR_COLUMN_UNIQUE)));
        jpaColumn.setDefaultValue(columnElement.getAttribute(Attributes.ATTR_COLUMN_DEFAULT));
        jpaColumn.setUniqueConstraint(uniqueConstraint);
        jpaColumn.setColumnDefinition(columnElement.getAttribute(Attributes.ATTR_COLUMN_SQL_TYPE));
        jpaColumn.setPrecision(NumberUtils.toInt(columnElement.getAttribute(Attributes.ATTR_COLUMN_PRECISION)));
        jpaColumn.setScale(NumberUtils.toInt(columnElement.getAttribute(Attributes.ATTR_COLUMN_SCALE)));
        return jpaColumn;
    }

    private void parseNaturalIds(final Element element, final JpaEntity entityDef) {
        final List<Element> naturalIdElements = Utils.getChildrenByTag(element, Tags.TAG_NATURAL_ID);
        for (final Element naturalId : naturalIdElements) {
            final String mutable = naturalId.getAttribute(Attributes.ATTR_NATURAL_ID_MUTABLE);
            parseProperty(naturalId, entityDef, null,
                    "true".equals(mutable) ? JpaColumn.NaturalId.MUTABLE : JpaColumn.NaturalId.IMMUTABLE, false);
            parseRelationships(naturalId, entityDef);
        }
    }

    private void parseRelationships(final Element element, final JpaEntity entityDef) {
        parseRelationships(null, element, entityDef);
    }

    private void parseRelationships(final JpaRelationship collectionRelationship, final Element element,
            final JpaEntity entityDef) {
        final List<Element> manyToOneElements = Utils.getChildrenByTag(element, Tags.TAG_MANY_TO_ONE);
        for (final Element itemElement : manyToOneElements) {
            final JpaRelationship relationship = new JpaRelationship();
            relationship.setType("ManyToOne");
            parseRelationship(relationship, itemElement, entityDef);
        }

        final List<Element> oneToOneElements = Utils.getChildrenByTag(element, Tags.TAG_ONE_TO_ONE);
        for (final Element itemElement : oneToOneElements) {
            final JpaRelationship relationship = new JpaRelationship();
            relationship.setType("OneToOne");
            parseRelationship(relationship, itemElement, entityDef);
        }

        final List<Element> oneToManyElements = Utils.getChildrenByTag(element, Tags.TAG_ONE_TO_MANY);
        for (final Element itemElement : oneToManyElements) {
            collectionRelationship.setType("OneToMany");
            parseRelationship(collectionRelationship, itemElement, entityDef);
        }

        final List<Element> manyToManyElements = Utils.getChildrenByTag(element, Tags.TAG_MANY_TO_MANY);
        for (final Element itemElement : manyToManyElements) {
            collectionRelationship.setType("ManyToMany");
            parseRelationship(collectionRelationship, itemElement, entityDef);
        }
    }

    private void parseRelationship(final JpaRelationship relationship, final Element itemElement,
            final JpaEntity entityDef) {
        final String name = itemElement.getAttribute(Attributes.ATTR_RELATIONSHIP_NAME);
        final String targetEntity = itemElement.getAttribute(Attributes.ATTR_RELATIONSHIP_CLASS);
        final String fetch = "false".equals(itemElement.getAttribute(Attributes.ATTR_RELATIONSHIP_LAZY)) ? "eager" : "lazy";
        final String cascade = itemElement.getAttribute(Attributes.ATTR_RELATIONSHIP_CASCADE);
        final String access = itemElement.getAttribute(Attributes.ATTR_RELATIONSHIP_ACCESS);
        final String index = itemElement.getAttribute(Attributes.ATTR_RELATIONSHIP_INDEX);

        if (StringUtils.isBlank(relationship.getName())) {
            relationship.setName(name);
        }
        relationship.setTargetEntity(targetEntity);
        relationship.setFetch(fetch);
        relationship.setCascade(cascade, entityDef.getDefaultCascade());
        relationship.setAccess(access);

        final List<JpaColumn> jpaColumns = parseColumns(itemElement, false, null,
                JpaColumn.NaturalId.NONE);
        if (!jpaColumns.isEmpty()) {
            if (StringUtils.isNotBlank(index)) {
                for (final JpaColumn jpaColumn : jpaColumns) {
                    jpaColumn.setIndex(index);
                }
            }
            relationship.setReferencedColumns(jpaColumns);
        }

        entityDef.addRelationship(relationship);
    }

    private void parseCollections(final Element root, final JpaEntity entityDef) {
        List<Element> collectionElements = Utils.getChildrenByTag(root, Tags.TAG_SET);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_SET);
        }

        collectionElements = Utils.getChildrenByTag(root, Tags.TAG_LIST);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_LIST);
        }

        collectionElements = Utils.getChildrenByTag(root, Tags.TAG_BAG);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_BAG);
        }

        collectionElements = Utils.getChildrenByTag(root, Tags.TAG_MAP);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_MAP);
        }
    }

    private void parseCollection(final Element collectionElement, final JpaEntity entityDef, final String collectionType) {
        final JpaRelationship relationship = new JpaRelationship();
        relationship.setName(collectionElement.getAttribute(Attributes.ATTR_RELATIONSHIP_NAME));
        relationship.setTable(collectionElement.getAttribute(Attributes.ATTR_RELATIONSHIP_TABLE));

        // Find the <one-to-many> or <many-to-many> within the collection
        parseRelationships(relationship, collectionElement, entityDef);

        relationship.setInverse(Boolean.parseBoolean(collectionElement.getAttribute(Attributes.ATTR_RELATIONSHIP_INVERSE)));
        relationship.setFetch("false".equals(collectionElement.getAttribute(Attributes.ATTR_RELATIONSHIP_LAZY)) ? "eager" : "lazy");
        relationship.setCascade(collectionElement.getAttribute(Attributes.ATTR_RELATIONSHIP_CASCADE), entityDef.getDefaultCascade());
        relationship.setCollectionType(collectionType);

        final List<Element> keyElements = Utils.getChildrenByTag(collectionElement, Tags.TAG_KEY);
        if (!keyElements.isEmpty()) {
            final Element keyElement = keyElements.getFirst();
            final JpaColumn keyColumn;

            if (StringUtils.isNotBlank(keyElement.getAttribute(Attributes.ATTR_KEY_COLUMN))) {
                keyColumn = new JpaColumn();
                keyColumn.setName(relationship.getName());
                keyColumn.setColumnName(keyElement.getAttribute(Attributes.ATTR_KEY_COLUMN));
            } else {
                final List<JpaColumn> jpaColumns = parseColumns(keyElement, false, null,
                        JpaColumn.NaturalId.NONE);
                keyColumn = jpaColumns.getFirst();
            }
            if (StringUtils.isNotBlank(keyElement.getAttribute(Attributes.ATTR_KEY_FOREIGN_KEY))) {
                keyColumn.setForeignKey(keyElement.getAttribute(Attributes.ATTR_KEY_FOREIGN_KEY));
            }
            relationship.addReferencedColumn(keyColumn);
        }

        final List<Element> mapKeyElements = Utils.getChildrenByTag(collectionElement, Tags.TAG_MAP_KEY);
        if (!mapKeyElements.isEmpty()) {
            final Element mapKeyElement = mapKeyElements.getFirst();
            final JpaColumn keyColumn = relationship.getReferencedColumns().getFirst();
            keyColumn.setType(Utils.mapHibernateTypeToJava(mapKeyElement.getAttribute(Attributes.ATTR_MAP_KEY_TYPE)));
            keyColumn.setName(mapKeyElement.getAttribute(Attributes.ATTR_MAP_KEY_COLUMN));
        }

        final List<Element> compositeKeyElements = Utils.getChildrenByTag(collectionElement, Tags.TAG_COMPOSITE_MAP_KEY);
        if (!compositeKeyElements.isEmpty()) {
            final Element keyElement = compositeKeyElements.getFirst();

            final List<Element> keyProperties = Utils.getChildrenByTag(keyElement, Tags.TAG_KEY_PROPERTY);
            for (final Element keyProperty : keyProperties) {
                final JpaColumn keyColumn = new JpaColumn();
                keyColumn.setType(keyElement.getAttribute(Attributes.ATTR_COMPOSITE_MAP_KEY_CLASS));
                keyColumn.setName(keyProperty.getAttribute(Attributes.ATTR_KEY_PROPERTY_NAME));
                keyColumn.setColumnName(keyProperty.getAttribute(Attributes.ATTR_KEY_PROPERTY_COLUMN));
                relationship.addReferencedColumn(keyColumn);
            }
        }

        relationship.setOrderColumn(collectionElement.getAttribute(Attributes.ATTR_RELATIONSHIP_ORDER_BY));

        final List<Element> indexElements = Utils.getChildrenByTag(collectionElement, Tags.TAG_LIST_INDEX);
        if (!indexElements.isEmpty()) {
            final Element indexElement = indexElements.getFirst();
            relationship.setOrderColumn(indexElement.getAttribute(Attributes.ATTR_LIST_INDEX_COLUMN));
        }
    }

    private void parseEmbeddedFields(final Element root, final JpaEntity entityDef) {
        final List<Element> components = Utils.getChildrenByTag(root, Tags.TAG_COMPONENT);
        for (final Element component : components) {
            final JpaEntity embeddedField = new JpaEntity();
            embeddedField.setParentClass(component.getAttribute(Attributes.ATTR_COMPONENT_NAME));
            embeddedField.setClassName(component.getAttribute(Attributes.ATTR_COMPONENT_CLASS));
            embeddedField.setEmbeddable(true);
            parseProperty(component, embeddedField);

            entityDef.addEmbeddedField(embeddedField);
        }
    }

    private void parseQueries(final Element root, final JpaEntity entityDef) {
        final List<Element> queries = Utils.getChildrenByTag(root, Tags.TAG_QUERY);
        for (final Element sqlQuery : queries) {
            final JpaNamedQuery namedQuery = new JpaNamedQuery();
            namedQuery.setName(sqlQuery.getAttribute(Attributes.ATTR_QUERY_NAME));
            namedQuery.setQuery(sqlQuery.getTextContent().trim());
            entityDef.addNamedQuery(namedQuery);
        }

        final List<Element> sqlQueries = Utils.getChildrenByTag(root, Tags.TAG_SQL_QUERY);
        for (final Element sqlQuery : sqlQueries) {
            final JpaNamedQuery namedQuery = new JpaNamedQuery();
            namedQuery.setName(sqlQuery.getAttribute(Attributes.ATTR_SQL_QUERY_NAME));
            namedQuery.setQuery(sqlQuery.getTextContent().trim());
            namedQuery.setNativeQuery(true);
            parseQueryReturnColumns(sqlQuery, namedQuery);
            entityDef.addNamedQuery(namedQuery);
        }
    }

    private void parseQueryReturnColumns(final Element element, final JpaNamedQuery namedQuery) {
        final NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && Tags.TAG_RETURN_SCALAR.equals(node.getNodeName())) {
                final Element propertyElement = (Element) node;
                final String columnName = propertyElement.getAttribute(Attributes.ATTR_RETURN_SCALAR_COLUMN);
                final String columnType = propertyElement.getAttribute(Attributes.ATTR_RETURN_SCALAR_TYPE);

                final JpaColumn returnColumn = new JpaColumn(columnType, columnName);
                namedQuery.addReturnColumn(returnColumn);
            }
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

    private void verifyUtilitiesFile(final String absoluteFilename, final JpaEntity entityDef) {
        if (StringUtils.isBlank(entityDef.getClassName())) {
            final String fileName = Utils.getFileNameNoExtensions(absoluteFilename);
            entityDef.setClassName(fileName);
        }
    }

    private void validateMissingImplementations(final Element parent) {
        final NodeList children = parent.getChildNodes();

        if (!Tags.TAGS.contains(parent.getTagName())) {
            LOG.error("ATTENTION: No implementation for tag " + parent.getTagName());
        }

        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);

            // Only process element nodes
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                for (int j = 0; j < parent.getAttributes().getLength(); j++) {
                    final Node attrElement = parent.getAttributes().item(j);
                    if (!Attributes.ATTRIBUTES.contains(attrElement.getNodeName())) {
                        LOG.error("ATTENTION: No implementation for attribute " + attrElement.getNodeName() +
                                " of tag " + parent.getTagName());
                    }
                }

                validateMissingImplementations((Element) child);
            }
        }
    }

}
