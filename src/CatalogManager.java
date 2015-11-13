import javafx.scene.control.Tab;
import structures.Attribute;
import structures.SQLstatement;
import structures.Table;

import java.io.*;
import java.util.ArrayList;

public class CatalogManager {
    ArrayList<Table> tableList;

    public CatalogManager() throws IOException, ClassNotFoundException {
        tableList = new ArrayList<>();
        Table table;
        File file = new File("./catalog/tableinfo.info");
        if (!file.exists()) file.createNewFile();
        FileInputStream fis = new FileInputStream(file);

        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            while ((table = (Table)ois.readObject()) != null) tableList.add(table);
            ois.close();
        } catch (EOFException e) {
            System.out.println("tableinfo.info is empty!");
        }

        fis.close();
    }

    @Override
    protected void finalize() throws Throwable {
        File file = new File("./catalog/tableinfo.info");
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("");
        fileWriter.close();

        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        for (Table table : tableList) oos.writeObject(table);
        oos.flush();
        oos.close();
        fos.close();
        super.finalize();
    }

    public void createTable(SQLstatement sqLstatement) {
        Table table = new Table();
        int tupleLength = 0;

        table.setAttributes(sqLstatement.getAttributes());
        table.setAttrNum(sqLstatement.getAttributes().size());
        table.setName(sqLstatement.getTableName());
        for (Attribute attribute : sqLstatement.getAttributes()) {
            if (attribute.isPrimKey()) table.setPrimKey(attribute.getName());
            tupleLength += attribute.getLength();
        }
        table.setTupleLength(tupleLength);
        tableList.add(table);
    }

    public Table findTable(String tableName) {
        for (Table table : tableList)
            if (table.getName().equals(tableName)) return table;
        return null;
    }

    public void dropTable(String tableName) {
        for (Table table : tableList)
            if (table.getName().equals(tableName)) {
                tableList.remove(table);
                return;
            }
    }

}
