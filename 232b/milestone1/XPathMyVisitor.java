import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XPathMyVisitor extends XPathBaseVisitor<ArrayList<Node>> {

    private ArrayList<Node> curr = new ArrayList<>();
    private boolean hasAttribute = false;
    Document doc = null;

    @Override
    public ArrayList<Node> visitApChildren(XPathParser.ApChildrenContext ctx) {
        return visitChildren(ctx);
    }

    private static ArrayList<Node> children(Node n){
        ArrayList<Node> childrenList = new ArrayList<Node>();
        for(int i = 0; i < n.getChildNodes().getLength(); i++){
            childrenList.add(n.getChildNodes().item(i));
        }
        return childrenList;
    }

    @Override
    public ArrayList<Node> visitApDescendant(XPathParser.ApDescendantContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        LinkedList<Node> ll = new LinkedList<>();
        visit(ctx.doc());
        res.addAll(curr);
        ll.addAll(curr);
        while(!ll.isEmpty()) {
            Node temp = ll.poll();
            res.addAll(children(temp));
            ll.addAll(children(temp));
        }
        curr = res;
        return visit(ctx.rp());
    }

    @Override
    public ArrayList<Node> visitDoc(XPathParser.DocContext ctx) {
        File xmlFile = new File(ctx.fileName().getText());
        DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
        DocumentBuilder docB = null;
        try {
            docB = docBF.newDocumentBuilder();
        } catch (ParserConfigurationException pE1) {
            pE1.printStackTrace();
        }

        try {
            if (docB != null) {
                doc = docB.parse(xmlFile);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        if (doc != null) {
            doc.getDocumentElement().normalize();
        }
        ArrayList<Node> res = new ArrayList<>();
        res.add(doc);
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitAllChildren(XPathParser.AllChildrenContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        for(Node temp : curr) {
            res.addAll(children(temp));
        }
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitRpwithParentheses(XPathParser.RpwithParenthesesContext ctx) {
        return visit(ctx.rp());
    }

    @Override
    public ArrayList<Node> visitTag(XPathParser.TagContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        String tName = ctx.getText();
        for(Node temp : curr) {
            ArrayList<Node> nodeList = children(temp);
            for(Node i : nodeList) {
                if(i.getNodeName().equals(tName)) res.add(i);
            }
        }
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitRpDescendant(XPathParser.RpDescendantContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        LinkedList<Node> ll = new LinkedList<>();
        visit(ctx.rp(0));
        res.addAll(curr);
        ll.addAll(curr);
        while(!ll.isEmpty()) {
            Node temp = ll.poll();
            res.addAll(children(temp));
            ll.addAll(children(temp));
        }
        curr = res;
        return visit(ctx.rp(1));
    }

    @Override
    public ArrayList<Node> visitParent(XPathParser.ParentContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        for(Node temp : curr) {
            if(!res.contains(temp.getParentNode())) res.add(temp.getParentNode());
        }
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitAttribute(XPathParser.AttributeContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        hasAttribute = true;
        for (Node temp : curr) {
            Element e = (Element) temp;
            String attr = e.getAttribute(ctx.NAME().getText());
            if (!attr.equals("")) {
                res.add(temp);
            }
        }
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitRpChildren(XPathParser.RpChildrenContext ctx) {
        visit(ctx.rp(0));
        ArrayList<Node> res = visit(ctx.rp(1));
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitText(XPathParser.TextContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        for (Node temp :curr) {
            for (int i = 0; i < temp.getChildNodes().getLength(); i++) {
                if (temp.getChildNodes().item(i).getNodeType() == javax.xml.soap.Node.TEXT_NODE && !temp.getChildNodes().item(i).getTextContent().equals("\n")) {
                    res.add(temp.getChildNodes().item(i));
                }
            }
        }
        return res;
    }

    @Override
    public ArrayList<Node> visitCurrent(XPathParser.CurrentContext ctx) {
        return curr;
    }

    @Override
    public ArrayList<Node> visitRpConcatenation(XPathParser.RpConcatenationContext ctx) {
        ArrayList<Node> res1 = new ArrayList<>();
        ArrayList<Node> res2 = new ArrayList<>();
        ArrayList<Node> tempList = new ArrayList<>(curr);
        res1.addAll(visit(ctx.rp(0)));
        curr = tempList;
        res2.addAll(visit(ctx.rp(1)));
        res1.addAll(res2);

        curr = res1;
        return res1;
    }

    @Override
    public ArrayList<Node> visitRpFilter(XPathParser.RpFilterContext ctx) {
        curr = visit(ctx.rp());
        curr = visit(ctx.filter());
        return curr;
    }

    @Override
    public ArrayList<Node> visitFilterAnd(XPathParser.FilterAndContext ctx) {
        HashSet<Node> left = new HashSet<Node>(visit(ctx.filter(0)));
        HashSet<Node> right = new HashSet<Node>(visit(ctx.filter(1)));
        HashSet<Node> intersection = new HashSet<>();
        intersection.addAll(left);
        intersection.retainAll(right);
        ArrayList<Node> res = new ArrayList<>(intersection);
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitFilterEqual(XPathParser.FilterEqualContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        ArrayList<Node> cpy = curr;
        for (Node tmp : cpy) {
            ArrayList<Node> singlenode = new ArrayList<>();
            singlenode.add(tmp);
            curr = singlenode;
            ArrayList<Node> left = visit(ctx.rp(0));
            curr = singlenode;
            ArrayList<Node> right = visit(ctx.rp(1));
            for (Node i : left) {
                for (Node j : right) {
                    if (i.isEqualNode(j)) {
                        res.add(tmp);
                    }
                }
            }
        }
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitFilterNot(XPathParser.FilterNotContext ctx) {
        HashSet<Node> leftSet = new HashSet<Node>(curr);
        HashSet<Node> rightSet = new HashSet<Node>(visit(ctx.filter()));
        HashSet<Node> difference = new HashSet<Node>();
        difference.addAll(leftSet);
        difference.removeAll(rightSet);

        ArrayList<Node> res = new ArrayList<Node>(difference);
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitFilterOr(XPathParser.FilterOrContext ctx) {
        HashSet<Node> leftSet = new HashSet<Node>(visit(ctx.filter(0)));
        HashSet<Node> rightSet = new HashSet<Node>(visit(ctx.filter(1)));
        HashSet<Node> union = new HashSet<Node>();
        union.addAll(leftSet);
        union.addAll(rightSet);
        ArrayList<Node> res = new ArrayList<Node>(union);
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitFilterIs(XPathParser.FilterIsContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        ArrayList<Node> cpy = curr;
        for (Node tmp : cpy) {
            ArrayList<Node> singlenode = new ArrayList<>();
            singlenode.add(tmp);
            curr = singlenode;
            ArrayList<Node> left = visit(ctx.rp(0));
            curr = singlenode;
            ArrayList<Node> right = visit(ctx.rp(1));
            for (Node i : left) {
                for (Node j : right) {
                    if (i == j) {
                        res.add(tmp);
                    }
                }
            }
        }
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitFilterwithParentheses(XPathParser.FilterwithParenthesesContext ctx) {
        return visit(ctx.filter());
    }

    @Override
    public ArrayList<Node> visitFilterRp(XPathParser.FilterRpContext ctx) {
        ArrayList<Node> cpy = curr;
        ArrayList<Node> res = new ArrayList<>();
        for(Node n : curr){
            ArrayList<Node> tmp = new ArrayList<>();
            tmp.add(n);
            curr = tmp;
            ArrayList<Node> r = visit(ctx.rp());
            if(r.size() > 0)
                res.add(n);
        }
        curr = res;
        return res;
    }
}