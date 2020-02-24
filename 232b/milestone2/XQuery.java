import java.io.*;
import java.util.ArrayList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XQuery {
    public static void main(String[] args) {
        String queryFile = args[0];
        String resultPath = "./output.xml";
        ANTLRInputStream input = null;
        try {
            InputStream is = new FileInputStream(queryFile);
            input = new ANTLRInputStream(is);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        XQueryParser parser = new XQueryParser(new CommonTokenStream(new XQueryLexer(input)));
        ParseTree tree = parser.xq();
        XQueryMyVisitor evaluation = new XQueryMyVisitor();
        ArrayList<Node> finalResult = evaluation.visit(tree);
        createResultFile(evaluation.outputDocument, finalResult, resultPath);

        System.out.println("Saving output.xml");
        if (!finalResult.isEmpty()){
            System.out.println("finalResult size: " + finalResult.size());
        }
    }

    public static void createResultFile(Document doc, ArrayList<Node> finalResult, String resultPath) {
        if(finalResult.size() > 1) {
            ArrayList<Node> MyResult = makeElem(doc, "myresult", finalResult);
            doc.appendChild(MyResult.get(0));
        }
        else
            doc.appendChild(finalResult.get(0));
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult res = new StreamResult(resultPath);
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