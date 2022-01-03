package heig.osmparser.utils.parsers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class SVGParser {

    public String toString(String svgFilename) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {

            String ret = "";

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(svgFilename));
            doc.getDocumentElement().normalize();

            NodeList paths = doc.getElementsByTagName("path");
            for (int i = 0; i < paths.getLength(); i++) {
                Node path = paths.item(i);
                Element element = (Element) path;
                String pathStr = element.getAttribute("d");
                ret += pathStr + " ";
            }
            return ret;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
