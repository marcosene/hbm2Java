package com.devtools.utils;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public interface DomUtils {

    static List<Element> getChildrenByTag(final Element parentElement, final String tagName) {
        final List<Element> matchingChildren = new ArrayList<>();

        // Get all direct children of the parent element
        final NodeList children = parentElement.getChildNodes();

        // Iterate through the NodeList
        for (int i = 0; i < children.getLength(); i++) {
            final Node childNode = children.item(i);

            // Check if the child node is an Element node (Node.ELEMENT_NODE)
            // This filters out text nodes, comments, processing instructions, etc.
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                final Element childElement = (Element) childNode; // Cast to Element

                // Check if the element has the desired tag name
                if (tagName == null || childElement.getTagName().equals(tagName)) {
                    matchingChildren.add(childElement);
                }
            }
        }

        return matchingChildren;
    }

    static Element getFirstChildByTag(final Element parentElement, final String tagName) {
        final List<Element> elements = getChildrenByTag(parentElement, tagName);
        if (elements.isEmpty()) {
            return null;
        }
        return elements.get(0);
    }
}
