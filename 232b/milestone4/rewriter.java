import java.io.File;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class rewriter {
    public String rewrite(XQueryParser.FLWRContext ctx){
        //PrintWriter writer = new PrintWriter("the-file-name.txt", "UTF-8");
        String output = "";

        int numFor;// nums of for clause
        numFor = ctx.forClause().var().size();
        List<HashSet<String>> classify = new LinkedList<>();
        List<String> relation = new LinkedList<>();
        for(int i=0; i < numFor;i++) {
            String key = ctx.forClause().var(i).getText();
            String xquery = ctx.forClause().xq(i).getText();
            String parent = xquery.split("/")[0];

            int size = classify.size();
            boolean found = false;
            // construct the classification
            for(int j = 0; j < size; j++) {
                HashSet<String> curSet = classify.get(j);
                if(curSet.contains(parent)) {
                    curSet.add(key);
                    found = true;
                    break;
                }
            }
            if(!found) {
                HashSet<String> newSet = new HashSet<>();
                newSet.add(key);
                classify.add(newSet);
                relation.add(key);
            }
        }
        if(classify.size() == 1) {
            System.out.println("No need to join!");
            return "";
        }

        //where clause
        String[] where = ctx.whereClause().cond().getText().split("and");
        String[][] cond = new String[where.length][2];
        for(int i = 0; i < where.length;i++) {
            cond[i][0] = where[i].split("eq|=")[0];
            cond[i][1] = where[i].split("eq|=")[1];
        }
        /*
        the relation that the where condition belongs to. it could belong to two relations at most
         */
        int[][] relaWhere = new int[cond.length][2];

        for(int i=0; i < cond.length; i++) {
            String cur0 = cond[i][0];
            String cur1 = cond[i][1];
            relaWhere[i][0] = -1;
            relaWhere[i][1] = -1;
            for(int j = 0; j < classify.size();j++) {
                if(classify.get(j).contains(cur0)) {
                    relaWhere[i][0] = j;
                }
                if(classify.get(j).contains(cur1)) {
                    relaWhere[i][1] = j;
                }
            }
        }

        int class_size = classify.size();
        //print out
        output += "for $tuple in";
        //writer.print("For $tuple in join  (");
//        System.out.print("for $tuple in");
        for (int i = 1; i < class_size;i++) {

            output += " join (";
//            System.out.print(" join (");

        }
        //for clause
        //print eq: [af1,al1],[af21,al21]
        output = PrintJoin(classify, ctx, output,cond,relaWhere);
        if(output == "")
            return "";
//        //System.out.println(output.length());

//        if(class_size > 2) {
//            output = Print3Join(classify, ctx, output, cond, relaWhere);
//        }
//        if(class_size > 3) {
//            output = Print4Join(classify, ctx, output, cond, relaWhere);
//        }
//        if(class_size > 4) {
//            output = Print5Join(classify, ctx, output, cond, relaWhere);
//        }
//        if(class_size > 5) {
//            output = Print6Join(classify, ctx, output, cond, relaWhere);
//        }

        /*
            return clause
        */
        String retClause = ctx.returnClause().xq().getText();
        String[] tempRet = retClause.split("\\$");
        for (int i = 0; i < tempRet.length-1; i++) {
            tempRet[i] = tempRet[i]+"$tuple/";
        }
        retClause  = tempRet[0];
        for (int i = 1; i < tempRet.length; i++) {
            String[] cur1 = tempRet[i].split(",",2);
            String[] cur2 = tempRet[i].split("}",2);
            String[] cur3 = tempRet[i].split("/",2);
            String[] cur = cur1;
            if(cur2[0].length() < cur[0].length()) {
                cur = cur2;
            }
            if(cur3[0].length() < cur[0].length()) {
                cur = cur3;
            }
            tempRet[i] = cur[0] + "/*";

            if(cur == cur1) {
                tempRet[i] += ",";
            }else if(cur == cur2) {
                tempRet[i] += "}";
            }else {
                tempRet[i] += "/";
            }
            tempRet[i] += cur[1];
            retClause = retClause + tempRet[i];
        }

        output += "return\n";
        output += retClause+"\n";
//        System.out.println("return");
//        System.out.println(retClause);
        /*
            write in txt
         */
//        writer w = new writer();
//        w.writing("input/output.txt",output);
        return output;
    }

    private String PrintJoinCond(LinkedList<String> ret0, LinkedList<String> ret1, String output) {
        //if the return value is empty
//        if(ret0.isEmpty() && ret1.isEmpty()){
////////            return "";
////////        }
        output += "                 [";
//        System.out.print("                 [");
        for(int i = 0; i < ret0.size();i++) {
            output += ret0.get(i);
//            System.out.print(ret0.get(i));
            if(i != ret0.size()-1) {
                output +=",";
//                System.out.print(",");
            }
        }
        output +="], [";
//        System.out.print("], [");
        for(int i = 0; i < ret1.size();i++) {
            output +=ret1.get(i);
//            System.out.print(ret1.get(i));
            if(i != ret1.size()-1) {
                output +=",";
//                System.out.print(",");
            }
        }
        output += "]  ";
//        System.out.print("]  ");
        return output;
    }


    private String PrintJoin(List<HashSet<String>> classify, XQueryParser.FLWRContext ctx, String output,String[][] cond,int[][] relaWhere) {
        //for clause
        int numFor = ctx.forClause().var().size();
        //for(int i = 0; i < classify.size(); i++) {
        for(int i = 0; i < classify.size(); i++) {
            HashSet<String> curSet = classify.get(i);
            String tuples = "";
            int count = 0;
            //print for
            for(int k = 0; k < numFor; k++) {
                String key = ctx.forClause().var(k).getText();
                if(curSet.contains(key)){
                    if(count == 0) {
                        String tmpp = ctx.forClause().xq(k).getText();
                        String[] sl = tmpp.split("return",2);
                        if(sl.length == 2)
                            tmpp = sl[0] + " return " + sl[1];
                        output += "for " + key + " in " + tmpp;
//                        System.out.print("for " + key + " in " + tmpp);
                        count++;
                    }else {
                        output += ",\n";
                        output += "                   " + key + " in " + ctx.forClause().xq(k).getText();
//                        System.out.println(",");
//                        System.out.print("                   " + key + " in " + ctx.forClause().xq(k).getText());
                    }
                    if(tuples.equals("")) {
                        tuples = tuples + " <" + key.substring(1) + "> " + " {" + key + "} " + " </" + key.substring(1) + ">";
                    }else {
                        tuples = tuples + ", <" + key.substring(1) + "> " + " {" + key + "} " + " </" + key.substring(1) + ">";
                    }
                }
            }
            output += "\n";
//            System.out.print("\n");
            //print where
            for(int j = 0;j < cond.length;j++) {
                int count1 = 0;
                if(relaWhere[j][1] == -1 && curSet.contains(cond[j][0])) {
                    if(count1 == 0){
                        count1++;
                        output += "where " + cond[j][0] + " eq " + cond[j][1] +"\n";
//                        System.out.println("where " + cond[j][0] + " eq " + cond[j][1]);
                    }else {
                        output += " and  " + cond[j][0] + " eq " + cond[j][1] + "\n";
//                        System.out.println(" and  " + cond[j][0] + " eq " + cond[j][1]);
                    }
                }
            }
            //print return
            tuples = "<tuple> {"+tuples+"} </tuple>,";
            output += "                  return " + tuples + "\n";
//            System.out.println("                  return " + tuples);

            // return cond
            if(i > 0) {
                LinkedList<String> ret0 = new LinkedList<>();
                LinkedList<String> ret1 = new LinkedList<>();
//                System.out.println(cond.length == 0);
                for (int ii = 0; ii < cond.length; ii++) {
                    if (relaWhere[ii][0] == i && (relaWhere[ii][1] >= 0 && relaWhere[ii][1] < i)) {
                        ret0.add(cond[ii][1].substring(1));
                        ret1.add(cond[ii][0].substring(1));
                    } else if (relaWhere[ii][1] == i && (relaWhere[ii][0] >= 0 && relaWhere[ii][0] < i)) {
                        ret0.add(cond[ii][0].substring(1));
                        ret1.add(cond[ii][1].substring(1));
                    }
                }
                output = PrintJoinCond(ret0, ret1, output);
                if(output == "")
                    return "";
                if(i != classify.size() - 1)
                    output += "),\n";
                else
                    output += ")\n";
//                System.out.println(")");
            }
        }
        return output;
    }
}

