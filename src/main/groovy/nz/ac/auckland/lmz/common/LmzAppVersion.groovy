package nz.ac.auckland.lmz.common

import com.bluetrainsoftware.classpathscanner.ClasspathScanner
import net.stickycode.stereotype.configured.Configured;
import net.stickycode.stereotype.configured.PostConfigured;
import nz.ac.auckland.common.config.ConfigKey;
import nz.ac.auckland.common.stereotypes.UniversityComponent
import nz.ac.auckland.lmz.flags.Flags
import org.slf4j.Logger
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Central place to hold and find the version of the application that is running. This comes from the manifest file,
 * unless we are in dev mode in which case it will come from the POM in the classpath that holds the test classes
 *
 * @author: Richard Vowles - https://plus.google.com/+RichardVowles
 */
@UniversityComponent
public class LmzAppVersion implements AppVersion {
	private static Logger log = LoggerFactory.getLogger(LmzAppVersion.class)

	/**
	 * @see nz.ac.auckland.common.config.JarManifestConfigurationSource#KEY_IMPLEMENTATION_VERSION
	 */
	public static final String DEFAULT = "unknown"

	@ConfigKey("Implementation-Version")
	protected String version = DEFAULT;

	private boolean initialized = false;

	@Configured
	// force @postconfigured
	String meh = "meh.";

	public static final Map<String, GroupArtifactVersion> classpathGavs = new HashMap<>();

	public static class GroupArtifactVersion {
		public String groupId;
		public String artfiactId;
		public String version;
	}

	private static Element findChildElement(Node node, String element) {
		for(Node child = node.getFirstChild(); child != null;) {
			if (child.getNodeName().equals(element))
				return (Element)child;

			child = child.getNextSibling();
		}

		return null;
	}

	private static Element findChild(Node node, String element) {
		Element el = findChildElement(node, element);
		// did we find it under "project"? if so, return it
		if (el != null) return el;

		// it wasn't specified which means its inherited from the parent, so go get that one
		Element parent = findChild(node, "parent");

		return (parent != null) ? findChildElement(parent, element) : null;
	}

	public static String parseGAVfromPOM(File projDir) {
		if (projDir.getName().equals("pom.xml"))
			projDir = projDir.getParentFile();

		File pom = new File(projDir, "pom.xml");

		if (!pom.exists())
			throw new RuntimeException("Unable to find pom.xml in " + projDir.getAbsolutePath());

		Document doc = null;

		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		} catch (SAXException|IOException|ParserConfigurationException e) {
			throw new RuntimeException("Cannot parse  " + projDir.getAbsolutePath(), e);
		}

		doc.getDocumentElement().normalize(); // apparently recommended
		NodeList projectNode = doc.getElementsByTagName("project");

		if (projectNode.getLength() < 1)
			throw new RuntimeException("Improperty formed pom.xml file in " + projDir.getAbsolutePath());

		GroupArtifactVersion gav = new GroupArtifactVersion();

		Node project = projectNode.item(0);

		gav.groupId = findChild(project, "groupId").getTextContent();
		gav.artfiactId = findChild(project, "artifactId").getTextContent();
		gav.version = findChild(project, "version").getTextContent();

		log.info("loaded pom: " + gav.groupId + ":" + gav.artfiactId + ":" + gav.version + " as key: " + projDir.getAbsolutePath() );

		classpathGavs.put(projDir.getAbsolutePath(), gav);

		return gav.version
	}

	@PostConfigured
	public void configured() {
		if (initialized) {
			return
		}

		initialized = true;

		if (DEFAULT.equals(version) && Flags.DEVMODE.on()) {
			File basePath = ClasspathScanner.findTestClassesBasePath();

			if (basePath) {
				version = parseGAVfromPOM(basePath)
			}
		}

		if (version.endsWith('-SNAPSHOT')) {
			version = version.replace('-SNAPSHOT', '-' + Long.toHexString(System.currentTimeMillis()))
		}
	}

	public String getVersion() {
		configured() // ensure we are configured in case we are getting called in a postconfigure from someone else

		return version
	}
}
