package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Build-profile contract tests for community and pro/enterprise module
 * boundaries.
 */
class WayangEditionBuildProfilesTest {

    private static final String PRO_ENTERPRISE_ADDONS = "pro-enterprise-addons";

    @Test
    void communityReactorDoesNotIncludeA2uiAddonByDefault() {
        Document pom = parentPom();

        assertThat(defaultModules(pom))
                .contains("a2a", "agentic-commerce", "wayang-gollek-sdk")
                .doesNotContain("a2ui");
    }

    @Test
    void proEnterpriseProfilePublishesA2uiAddonModule() {
        Document pom = parentPom();

        assertThat(profileModules(pom, PRO_ENTERPRISE_ADDONS))
                .containsExactly("a2ui");
    }

    private static Document parentPom() {
        Path pom = Path.of(System.getProperty("user.dir")).resolve("../pom.xml").normalize();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(pom.toFile());
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Unable to read Wayang parent POM at " + pom, e);
        }
    }

    private static List<String> defaultModules(Document document) {
        return modules(project(document));
    }

    private static List<String> profileModules(Document document, String profileId) {
        for (Element profiles : children(project(document), "profiles")) {
            for (Element profile : children(profiles, "profile")) {
                if (profileId.equals(text(firstChild(profile, "id")))) {
                    return modules(profile);
                }
            }
        }
        return List.of();
    }

    private static List<String> modules(Element parent) {
        Element modules = firstChild(parent, "modules");
        if (modules == null) {
            return List.of();
        }
        return children(modules, "module").stream()
                .map(WayangEditionBuildProfilesTest::text)
                .toList();
    }

    private static Element project(Document document) {
        return document.getDocumentElement();
    }

    private static Element firstChild(Element parent, String name) {
        List<Element> matches = children(parent, name);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static List<Element> children(Element parent, String name) {
        if (parent == null) {
            return List.of();
        }
        List<Element> values = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getTagName())) {
                values.add(element);
            }
        }
        return values;
    }

    private static String text(Element element) {
        return element == null ? "" : element.getTextContent().trim();
    }
}
