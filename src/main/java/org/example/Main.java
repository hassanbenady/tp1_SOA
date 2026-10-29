import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * TP1 - Architecture SOA et services web.
 * Fonctionnalite 1 : validation XML / XSD (SchemaFactory + Validator)
 * Fonctionnalite 2 : interrogation XPath (XPathFactory + XPathExpression)
 */
public class Main {

    private static final String XSD = "/cinema.xsd";
    private static final String XML_VALIDE = "/cinema.xml";
    private static final String XML_INVALIDE = "/cinema-invalide.xml";

    public static void main(String[] args) throws Exception {
        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8")); // accents corrects dans la console
        System.out.println("=== 1. VALIDATION XML / XSD ===");
        validerXml(XML_VALIDE, XSD);
        validerXml(XML_INVALIDE, XSD);

        System.out.println("\n=== 2. INTERROGATION XPATH ===");
        Document doc = chargerDocument(XML_VALIDE);
        XPath xpath = XPathFactory.newInstance().newXPath();

        afficher("Tous les titres de films", "//film/titre", doc, xpath);
        afficher("Films actuellement en salle", "//film[enSalle='true']/titre", doc, xpath);
        afficher("Films de science-fiction", "//film[@genre='SCIENCE_FICTION']/titre", doc, xpath);
        afficher("Films avec une note >= 8.8", "//film[note >= 8.8]/titre", doc, xpath);
        afficher("Films sortis apres 2010 (realisateur)", "//film[annee > 2010]/realisateur", doc, xpath);
        afficher("Seances a moins de 13 DT (heure)", "//seance[prix < 13]/heure", doc, xpath);
        afficher("Titre du film de la seance se3", "id(//seance[@id='se3']/@filmRef)/titre", doc, xpath);

        afficher("Spectateurs actifs", "//spectateur[@actif='true']/nom", doc, xpath);
        afficher("Spectateurs de moins de 25 ans", "//spectateur[age < 25]/nom", doc, xpath);
        afficher("Spectateurs ayant reserve la seance se1", "id(//reservation[@seanceRef='se1']/@spectateurRef)/nom", doc, xpath);

        Double placesSe1 = (Double) xpath.compile("sum(//reservation[@seanceRef='se1']/nbPlaces)")
                .evaluate(doc, XPathConstants.NUMBER);
        System.out.println("\n[Audience de la seance se1] " + placesSe1.intValue() + " places reservees");

        Double placesTotal = (Double) xpath.compile("sum(//reservation/nbPlaces)")
                .evaluate(doc, XPathConstants.NUMBER);
        System.out.println("[Audience totale] " + placesTotal.intValue() + " places reservees");

        Double capaciteTotale = (Double) xpath.compile("sum(//salle/capacite)")
                .evaluate(doc, XPathConstants.NUMBER);
        System.out.println("\n[Capacite totale des salles] " + capaciteTotale.intValue() + " places");

        Double nbFilms = (Double) xpath.compile("count(//film)").evaluate(doc, XPathConstants.NUMBER);
        System.out.println("[Nombre de films] " + nbFilms.intValue());
    }

    // ---------- Fonctionnalite 1 : validation ----------

    /** Valide un fichier XML (ressource du classpath) contre un schema XSD. */
    public static boolean validerXml(String xmlRes, String xsdRes) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream xsd = Main.class.getResourceAsStream(xsdRes);
             InputStream xml = Main.class.getResourceAsStream(xmlRes)) {
            Schema schema = factory.newSchema(new StreamSource(xsd));
            Validator validator = schema.newValidator();
            final List<String> erreurs = new ArrayList<>();
            validator.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override public void error(SAXParseException e) {
                    erreurs.add("ligne " + e.getLineNumber() + " : " + e.getMessage());
                }
                @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });
            validator.validate(new StreamSource(xml));
            if (erreurs.isEmpty()) {
                System.out.println("[OK]  " + xmlRes + " est VALIDE par rapport a " + xsdRes);
                return true;
            }
            System.out.println("[KO]  " + xmlRes + " est INVALIDE (" + erreurs.size() + " erreur(s)) :");
            erreurs.forEach(e -> System.out.println("      - " + e));
            return false;
        } catch (SAXException e) {
            System.out.println("[KO]  " + xmlRes + " : " + e.getMessage());
            return false;
        }
    }

    // ---------- Fonctionnalite 2 : XPath ----------

    private static Document chargerDocument(String xmlRes) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // DTD/ID : le schema doit etre associe pour que id() fonctionne (attributs de type ID)
        dbf.setNamespaceAware(true);
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream xsd = Main.class.getResourceAsStream(XSD);
             InputStream xml = Main.class.getResourceAsStream(xmlRes)) {
            dbf.setSchema(sf.newSchema(new StreamSource(xsd)));
            return dbf.newDocumentBuilder().parse(xml);
        }
    }

    /** Compile et evalue une expression XPath retournant un ensemble de noeuds. */
    public static List<String> requete(String expression, Document doc, XPath xpath) throws Exception {
        XPathExpression expr = xpath.compile(expression);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        List<String> res = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            res.add(n.getTextContent().trim());
        }
        return res;
    }

    private static void afficher(String titre, String expression, Document doc, XPath xpath) throws Exception {
        System.out.println("\n[" + titre + "]  " + expression);
        for (String s : requete(expression, doc, xpath)) System.out.println("   - " + s);
    }
}