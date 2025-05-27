package com.devtools.processing;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

import com.devtools.model.hbm.Attributes;
import com.devtools.model.hbm.Tags;
import com.devtools.model.jpa.JpaColumn;
import com.devtools.model.jpa.JpaCompositeColumn;
import com.devtools.model.jpa.JpaDiscriminator;
import com.devtools.model.jpa.JpaEntity;
import com.devtools.model.jpa.JpaNamedQuery;
import com.devtools.model.jpa.JpaPrimaryKey;
import com.devtools.model.jpa.JpaRelationship;
import com.devtools.utils.ClassNameUtils;
import com.devtools.utils.DomUtils;
import com.devtools.utils.FileUtils;
import com.devtools.utils.HibernateUtils;

public class HbmParser {

    private static final Log LOG = LogFactory.getLog(HbmParser.class);

    public List<JpaEntity> parse(final String filePath) throws Exception {
        final List<JpaEntity> entities = new ArrayList<>();
        final Element root = getRootElement(filePath);
        final String packageName = root.getAttribute(Attributes.ATTR_PACKAGE);

        parseClasses(root, entities, packageName);

        // Check for a hbm.xml file only with queries (no entities)
        if (entities.isEmpty()) {
            final JpaEntity queriesEntity = new JpaEntity();
            parseQueries(root, queriesEntity);
            if (!queriesEntity.getNamedQueries().isEmpty()) {
                final String fileName = FileUtils.getFileNameNoExtensions(filePath);
                queriesEntity.setName((StringUtils.isNotBlank(packageName) ? packageName + "." : "") + fileName);
                entities.add(queriesEntity);
            }
        }

        checkMissingTagAttributeImplementations(root);

        return entities;
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

    private void parseClasses(final Element root, final List<JpaEntity> entities,
            final String packageName) {

        parseClasses(Tags.TAG_CLASS, root, entities, packageName);

        parseClasses(Tags.TAG_SUBCLASS, root, entities, packageName);

        parseClasses(Tags.TAG_UNION_SUBCLASS, root, entities, packageName);
    }

    private void parseClasses(final String tagName, final Element root, final List<JpaEntity> entities,
            final String packageName) {
        final String defaultCascade = root.getAttribute(Attributes.ATTR_DEFAULT_CASCADE);
        final List<Element> classElements = DomUtils.getChildrenByTag(root, tagName);
        for (final Element classElement : classElements) {
            final JpaEntity jpaEntity = parseEntity(classElement, defaultCascade);

            // If package was mapped in HBM and entity class name has no full name, include the package on it
            if (StringUtils.isNotBlank(packageName) &&
                    StringUtils.equals(jpaEntity.getName(), ClassNameUtils.getSimpleClassName(jpaEntity.getName()))) {
                jpaEntity.setName(packageName + "." + ClassNameUtils.getSimpleClassName(jpaEntity.getName()));
            }
            entities.add(jpaEntity);
            parseClasses(classElement, entities, packageName);
        }
    }

    private JpaEntity parseEntity(final Element classElement, final String defaultCascade) {
        return parseEntity(null, classElement, defaultCascade);
    }

    private JpaEntity parseEntity(JpaEntity entityDef, final Element classElement, final String defaultCascade) {
        if (entityDef == null) {
            entityDef = new JpaEntity();
        }

        entityDef.setDefaultCascade(defaultCascade);
        entityDef.setName(classElement.getAttribute(Attributes.ATTR_NAME));
        entityDef.setTable(classElement.getAttribute(Attributes.ATTR_TABLE));
        entityDef.setDynamicInsert(classElement.getAttribute(Attributes.ATTR_DYNAMIC_INSERT));
        entityDef.setDynamicUpdate(classElement.getAttribute(Attributes.ATTR_DYNAMIC_UPDATE));
        entityDef.setAbstractClass(classElement.getAttribute(Attributes.ATTR_ABSTRACT));
        entityDef.setMutable(classElement.getAttribute(Attributes.ATTR_MUTABLE));
        entityDef.setLazy(classElement.getAttribute(Attributes.ATTR_LAZY));

        // Only for subclasses
        entityDef.setParentClass(classElement.getAttribute(Attributes.ATTR_EXTENDS));
        entityDef.getDiscriminator().setValue(classElement.getAttribute(Attributes.ATTR_DISCRIMINATOR_VALUE));

        final Element cacheElement = DomUtils.getFirstChildByTag(classElement, Tags.TAG_CACHE);
        if (cacheElement != null) {
            entityDef.setCacheUsage(cacheElement.getAttribute(Attributes.ATTR_USAGE));
        }

        final Element joinElement = DomUtils.getFirstChildByTag(classElement, Tags.TAG_JOIN);
        if (joinElement != null) {
            entityDef.setTable(joinElement.getAttribute(Attributes.ATTR_TABLE));
            entityDef.setSecondTable(true);

            final JpaColumn keyColumn = parseKey(joinElement, null);
            if (keyColumn != null) {
                entityDef.setSecondTableKeys(keyColumn);
            }

            parseEntity(entityDef, joinElement, defaultCascade);
        }

        parseDiscriminator(classElement, entityDef);

        parseIdAndGenerator(classElement, entityDef);

        parseNaturalIds(classElement, entityDef);

        parseVersions(classElement, entityDef);

        parseProperties(classElement, entityDef);

        parsePropertyList(classElement, entityDef);

        parseRelationships(classElement, entityDef);

        parseCollections(classElement, entityDef);

        parseComponents(classElement, entityDef);

        parseQueries(classElement, entityDef);

        return entityDef;
    }

    private void parseDiscriminator(final Element element, final JpaEntity entityDef) {
        final Element discriminatorElement = DomUtils.getFirstChildByTag(element, Tags.TAG_DISCRIMINATOR);
        if (discriminatorElement != null) {
            final JpaDiscriminator jpaDiscriminator = entityDef.getDiscriminator();
            jpaDiscriminator.setType(discriminatorElement.getAttribute(Attributes.ATTR_TYPE));

            final Element columnElement = DomUtils.getFirstChildByTag(discriminatorElement, Tags.TAG_COLUMN);
            if (columnElement != null) {
                jpaDiscriminator.setColumn(columnElement.getAttribute(Attributes.ATTR_NAME));
                jpaDiscriminator.setLength(columnElement.getAttribute(Attributes.ATTR_LENGTH));
            }
        }
    }

    private void parseIdAndGenerator(final Element element, final JpaEntity entityDef) {
        final Element idElement = DomUtils.getFirstChildByTag(element, Tags.TAG_ID);
        if (idElement != null) {
            final JpaPrimaryKey primaryKey = new JpaPrimaryKey();

            primaryKey.setName(idElement.getAttribute(Attributes.ATTR_NAME));
            primaryKey.setColumnName(idElement.getAttribute(Attributes.ATTR_COLUMN));
            primaryKey.setType(HibernateUtils.mapHibernateTypeToJava(idElement.getAttribute(Attributes.ATTR_TYPE)));

            final Element columnElement = DomUtils.getFirstChildByTag(idElement, Tags.TAG_COLUMN);
            if (columnElement != null) {
                primaryKey.setColumnName(columnElement.getAttribute(Attributes.ATTR_NAME));
            }

            // Handle the generator
            final Element generatorElement = DomUtils.getFirstChildByTag(idElement, Tags.TAG_GENERATOR);
            if (generatorElement != null) {
                final String generatorClass = generatorElement.getAttribute(Attributes.ATTR_CLASS);

                // Collect all params
                final List<Element> paramElements = DomUtils.getChildrenByTag(generatorElement, Tags.TAG_PARAM);
                for (final Element paramElement : paramElements) {
                    final String paramName = paramElement.getAttribute(Attributes.ATTR_NAME);
                    final String paramValue = paramElement.getTextContent().trim();
                    primaryKey.getGeneratorParams().put(paramName, paramValue);
                }

                // Map generator to JPA annotations
                switch (generatorClass) {
                    case "sequence":
                    case "seqhilo":
                    case "identity":
                    case "foreign":
                        primaryKey.setGeneratorType(generatorClass.toUpperCase());
                        break;

                    case "increment":
                    case "native":
                        primaryKey.setGeneratorType("AUTO");
                        break;

                    default:
                        // Default to a general generator if not a common type
                        primaryKey.setGeneratorType("GENERATOR");
                        primaryKey.getGeneratorParams().put(JpaPrimaryKey.PARAMS_SEQUENCE, generatorClass);
                        break;
                }
            }

            // Add the ID attribute
            entityDef.setPrimaryKey(primaryKey);
        }
    }

    private void parseVersions(final Element element, final JpaEntity entityDef) {
        final List<Element> versionElements = DomUtils.getChildrenByTag(element, Tags.TAG_VERSION);
        for(final Element versionElement : versionElements) {
            final List<JpaColumn> jpaColumns = parseColumns(versionElement, null);
            for (final JpaColumn jpaColumn : jpaColumns) {
                jpaColumn.setVersion(true);
                entityDef.addColumn(jpaColumn);
            }
        }
    }

    private void parseProperties(final Element element, final JpaEntity entityDef) {
        final List<Element> propertiesElements = DomUtils.getChildrenByTag(element, Tags.TAG_PROPERTIES);
        for (final Element propertiesElement : propertiesElements) {
            final boolean unique = Boolean.parseBoolean(propertiesElement.getAttribute(Attributes.ATTR_UNIQUE));
            final String uniqueConstraintName = unique ? propertiesElement.getAttribute(Attributes.ATTR_NAME) : "";

            parsePropertyList(propertiesElement, entityDef, uniqueConstraintName);

            parseRelationships(propertiesElement, entityDef, uniqueConstraintName);
        }
    }

    private void parsePropertyList(final Element element, final JpaEntity entityDef, final String uniqueConstraint) {
        parsePropertyList(element, entityDef, uniqueConstraint, JpaColumn.NaturalId.NONE);
    }

    private void parsePropertyList(final Element element, final JpaEntity entityDef, final JpaColumn.NaturalId naturalId) {
        parsePropertyList(element, entityDef, null, naturalId);
    }

    private void parsePropertyList(final Element element, final JpaEntity entityDef) {
        parsePropertyList(element, entityDef, null, JpaColumn.NaturalId.NONE);
    }

    private void parsePropertyList(final Element element, final JpaEntity entityDef,
            final String uniqueConstraint, final JpaColumn.NaturalId naturalId) {
        final List<Element> propertyElements = DomUtils.getChildrenByTag(element, Tags.TAG_PROPERTY);
        for (final Element propertyElement : propertyElements) {
            final List<JpaColumn> jpaColumns = parseColumns(propertyElement, uniqueConstraint);
            if (jpaColumns.isEmpty()) {
                final JpaColumn jpaColumn = parseProperty(propertyElement, uniqueConstraint);
                jpaColumns.add(jpaColumn);
            }

            for (final JpaColumn jpaColumn : jpaColumns) {
                jpaColumn.setNaturalId(naturalId);
            }

            if (jpaColumns.size() == 1) {
                entityDef.addColumn(jpaColumns.get(0));
            }
            // Composite column
            if (jpaColumns.size() > 1) {
                final JpaCompositeColumn compositeColumn = new JpaCompositeColumn();
                compositeColumn.setType(propertyElement.getAttribute(Attributes.ATTR_TYPE));
                compositeColumn.setName(propertyElement.getAttribute(Attributes.ATTR_NAME));
                jpaColumns.forEach(compositeColumn::addColumn);
                entityDef.addCompositeColumn(compositeColumn);
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

        final Element typeElement = DomUtils.getFirstChildByTag(parentElement, Tags.TAG_TYPE);
        if (typeElement != null) {
            jpaColumn.setType(typeElement.getAttribute(Attributes.ATTR_NAME));

            final List<Element> typeParams = DomUtils.getChildrenByTag(typeElement, Tags.TAG_PARAM);
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

        final List<Element> columns = DomUtils.getChildrenByTag(parentElement, Tags.TAG_COLUMN);
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
        final List<Element> naturalIdElements = DomUtils.getChildrenByTag(element, Tags.TAG_NATURAL_ID);
        for (final Element naturalIdElement : naturalIdElements) {
            final String mutable = naturalIdElement.getAttribute(Attributes.ATTR_MUTABLE);
            final JpaColumn.NaturalId naturalId = "true".equals(mutable) ?
                    JpaColumn.NaturalId.MUTABLE : JpaColumn.NaturalId.IMMUTABLE;
            parsePropertyList(naturalIdElement, entityDef, naturalId);
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
        final List<Element> manyToOneElements = DomUtils.getChildrenByTag(element, Tags.TAG_MANY_TO_ONE);
        for (final Element relationshipElement : manyToOneElements) {
            final JpaRelationship relationship = new JpaRelationship();
            relationship.setRelationshipType(JpaRelationship.Type.ManyToOne);
            relationship.setFetch("eager");
            parseRelationship(relationship, relationshipElement, entityDef, uniqueConstraintName);
        }

        final List<Element> oneToOneElements = DomUtils.getChildrenByTag(element, Tags.TAG_ONE_TO_ONE);
        for (final Element relationshipElement : oneToOneElements) {
            final JpaRelationship relationship = new JpaRelationship();
            relationship.setRelationshipType(JpaRelationship.Type.OneToOne);
            relationship.setFetch("eager");
            parseRelationship(relationship, relationshipElement, entityDef, uniqueConstraintName);
        }

        final List<Element> oneToManyElements = DomUtils.getChildrenByTag(element, Tags.TAG_ONE_TO_MANY);
        for (final Element relationshipElement : oneToManyElements) {
            collectionRelationship.setRelationshipType(JpaRelationship.Type.OneToMany);
            parseRelationship(collectionRelationship, relationshipElement, entityDef, uniqueConstraintName);
        }

        final List<Element> manyToManyElements = DomUtils.getChildrenByTag(element, Tags.TAG_MANY_TO_MANY);
        for (final Element relationshipElement : manyToManyElements) {
            collectionRelationship.setRelationshipType(JpaRelationship.Type.ManyToMany);
            parseRelationship(collectionRelationship, relationshipElement, entityDef, uniqueConstraintName);
        }
    }

    private void parseRelationship(final JpaRelationship relationship, final Element relationshipElement,
            final JpaEntity entityDef, final String uniqueConstraintName) {
        final String name = relationshipElement.getAttribute(Attributes.ATTR_NAME);
        final String type = relationshipElement.getAttribute(Attributes.ATTR_CLASS);
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
        relationship.setType(type);

        if (StringUtils.isNotBlank(relationshipElement.getAttribute(Attributes.ATTR_LAZY))) {
            relationship.setFetch("false".equals(relationshipElement.getAttribute(Attributes.ATTR_LAZY)) ? "eager" : "lazy");
        } else if (StringUtils.isNotBlank(relationshipElement.getAttribute(Attributes.ATTR_FETCH))) {
            relationship.setFetch("join".equals(relationshipElement.getAttribute(Attributes.ATTR_FETCH)) ? "eager" : "lazy");
        }
        relationship.setCascade(cascade, entityDef.getDefaultCascade());
        relationship.setAccess(access);
        relationship.setOptional(optional);
        relationship.setMappedBy(mappedBy);

        final List<JpaColumn> jpaColumns = parseColumns(relationshipElement, null);
        if (!jpaColumns.isEmpty()) {
            for (final JpaColumn jpaColumn : jpaColumns) {
                if (StringUtils.isNotBlank(update)) {
                    jpaColumn.setUpdatable(Boolean.parseBoolean(update));
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
        List<Element> collectionElements = DomUtils.getChildrenByTag(element, Tags.TAG_SET);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_SET);
        }

        collectionElements = DomUtils.getChildrenByTag(element, Tags.TAG_LIST);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_LIST);
        }

        collectionElements = DomUtils.getChildrenByTag(element, Tags.TAG_BAG);
        for (final Element collectionElement : collectionElements) {
            parseCollection(collectionElement, entityDef, Tags.TAG_BAG);
        }

        collectionElements = DomUtils.getChildrenByTag(element, Tags.TAG_MAP);
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
        } else if (StringUtils.isNotBlank(collectionElement.getAttribute(Attributes.ATTR_FETCH))) {
            relationship.setFetch("join".equals(collectionElement.getAttribute(Attributes.ATTR_FETCH)) ? "eager" : "lazy");
        }

        final JpaColumn keyColumn = parseKey(collectionElement, relationship.getName());
        if (keyColumn != null) {
            relationship.addReferencedColumn(keyColumn);
        }

        final Element mapKeyElement = DomUtils.getFirstChildByTag(collectionElement, Tags.TAG_MAP_KEY);
        if (mapKeyElement != null) {
            final JpaColumn mapKeyColumn = relationship.getReferencedColumns().get(0);
            mapKeyColumn.setType(HibernateUtils.mapHibernateTypeToJava(mapKeyElement.getAttribute(Attributes.ATTR_TYPE)));
            mapKeyColumn.setName(mapKeyElement.getAttribute(Attributes.ATTR_COLUMN));
        }

        final Element compositeMapkeyElement = DomUtils.getFirstChildByTag(collectionElement, Tags.TAG_COMPOSITE_MAP_KEY);
        if (compositeMapkeyElement != null) {
            final List<Element> keyProperties = DomUtils.getChildrenByTag(compositeMapkeyElement, Tags.TAG_KEY_PROPERTY);
            for (final Element keyProperty : keyProperties) {
                relationship.setCompositeMapKey(compositeMapkeyElement.getAttribute(Attributes.ATTR_CLASS));

                final JpaColumn compositeMapKeyColumn = new JpaColumn();
                compositeMapKeyColumn.setEmbedded(true);
                compositeMapKeyColumn.setType(keyProperty.getAttribute(Attributes.ATTR_TYPE));
                compositeMapKeyColumn.setName(keyProperty.getAttribute(Attributes.ATTR_NAME));
                compositeMapKeyColumn.setColumnName(keyProperty.getAttribute(Attributes.ATTR_COLUMN));
                relationship.addReferencedColumn(compositeMapKeyColumn);
            }
        }

        relationship.setOrderBy(collectionElement.getAttribute(Attributes.ATTR_ORDER_BY));

        final Element indexElement = DomUtils.getFirstChildByTag(collectionElement, Tags.TAG_LIST_INDEX);
        if (indexElement != null) {
            relationship.setListIndex(indexElement.getAttribute(Attributes.ATTR_COLUMN));
        }
    }

    private JpaColumn parseKey(final Element parentElement, final String name) {
        final Element keyElement = DomUtils.getFirstChildByTag(parentElement, Tags.TAG_KEY);
        if (keyElement != null) {
            JpaColumn keyColumn = null;

            if (StringUtils.isNotBlank(keyElement.getAttribute(Attributes.ATTR_COLUMN))) {
                keyColumn = new JpaColumn();
                keyColumn.setName(name);
                keyColumn.setColumnName(keyElement.getAttribute(Attributes.ATTR_COLUMN));
            } else {
                final List<JpaColumn> jpaColumns = parseColumns(keyElement, null);
                if (!jpaColumns.isEmpty()) {
                    keyColumn = jpaColumns.get(0);
                }
            }
            if (keyColumn != null &&
                    StringUtils.isNotBlank(keyElement.getAttribute(Attributes.ATTR_FOREIGN_KEY))) {
                keyColumn.setForeignKey(keyElement.getAttribute(Attributes.ATTR_FOREIGN_KEY));
            }
            return keyColumn;
        }
        return null;
    }

    private void parseComponents(final Element element, final JpaEntity entityDef) {
        final List<Element> componentElements = DomUtils.getChildrenByTag(element, Tags.TAG_COMPONENT);
        for (final Element componentElement : componentElements) {
            final JpaColumn embeddedColumn = new JpaColumn();
            embeddedColumn.setEmbedded(true);
            embeddedColumn.setName(componentElement.getAttribute(Attributes.ATTR_NAME));
            embeddedColumn.setType(componentElement.getAttribute(Attributes.ATTR_CLASS));
            entityDef.addColumn(embeddedColumn);

            final JpaEntity embeddedEntity = new JpaEntity();
            embeddedEntity.setName(componentElement.getAttribute(Attributes.ATTR_CLASS));
            embeddedEntity.setEmbeddable(true);
            parsePropertyList(componentElement, embeddedEntity);

            entityDef.addEmbeddedEntity(embeddedEntity);
        }
    }

    private void parseQueries(final Element root, final JpaEntity jpaEntity) {
        final List<Element> queries = DomUtils.getChildrenByTag(root, Tags.TAG_QUERY);
        for (final Element query : queries) {
            final JpaNamedQuery namedQuery = new JpaNamedQuery();
            namedQuery.setName(query.getAttribute(Attributes.ATTR_NAME));
            namedQuery.setQuery(query.getTextContent().trim());
            jpaEntity.addNamedQuery(namedQuery);
        }

        final List<Element> sqlQueries = DomUtils.getChildrenByTag(root, Tags.TAG_SQL_QUERY);
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
        final List<Element> returnScalarElements = DomUtils.getChildrenByTag(element, Tags.TAG_RETURN_SCALAR);
        for (final Element returnScalarElement : returnScalarElements) {
            final JpaColumn returnColumn = new JpaColumn();
            returnColumn.setColumnName(returnScalarElement.getAttribute(Attributes.ATTR_COLUMN));
            returnColumn.setType(returnScalarElement.getAttribute(Attributes.ATTR_TYPE));
            namedQuery.addReturnColumn(returnColumn);
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

        final List<Element> children = DomUtils.getChildrenByTag(parent, null);
        for (final Element child : children) {
            checkMissingTagAttributeImplementations(child);
        }
    }

}
