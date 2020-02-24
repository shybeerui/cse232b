import java.io.File;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XQueryMyVisitor extends XQueryBaseVisitor<ArrayList<Node>> {
    private boolean hasAttribute = false;
    private Document inputDocument = null;
    Document outputDocument = null;
    private HashMap<String, ArrayList<Node>> contextMap = new HashMap<>();
    private Stack<HashMap<String, ArrayList<Node>>> contextStack = new Stack<>();
    private ArrayList<Node> curr = new ArrayList<>();

    @Override
    public ArrayList<Node> visitXqAp(XQueryParser.XqApContext context) {
        return visit(context.ap());
    }

    @Override
    public ArrayList<Node> visitXqConstructor(XQueryParser.XqConstructorContext context) {
        if (outputDocument == null){
            try {
                DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
                DocumentBuilder docB = docBF.newDocumentBuilder();
                outputDocument = docB.newDocument();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
        ArrayList<Node> result = new ArrayList<>();

        ArrayList<Node> xqRes = visit(context.xq());

        //System.out.println("xqRes size: " + xqRes.size());
        //System.out.println(context.NAME(0).getText());

        result.add(createNode(context.NAME(0).getText(), xqRes));

        return result;
    }

    private void permute(XQueryParser.FLWRContext context, int k, ArrayList<Node> result){
        if (k == context.forClause().var().size()){
            HashMap<String, ArrayList<Node>> contextMapOld = new HashMap<>(contextMap);
            if (context.letClause() != null) {
                visit(context.letClause());
            }
            if (context.whereClause() != null) {
                if(visit(context.whereClause()).size()==0){
                    return;
                }
            }
            ArrayList<Node> c = visit(context.returnClause());
            if (c != null) {
                result.addAll(visit(context.returnClause()));
            }
            contextMap = contextMapOld;
        }
        else {
            String var = context.forClause().var(k).getText();
            ArrayList<Node> varNodes = visit(context.forClause().xq(k));
            for (Node temp : varNodes){
                contextMap.remove(var);
                ArrayList<Node> nList = new ArrayList<>();
                nList.add(temp);
                contextMap.put(var, nList);
                permute(context, k + 1, result);
            }

        }
    }

    @Override
    public ArrayList<Node> visitFLWR(XQueryParser.FLWRContext context) {
        ArrayList<Node> result = new ArrayList<>();
        HashMap<String, ArrayList<Node>> contextMapOld = new HashMap<>(contextMap);
        contextStack.push(contextMapOld);
        permute(context, 0, result);
        contextMap = contextStack.pop();
        return result;
    }

    @Override
    public ArrayList<Node> visitTwoXq(XQueryParser.TwoXqContext context) {
        //System.out.println("im in");
        ArrayList<Node> cpy = curr;
        //System.out.println(context.xq(0));
        ArrayList<Node> result = visit(context.xq(0));
        //curr = cpy;
        //System.out.println(context.xq(1));
        result.addAll(visit(context.xq(1)));
        curr = result;
        return result;
    }

    @Override
    public ArrayList<Node> visitVariable(XQueryParser.VariableContext context) {
        return contextMap.get(context.getText());
    }

    @Override
    public ArrayList<Node> visitXqDescendantRp(XQueryParser.XqDescendantRpContext context) {
        ArrayList<Node> result = new ArrayList<>();
        LinkedList<Node> ll = new LinkedList<>();
        ArrayList<Node> temp = visit(context.xq());
        result.addAll(temp);
        ll.addAll(temp);
        while(!ll.isEmpty()){
            Node tempNode = ll.poll();
            result.addAll(children(tempNode));
            ll.addAll(children(tempNode));
        }
        curr = result;
        return visit(context.rp());
    }

    @Override
    public ArrayList<Node> visitXqwithParentheses(XQueryParser.XqwithParenthesesContext context) {
        return visit(context.xq());
    }

    @Override
    public ArrayList<Node> visitStringConstant(XQueryParser.StringConstantContext context) {
        Node temp = createText(context.StringConstant().getText().substring(1, context.StringConstant().getText().length()-1));
        ArrayList<Node> result = new ArrayList<>();
        result.add(temp);
        return result;
    }

    @Override
    public ArrayList<Node> visitXqLet(XQueryParser.XqLetContext context) {
        HashMap<String, ArrayList<Node>> contextMapOld = new HashMap<>(contextMap);
        contextStack.push(contextMapOld);
        ArrayList<Node> result = visitChildren(context);
        contextMap = contextStack.pop();
        return result;
    }

    @Override
    public ArrayList<Node> visitXqRp(XQueryParser.XqRpContext context) {
        curr = visit(context.xq());
        return visit(context.rp());
    }

    private ArrayList<Node> getItems (int v, XQueryParser.ForClauseContext context) {
        ArrayList<Node> result = new ArrayList<>();
        ArrayList<Node> tempList = visit(context.xq(v));
        if(context.xq().size() == 1) {
            for(Node temp: tempList) {
                ArrayList<Node> tempList2 = new ArrayList<>();
                tempList2.add(temp);
                contextMap.put(context.var(v).getText(), tempList2);
                result.add(temp);
            }
            return result;
        }
        else {
            for(Node temp: tempList) {
                HashMap<String, ArrayList<Node>> contextMapOld = new HashMap<>(contextMap);
                contextStack.push(contextMapOld);
                ArrayList<Node> tempList2 = new ArrayList<>();
                tempList2.add(temp);
                contextMap.put(context.var(v).getText(), tempList2);
                result.addAll(getItems(v + 1, context));
                contextMap = contextStack.pop();
            }
            return result;
        }
    }

    @Override
    public ArrayList<Node> visitForClause(XQueryParser.ForClauseContext context) {
        ArrayList<Node> result = new ArrayList<>();
        result.addAll(getItems(0, context));
        return result;
    }

    @Override
    public ArrayList<Node> visitLetClause(XQueryParser.LetClauseContext context) {
        for (int i = 0; i < context.var().size(); i++) {
            contextMap.put(context.var(i).getText(), visit(context.xq(i)));
        }
        return null;
    }

    @Override
    public ArrayList<Node> visitWhereClause(XQueryParser.WhereClauseContext context) {
        return visit(context.cond());
    }

    @Override
    public ArrayList<Node> visitReturnClause(XQueryParser.ReturnClauseContext context) {
        return visit(context.xq());
    }

    @Override
    public ArrayList<Node> visitXqEqual(XQueryParser.XqEqualContext context) {
        ArrayList<Node> tempList = curr;
        ArrayList<Node> left = visit(context.xq(0));
        curr = tempList;
        ArrayList<Node> right = visit(context.xq(1));
        curr = tempList;
        ArrayList<Node> result = new ArrayList<>();
        for (Node i : left) {
            for (Node j : right) {
                if (i.isEqualNode(j)) {
                    result.add(i);
                    return result;
                }
            }
        }
        return result;
    }

    @Override
    public ArrayList<Node> visitXqEmpty(XQueryParser.XqEmptyContext context) {
        ArrayList<Node> xqResult = visit(context.xq());
        ArrayList<Node> result = new ArrayList<>();
        if (xqResult.isEmpty()){
            Node dummy = inputDocument.createElement("dummy");
            result.add(dummy);
        }
        return result;
    }

    @Override
    public ArrayList<Node> visitXqCondOr(XQueryParser.XqCondOrContext context) {
        ArrayList<Node> left = visit(context.cond(0));
        if (!left.isEmpty()){
            return left;
        }
        ArrayList<Node> right = visit(context.cond(1));
        if (!right.isEmpty()){
            return right;
        }
        //inputDocument.createTextNode()
        return new ArrayList<>();
    }

    @Override
    public ArrayList<Node> visitXqSome(XQueryParser.XqSomeContext context) {
        for (int i = 0; i < context.var().size(); i++) {
            contextMap.put(context.var(i).getText(), visit(context.xq(i)));
        }
        return visit(context.cond());
    }

    @Override
    public ArrayList<Node> visitXqIs(XQueryParser.XqIsContext context) {
        ArrayList<Node> tempList = curr;
        ArrayList<Node> left = visit(context.xq(0));
        curr = tempList;
        ArrayList<Node> right = visit(context.xq(1));
        curr = tempList;
        ArrayList<Node> result = new ArrayList<>();
        for (Node i : left) {
            for (Node j : right) {
                if (i == j) {
                    result.add(i);
                    return result;
                }
            }
        }
        return result;
    }

    @Override
    public ArrayList<Node> visitXqCondNot(XQueryParser.XqCondNotContext context) {
        ArrayList<Node> notList = visit(context.cond());
        ArrayList<Node> result = new ArrayList<>();
        if (notList.isEmpty()){
            Node dummy = inputDocument.createElement("dummy");
            result.add(dummy);
        }
        return result;
    }

    @Override
    public ArrayList<Node> visitXqCondwithParentheses(XQueryParser.XqCondwithParenthesesContext context) {
        return visit(context.cond());
    }

    @Override
    public ArrayList<Node> visitXqCondAnd(XQueryParser.XqCondAndContext context) {
        ArrayList<Node> left = visit(context.cond(0));
        ArrayList<Node> right = visit(context.cond(1));
        if (!left.isEmpty() && !right.isEmpty()){
            return left;
        }
        return new ArrayList<>();
    }

    @Override
    public ArrayList<Node> visitDoc(XQueryParser.DocContext context) {
        if(inputDocument == null) {
            File xmlFile = new File(context.fileName().getText());
            DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
            docBF.setIgnoringElementContentWhitespace(true);
            DocumentBuilder docB = null;
            try {
                docB = docBF.newDocumentBuilder();
            } catch (ParserConfigurationException pE1) {
                pE1.printStackTrace();
            }

            try {
                if (docB != null) {
                    inputDocument = docB.parse(xmlFile);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            inputDocument.getDocumentElement().normalize();
        }
        ArrayList<Node> result = new ArrayList<>();
        result.add(inputDocument);
        curr = result;
        return result;
    }

    @Override
    public ArrayList<Node> visitApChildren(XQueryParser.ApChildrenContext ctx) {
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
    public ArrayList<Node> visitApDescendant(XQueryParser.ApDescendantContext ctx) {
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
    public ArrayList<Node> visitAllChildren(XQueryParser.AllChildrenContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        for(Node temp : curr) {
            res.addAll(children(temp));
        }
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitRpwithParentheses(XQueryParser.RpwithParenthesesContext ctx) {
        return visit(ctx.rp());
    }

    @Override
    public ArrayList<Node> visitTag(XQueryParser.TagContext ctx) {
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
    public ArrayList<Node> visitRpDescendant(XQueryParser.RpDescendantContext ctx) {
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
    public ArrayList<Node> visitParent(XQueryParser.ParentContext ctx) {
        ArrayList<Node> res = new ArrayList<>();
        for(Node temp : curr) {
            if(!res.contains(temp.getParentNode())) res.add(temp.getParentNode());
        }
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitAttribute(XQueryParser.AttributeContext ctx) {
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
    public ArrayList<Node> visitRpChildren(XQueryParser.RpChildrenContext ctx) {
        visit(ctx.rp(0));
        ArrayList<Node> res = visit(ctx.rp(1));
        curr = res;
        return res;
    }

    @Override
    public ArrayList<Node> visitText(XQueryParser.TextContext ctx) {
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
    public ArrayList<Node> visitCurrent(XQueryParser.CurrentContext ctx) {
        return curr;
    }

    @Override
    public ArrayList<Node> visitRpConcatenation(XQueryParser.RpConcatenationContext ctx) {
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
    public ArrayList<Node> visitRpFilter(XQueryParser.RpFilterContext ctx) {
        curr = visit(ctx.rp());
        curr = visit(ctx.filter());
        return curr;
    }

    @Override
    public ArrayList<Node> visitFilterAnd(XQueryParser.FilterAndContext ctx) {
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
    public ArrayList<Node> visitFilterEqual(XQueryParser.FilterEqualContext ctx) {
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
    public ArrayList<Node> visitFilterNot(XQueryParser.FilterNotContext ctx) {
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
    public ArrayList<Node> visitFilterOr(XQueryParser.FilterOrContext ctx) {
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
    public ArrayList<Node> visitFilterIs(XQueryParser.FilterIsContext ctx) {
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
    public ArrayList<Node> visitFilterwithParentheses(XQueryParser.FilterwithParenthesesContext ctx) {
        return visit(ctx.filter());
    }

    @Override
    public ArrayList<Node> visitFilterRp(XQueryParser.FilterRpContext ctx) {
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

    private Node createNode(String s, ArrayList<Node> nodeList) {
        Node result = outputDocument.createElement(s);
        for (Node temp : nodeList) {
            if (temp != null) {
                Node newNode = outputDocument.importNode(temp, true);
                result.appendChild(newNode);
            }
        }
        return result;
    }

    private Node createText(String s) {
        return inputDocument.createTextNode(s);
    }
}