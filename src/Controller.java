import structures.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import java.io.*;

/**
 * Created by Administrator on 2015/10/7.
 */
public class Controller {
    RecordManager recordManager;
    CatalogManager catalogManager;
    IndexManager indexManager;
    Table table;
    BPTree index;
    ArrayList<Hashtable<String,String>> resList;
    Attribute attr;
    int rows;

    public Controller() throws IOException, ClassNotFoundException {
        recordManager = new RecordManager();
        catalogManager = new CatalogManager();
        indexManager = new IndexManager();
    }

    public void executeSQL(SQLstatement sql) throws Throwable {
        switch (sql.getType()) {
            case CREATE_INDEX:
                if (indexManager.getIndex(sql.getIndexName())!=null) {
                    System.out.println("<---INDEX "+sql.getIndexName()+" ALREADY EXISTS!--->");
                    return;
                }
                table = catalogManager.findTable(sql.getTableName());
                attr = table.getAttr(sql.getContent());
                if (!attr.getIndexName().equals("")) {
                    System.out.println("<---INDEX ALREADY EXISTS!--->");
                    return;
                }
                if (!(attr.isPrimKey() || attr.isUnique())) {
                    System.out.println("<---ATTRIBUTE "+attr.getName()+" IS NOT UNIQUE!--->");
                }
                sql.attributes.add(table.getAttr(sql.getContent()));
                resList= recordManager.select(table, sql.getConditions());
                indexManager.createIndex(table, sql.getContent(), resList, sql.getIndexName());
                table.getAttr(sql.getContent()).setIndexName(sql.getIndexName());
                System.out.println("<---INDEX "+sql.getIndexName()+" CREATED!--->");
                break;
            case CREATE_TABLE:
                table = catalogManager.findTable(sql.getTableName());
                if (table!=null) {
                    System.out.println("<---TABLE "+sql.getTableName()+" ALREADY EXISTS!--->");
                    return;
                }
                catalogManager.createTable(sql);
                table = catalogManager.findTable(sql.getTableName());
                indexManager.createIndex(table, table.getPrimKey(), null, table.getName() + "_prim_idx");
                table.getAttr(table.getPrimKey()).setIndexName(table.getName() + "_prim_idx");
                for (Attribute attr : table.getAttributes())
                    if (attr.isUnique()) {
                        indexManager.createIndex(table, attr.getName(), null, table.getName() + "_unique_idx_" +attr.getName());
                        attr.setIndexName(table.getName() + "_unique_idx_" +attr.getName());
                    }

                System.out.println("<---TABLE " + sql.getTableName() + " CREATED!--->");
                break;
            case DROP_INDEX:
                index = indexManager.getIndex(sql.getIndexName());
                if (index == null) {
                    System.out.println("<---INDEX "+sql.getIndexName()+" DOES NOT EXIST!--->");
                    return;
                }

                indexManager.dropIndex(index.indexName);
                System.out.println("<---INDEX "+sql.getIndexName()+" DROPPED!--->");
                break;
            case DROP_TABLE:
                table = catalogManager.findTable(sql.getTableName());
                if (table==null) {
                    System.out.println("<---TABLE "+sql.getTableName()+" DOES NOT EXIST!--->");
                    return;
                }
                catalogManager.dropTable(sql.getTableName());
                recordManager.dropTable(sql.getTableName());
                System.out.println("<---TABLE "+sql.getTableName()+" DROPPED!--->");
                break;
            case SELECT:
                table = catalogManager.findTable(sql.getTableName());
                if (table==null) {
                    System.out.println("<---TABLE "+sql.getTableName()+" DOES NOT EXIST!--->");
                    return;
                }
                resList = recordManager.select(table, sql.getConditions());
                printSelect(resList, sql.getAttributes());
                break;
            case SELECT_WHERE:
                table = catalogManager.findTable(sql.getTableName());
                if (table==null) {
                    System.out.println("<---TABLE "+sql.getTableName()+" DOES NOT EXIST!--->");
                    return;
                }
                if (isSimpleEqual(sql.getConditions()) && !table.getAttr(sql.getConditions().get(0).getAttrName()).getIndexName().equals(""))
                    resList = indexManager.selectByIndex(table.getAttr(sql.getConditions().get(0).getAttrName()).getIndexName(), sql.getConditions().get(0), recordManager, table);
                 else resList = recordManager.select(table, sql.getConditions());

                printSelect(resList, sql.getAttributes());
                break;
            case DELETE:
                table = catalogManager.findTable(sql.getTableName());
                if (table==null) {
                    System.out.println("<---TABLE "+sql.getTableName()+" DOES NOT EXIST!--->");
                    return;
                }
                rows = recordManager.delete(table, sql.getConditions(), indexManager);

                System.out.println("<---"+rows+" ROWS DELETED!--->");
                break;
            case DELETE_WHERE:
                table = catalogManager.findTable(sql.getTableName());
                if (table==null) {
                    System.out.println("<---TABLE "+sql.getTableName()+" DOES NOT EXIST!--->");
                    return;
                }
                if (isSimpleEqual(sql.getConditions()) && !table.getAttr(sql.getConditions().get(0).getAttrName()).getIndexName().equals("")) {  //only single Equal condition may use index
                    rows = indexManager.deleteByIndex(table.getAttr(sql.getConditions().get(0).getAttrName()).getIndexName(), sql.getConditions().get(0), recordManager, table);
                    System.out.println("<---" + rows + " ROWS DELETED!--->");
                } else {
                    rows = recordManager.delete(table, sql.getConditions(), indexManager);
                    System.out.println("<---" + rows + " ROWS DELETED!--->");
                }
                break;
            case INSERT:
                table = catalogManager.findTable(sql.getTableName());
                if (table==null) {
                    System.out.println("<---TABLE "+sql.getTableName()+" DOES NOT EXIST!--->");
                    return;
                }
                int res = recordManager.insert(table, sql.getContent(), indexManager);
//                indexManager.printTree(indexManager.getIndex(table.getAttr(table.getPrimKey()).getIndexName()));
                if (res != -1) System.out.println("<---1 ROW INSERTED!--->");
                break;
            case EXECFILE:
                File file = new File("./scripts/"+sql.getContent());
                if (!file.exists()) {
                    System.out.println("<---FILE "+sql.getContent()+" DOES NOT EXIST!--->");
                    return;
                }
                BufferedReader bfr = new BufferedReader(new FileReader(file));
                String sqlOrder = "", str;
                while (true) {
                    try{
                        str = bfr.readLine();
                        if (str == null) break;
                    } catch (EOFException e) {
                        break;
                    }
                    sqlOrder += str.trim() + " ";
                    if (str.contains(";")) {
                        SQLstatement fileSql = new SQLstatement(sqlOrder);
                        sqlOrder = "";
                        executeSQL(fileSql);
                    }
                }
                break;
            case QUIT:
                indexManager.printTree(indexManager.indexList.get(0));

                recordManager.finalize();
                indexManager.finalize();
                catalogManager.finalize();
                System.exit(0);
            default:break;
        }
    }


    public boolean isSimpleEqual(ArrayList<Condition> condList) {
        return (condList.size() == 1) && (condList.get(0).getRelationType() == RELATION_TYPE.EQUAL);
    }

    public boolean isSelectAttr(ArrayList<Attribute> attrList, Attribute attr) {
        for (Attribute a : attrList)
            if (attr.getName().equals(a.getName())) return true;
        return false;
    }

    public void printSelect(ArrayList<Hashtable<String,String>> resList, ArrayList<Attribute> attrList) {
        int cnt = 0;
        if (attrList.get(0).getName().equals("*")) attrList = table.getAttributes();
        for (Hashtable ht : resList) {
            for (Attribute attr : table.getAttributes())
                if (isSelectAttr(attrList, attr))
                    System.out.print(attr.getName() + " : " + ht.get(attr.getName()) + " ");
            cnt++;
            System.out.println();
        }
        System.out.println("<---"+cnt+" ROWS SELECTED!--->");
    }
}
