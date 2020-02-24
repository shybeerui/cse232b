import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XPath {
    public static void main(String[] args) throws IOException {
        String inputFile = args[0];
        InputStream is = System.in;
        if (inputFile!=null) is = new FileInputStream(inputFile);
        ANTLRInputStream input = new ANTLRInputStream(is);
        XPathLexer lexer = new XPathLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        XPathParser parser = new XPathParser(tokens);
        ParseTree tree = parser.ap();
        XPathMyVisitor eval = new XPathMyVisitor();
        ArrayList<Node> result = eval.visit(tree);

        Document outputDoc = null;

        DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
        DocumentBuilder docB = null;
        try {
            docB = docBF.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        outputDoc = docB.newDocument();

        ArrayList<Node> finalResult = makeElem(eval.doc, "result", result);
        writeToFile(outputDoc, finalResult, "XPath1.xml");

        System.out.println("finalResult size: " + result.size());
//        for(Node n:finalResult) {
//            System.out.println(n.getNodeName());
//            System.out.println(n.getNodeValue());
//            System.out.println(n.getFirstChild().getNodeName());
//            System.out.println(n.getFirstChild().getNodeValue());
//            //System.out.println(n.getFirstChild().getFirstChild().getNodeValue());
//            //System.out.println(n.getNextSibling().getNextSibling().getNodeName());
//            System.out.println();
//        }
    }

    private static void writeToFile(Document doc, ArrayList<Node> result, String filePath) {
        Node newNode = doc.importNode(result.get(0), true);
        doc.appendChild(newNode);
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult res = new StreamResult(filePath);
            transformer.transform(source, res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<Node> makeElem(Document doc, String tag, ArrayList<Node> list){
        Node result = doc.createElement(tag);
        for (Node node : list) {
            if (node != null) {
                Node newNode = doc.importNode(node, true);
                result.appendChild(newNode);
            }
        }
        ArrayList<Node> results = new ArrayList<>();
        results.add(result);
        return results;
    }
}
