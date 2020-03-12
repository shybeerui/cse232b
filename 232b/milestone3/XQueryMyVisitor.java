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

    private Document doc;

    rewriter rw = new rewriter();

    @Override
    public ArrayList<Node> visitXqjoin(XQueryParser.XqjoinContext ctx) {
        //System.out.println(3);
        return visitChildren(ctx);
    }

    @Override
    public ArrayList<Node> visitJoinClause(XQueryParser.JoinClauseContext ctx) {
        //System.out.println(4);
        ArrayList<Node> left = visit(ctx.xq(0));
        ArrayList<Node> right = visit(ctx.xq(1));
        int idSize = ctx.nameList(0).NAME().size();
        String [] idListLeft = new String [idSize];
        String [] idListRight = new String [idSize];
        for (int i = 0; i < idSize; i++){
            idListLeft[i] = ctx.nameList(0).NAME(i).getText();
            idListRight[i] = ctx.nameList(1).NAME(i).getText();
        }
        HashMap<String, ArrayList<Node>> hashMapOnLeft = buildHashTable(left, idListLeft);
        ArrayList<Node> result = probeJoin(hashMapOnLeft, right, idListLeft, idListRight);
        curr = result;
        return result;
    }

    private HashMap buildHashTable(ArrayList<Node> tupleList, String [] hashAtts){
        HashMap<String, ArrayList<Node>> result = new HashMap<>();
        for (Node tuple: tupleList){
            ArrayList<Node> children = children(tuple);
            String key = "";
            for (String hashAtt: hashAtts) {
                for (Node child: children){
                    if (hashAtt.equals(child.getNodeName()))
                        key += child.getFirstChild().getTextContent();
                }
            }
            if (result.containsKey(key))
                result.get(key).add(tuple);
            else{
                ArrayList<Node> value = new ArrayList<>();
                value.add(tuple);
                result.put(key, value);
            }
        }
        return result;
    }

    private ArrayList<Node> probeJoin(HashMap<String, ArrayList<Node>> hashMapOnLeft, ArrayList<Node> right, String [] idListLeft, String []idListRight){
        ArrayList<Node> result = new ArrayList<>();
        for (Node tuple: right){
            ArrayList<Node> children = children(tuple);
            String key = "";
            for (String hashAtt: idListRight) {
                for (Node child: children){
                    if (hashAtt.equals(child.getNodeName())) {
                        key += child.getFirstChild().getTextContent();
                    }
                }
            }
            if (hashMapOnLeft.containsKey(key))
                result.addAll(product(hashMapOnLeft.get(key),tuple));
        }
        return result;
    }

    private Node makeElem(String tag, ArrayList<Node> list){
        Node result = outputDocument.createElement(tag);
        for (Node node : list) {
            if (node != null) {
                Node newNode = outputDocument.importNode(node, true);
                result.appendChild(newNode);
            }
        }
        return result;
    }

    private ArrayList<Node> product(ArrayList<Node> leftList, Node right){
        ArrayList<Node> result = new ArrayList<>();
        for (Node left: leftList){
            ArrayList<Node> newTupleChildren = children(left);
            newTupleChildren.addAll(children(right));
            result.add(makeElem("tuple", newTupleChildren));
        }
        return result;
    }

    @Override
    public ArrayList<Node> visitXqAp(XQueryParser.XqApContext context) {
        return visit(context.ap());
    }

    @Override
    public ArrayList<Node> visitXqConstructor(XQueryParser.XqConstructorContext context) {
        if(doc == null) {
            try {
                DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
                DocumentBuilder docB = docBF.newDocumentBuilder();
                doc = docB.newDocument();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }

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
            contextStack.push(contextMapOld);
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
            contextMap = contextStack.pop();
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

    boolean needRewrite = true;



    @Override
    public ArrayList<Node> visitFLWR(XQueryParser.FLWRContext context) {
        //System.out.println(0);
        if (doc == null) {
            try {
                DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
                DocumentBuilder docB = docBF.newDocumentBuilder();
                doc = docB.newDocument();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }

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
        HashMap<String, ArrayList<Node>> contextMapOld = new HashMap<>(contextMap);
        contextStack.push(contextMapOld);
        if (!needRewrite){
            //if (!needRewrite || ctx.forClause().xq(0).getText().startsWith("join")){
            permute(context, 0, result);
        }
        else{
            String rewrited = rw.rewrite(context);
            System.out.println(rewrited);
            if (rewrited  == ""){
                needRewrite = false;
                permute(context, 0, result);
            }
            else {
                //System.out.println(1);
                result = XQuery.evalRewrited(rewrited);
            }
        }
        //System.out.println(2);
        contextMap = contextStack.pop();
        //System.out.println(result == null);
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
        curr = contextMap.get(context.getText());
        return contextMap.get(context.getText());
    }

    @Override
    public ArrayList<Node> visitXqDescendantRp(XQueryParser.XqDescendantRpContext context) {
        ArrayList<Node> result = new ArrayList<>();
        LinkedList<Node> ll = new LinkedList<>();
        ArrayList<Node> temp = visit(context.xq());
        if(temp == null){
            curr = result;
            return result;
        }

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
        String t = context.StringConstant().getText().substring(1, context.StringConstant().getText().length()-1);
        Node temp = createText(t);
        ArrayList<Node> result = new ArrayList<>();
        result.add(temp);
        return result;
    }

    @Override
    public ArrayList<Node> visitXqLet(XQueryParser.XqLetContext context) {
//        visit(context.letClause());
//        return visit(context.xq());

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

    @Override
    public ArrayList<Node> visitForClause(XQueryParser.ForClauseContext context) {
        return null;
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
//        System.out.println(context.xq(0).getText());
////        System.out.println(left.size());
////        System.out.println(context.xq(1).getText());
////        System.out.println(right.size());
        ArrayList<Node> result = new ArrayList<>();
        if(left == null || right == null)
            return result;
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

    private boolean satisfyCondHelper(int k, XQueryParser.XqSomeContext context){
        int numFor = context.var().size();
        if (k == numFor){
            if (visit(context.cond()).size() != 0)
                return true;
        }
        else{
            String key = context.var(k).getText();
            ArrayList<Node> valueList = visit(context.xq(k));

            for (Node node: valueList){
                HashMap<String, ArrayList<Node>> contextMapOld = new HashMap<>(contextMap);
                contextStack.push(contextMapOld);

                ArrayList<Node> value = new ArrayList<>(); value.add(node);
                contextMap.put(key, value);
                if (k+1 <= numFor)
                    if (satisfyCondHelper(k+1, context)) {
                        contextMap = contextStack.pop();
                        return true;
                    }
                contextMap = contextStack.pop();
            }
        }
        return false;
    }


    @Override public ArrayList<Node> visitXqSome(XQueryParser.XqSomeContext context) {
        ArrayList<Node> result = new ArrayList<>();
        if (satisfyCondHelper(0, context)){
            Node True = doc.createTextNode("true");
            result.add(True);
        }
        return result;
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
            //System.out.println(context.fileName().getText());
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
        //System.out.println("ok");
//        if(nodeList == null)
//            return result;
        for (Node temp : nodeList) {
            if (temp != null) {
                Node newNode = outputDocument.importNode(temp, true);
                result.appendChild(newNode);
            }
        }
        return result;
    }

    private Node createText(String s) {
        Node res = doc.createTextNode(s);
        return res;
    }
}