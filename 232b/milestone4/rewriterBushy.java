import java.io.File;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class rewriterBushy {
    public String rewrite(XQueryParser.FLWRContext ctx){
        //PrintWriter writer = new PrintWriter("the-file-name.txt", "UTF-8");
        String output = "";
        //return "";
        int numFor;// nums of for clause
        numFor = ctx.forClause().var().size();
        List<HashSet<String>> groups = new LinkedList<>();

        for(int i=0; i < numFor;i++) {
            String key = ctx.forClause().var(i).getText();
            String xquery = ctx.forClause().xq(i).getText();
            String parent = xquery.split("/")[0];

            if(parent.contains("for")){
                //nested situation
                System.out.println("Nested group, no need to join!");
                return "";
            }

            boolean found = false;
            // construct the classification
            for(int j = 0; j < groups.size(); j++) {
                HashSet<String> group = groups.get(j);
                if(group.contains(parent)) {
                    group.add(key);
                    found = true;
                    break;
                }
            }
            if(!found) {
                HashSet<String> newGroup = new HashSet<>();
                newGroup.add(key);
                groups.add(newGroup);
            }
        }
        //Collections.reverse(groups);

        //System.out.println(groups.size());

        if(groups.size() == 1) {
            System.out.println("One group, no need to join!");
            return "";
        }


        //TODO: what if where condition is empty
        if(ctx.whereClause() == null){
            output += "for $tuple in";
            //writer.print("For $tuple in join  (");
            //System.out.print("for $tuple in");
            int[][] whereCond = new int[0][2];
            String[][] cond = new String[0][2];
            output += bushyJoin(groups, 0, groups.size() - 1, ctx, output, cond , whereCond);
        }
        else {
            //where clause
            String[] where = ctx.whereClause().cond().getText().split("and");
            String[][] cond = new String[where.length][2];

            for (int i = 0; i < where.length; i++) {
                cond[i][0] = where[i].split("eq|=")[0];
                cond[i][1] = where[i].split("eq|=")[1];
            }

        /*
        the relation that the where condition belongs to. it could belong to two relations at most
         */
            int[][] whereCond = new int[cond.length][2];

            for (int i = 0; i < cond.length; i++) {
                String left = cond[i][0];
                String right = cond[i][1];
                whereCond[i][0] = -1;
                whereCond[i][1] = -1;
                for (int j = 0; j < groups.size(); j++) {
                    if (groups.get(j).contains(left)) {
                        whereCond[i][0] = j;
                    }
                    if (groups.get(j).contains(right)) {
                        whereCond[i][1] = j;
                    }
                }
            }

            //print out
            output += "for $tuple in";
            //writer.print("For $tuple in join  (");
            //System.out.print("for $tuple in");
            output += bushyJoin(groups, 0, groups.size() - 1, ctx, output, cond, whereCond);
        }

       // System.out.println(output);

//        for (int i = 1; i < groups.size(); i++) {
//            output += " join (";
//            System.out.print(" join (");
//        }

        //for clause
        //print eq: [af1,al1],[af21,al21]
        //output = printJoin(groups, ctx, output,cond,whereCond);



        /*
            produce return clause
        */
        String retClause = ctx.returnClause().xq().getText();

        String[] ret = retClause.split("\\$");
        for (int i = 0; i < ret.length-1; i++) {
            ret[i] = ret[i]+"$tuple/";
        }
        retClause  = ret[0];
        for (int i = 1; i < ret.length; i++) {
            String[] cur1 = ret[i].split(",",2);
            String[] cur2 = ret[i].split("}",2);
            String[] cur3 = ret[i].split("/",2);
            String[] cur = cur1;
            if(cur2[0].length() < cur[0].length()) {
                cur = cur2;
            }
            if(cur3[0].length() < cur[0].length()) {
                cur = cur3;
            }
            ret[i] = cur[0] + "/*";

            if(cur == cur1) {
                ret[i] += ",";
            }else if(cur == cur2) {
                ret[i] += "}";
            }else {
                ret[i] += "/";
            }
            ret[i] += cur[1];
            retClause = retClause + ret[i];
        }

        output += "\nreturn\n";
        output += retClause+"\n";

        return output;
    }

    private String printJoinCond(LinkedList<String> left, LinkedList<String> right, String output) {
        output += "                 [";
        for(int i = 0; i < left.size();i++) {
            output += left.get(i);
            if(i != left.size()-1) {
                output +=",";
            }
        }
        output +="], [";
        for(int i = 0; i < right.size();i++) {
            output += right.get(i);
            if(i != right.size()-1) {
                output +=",";
            }
        }
        output += "]  ";
        return output;
    }

    private String bushyJoin(List<HashSet<String>> groups, int l, int r, XQueryParser.FLWRContext ctx, String output,String[][] cond,int[][] whereCond){
        if(l == r){ //one group
            return leftJoin(groups, l, r, ctx, cond, whereCond);
        }
        else if(l == r - 1){
            String ret = "";
            ret = ret + " join (" + leftJoin(groups, l, r, ctx, cond, whereCond) + ")";
            return ret;
        }

        int m = (l + r) / 2;
        String res = "";
        String leftBushy = bushyJoin(groups, l, m, ctx, output, cond, whereCond);
        String rightBushy = bushyJoin(groups, m + 1, r, ctx, output, cond, whereCond);
        if(rightBushy.charAt(rightBushy.length()-1) != '\n')
            res = res + " join (" + leftBushy + ",\n" + rightBushy + ",\n";
        else
            res = res + " join (" + leftBushy + ",\n" + rightBushy + "\n";

        //>=l && <=m
        // >=m+1 && <=r
        // is it possible to return reference
        LinkedList<String> left = new LinkedList<>();
        LinkedList<String> right = new LinkedList<>();
        for (int ii = 0; ii < cond.length; ii++) {
            if (whereCond[ii][0] >= l && whereCond[ii][0] <= m && (whereCond[ii][1] >= m+1 && whereCond[ii][1] <= r)) {
                left.add(cond[ii][0].substring(1));
                right.add(cond[ii][1].substring(1));
            } else if (whereCond[ii][1] >= l && whereCond[ii][1] <= m && (whereCond[ii][0] >= m+1 && whereCond[ii][0] <= r)) {
                left.add(cond[ii][1].substring(1));
                right.add(cond[ii][0].substring(1));
            }
        }
        //res += ",";
        res = printJoinCond(left, right, res);

//        if(l == 0 && r == groups.size() - 1) {
//            res += ")\n";
//        }
//        else{
//            res += "),\n";
//        }
        //res += "),\n";
        res += ")";
        return res;
    }


    private String leftJoin(List<HashSet<String>> groups, int g1, int g2, XQueryParser.FLWRContext ctx, String[][] cond,int[][] whereCond) {
        //for clause
        int numFor = ctx.forClause().var().size();
        //for(int i = 0; i < classify.size(); i++) {
        String res = "";
        for(int i = g1; i <= g2; i++) {
            HashSet<String> group = groups.get(i);
            String tuples = "";
            int count = 0;
            //print for
            for (int k = 0; k < numFor; k++) {
                String key = ctx.forClause().var(k).getText();
                if (group.contains(key)) {
                    if (count == 0) {
                        res += "for " + key + " in " + ctx.forClause().xq(k).getText();
                        //System.out.print("for " + key + " in " + ctx.forClause().xq(k).getText());
                        count++;
                    } else {
                        res += ",\n";
                        res += "                   " + key + " in " + ctx.forClause().xq(k).getText();
                        //System.out.println(",");
                        //System.out.print("                   " + key + " in " + ctx.forClause().xq(k).getText());
                    }
                    if (tuples.equals("")) {
                        tuples = tuples + " <" + key.substring(1) + "> " + " {" + key + "} " + " </" + key.substring(1) + ">";
                    } else {
                        tuples = tuples + ", <" + key.substring(1) + "> " + " {" + key + "} " + " </" + key.substring(1) + ">";
                    }
                }
            }

            res += "\n";
         //
            //   System.out.print("\n");

            //print where
            for (int j = 0; j < cond.length; j++) {
                int count1 = 0;
                if (whereCond[j][1] == -1 && group.contains(cond[j][0])) {
                    if (count1 == 0) {
                        count1++;
                        res += "where " + cond[j][0] + " eq " + cond[j][1] + "\n";
                        //System.out.println("where " + cond[j][0] + " eq " + cond[j][1]);
                    } else {
                        res += " and  " + cond[j][0] + " eq " + cond[j][1] + "\n";
                        //System.out.println(" and  " + cond[j][0] + " eq " + cond[j][1]);
                    }
                }
            }

            //print return
            tuples = "<tuple>{" + tuples + "}</tuple>,";
            res += "                  return " + tuples + "\n";
         //   System.out.println("                  return" + tuples);

            // return cond
            if(i > g1) {
             if (g1 != g2) {
                LinkedList<String> left = new LinkedList<>();
                LinkedList<String> right = new LinkedList<>();
                //duplicate?
                for (int ii = 0; ii < cond.length; ii++) {
                    if (whereCond[ii][0] == g1 && whereCond[ii][1] == g2) {
                        left.add(cond[ii][0].substring(1));
                        right.add(cond[ii][1].substring(1));
                    } else if (whereCond[ii][1] == g1 && whereCond[ii][0] == g2) {
                        left.add(cond[ii][1].substring(1));
                        right.add(cond[ii][0].substring(1));
                    }
//                    if (whereCond[ii][0] == i && (whereCond[ii][1] >= g1 && whereCond[ii][1] < g2)) {
//                        left.add(cond[ii][1].substring(1));
//                        right.add(cond[ii][0].substring(1));
//                    } else if (whereCond[ii][1] == i && (whereCond[ii][0] >= g1 && whereCond[ii][0] < g2)) {
//                        left.add(cond[ii][0].substring(1));
//                        right.add(cond[ii][1].substring(1));
//                    }
                }
                res = printJoinCond(left, right, res);

//                if(i == groups.size() - 1){
//                    output += ")\n";
//                    //System.out.println(")");
//                }
//                else{
//                    output += "),\n";
//                    //System.out.println("),");
                }
            }
        }

        return res;
    }

}
