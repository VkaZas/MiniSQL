import structures.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;


public class IndexManager {
    ArrayList<BPTree> indexList;
    public static int indexCnt = 0;

    public IndexManager() throws IOException, ClassNotFoundException {
        indexList =  new ArrayList<BPTree>();
        BPTree index;
        File file = new File("./indices/indexinfo.info");
        if (!file.exists()) file.createNewFile();
        FileInputStream fis = new FileInputStream(file);

        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            while ((index = (BPTree)ois.readObject()) != null) indexList.add(index);
            ois.close();
        } catch (EOFException e) {
            System.out.println("indexinfo.info is empty!");
        }

        fis.close();
    }

    @Override
    protected void finalize() throws Throwable {
        File file = new File("./indices/indexinfo.info");
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("");
        fileWriter.close();

        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        for (BPTree index : indexList) oos.writeObject(index);
        oos.flush();
        oos.close();
        fos.close();
        super.finalize();
    }

    public BPTree getIndex(String indexName) {
        for (BPTree bpTree : indexList)
            if (bpTree.indexName.equals(indexName))
                return bpTree;
        return null;
    }

    public void createIndex(Table table, String attrName, ArrayList<Hashtable<String, String>> valList, String indexName) {
        DATA_TYPE type = table.getAttr(attrName).getType();
        BPTree index = new BPTree();
        index.indexName = indexName;
        index.type = type;
        index.belongsToAttr = table.getAttr(attrName);

        if (valList != null) for (Hashtable<String, String> val : valList)
            index.insert(val.get(attrName), Integer.parseInt(val.get("fileofs")));
        indexList.add(index);
        indexCnt++;
    }

    public void dropIndex(String indexName) {
        for (BPTree bpTree : indexList)
            if (bpTree.indexName.equals(indexName)){
                bpTree.belongsToAttr.setIndexName("");
                indexList.remove(bpTree);
                indexCnt--;
                return;
            }
    }

    public int deleteByIndex(String indexName, Condition condition, RecordManager rm, Table fileInfo) throws IOException {
        BPTree index = getIndex(indexName);
        String eleVal = condition.getValue();
        BufferManager bm = rm.bm;

        int ofs = index.find(eleVal);
        if (ofs == -1) return 0;
        int blockOfs = ofs / Buffer.BLOCKSIZE;

        Buffer bf = bm.bufferArray[bm.getBufferNum(fileInfo.getName()+".table", blockOfs)];
        bf.value[ofs % Buffer.BLOCKSIZE] = Buffer.EMPTY;
        int tupleOfs = ofs % Buffer.BLOCKSIZE + 1;
        for (Attribute attr : fileInfo.getAttributes()) {
            byte[] tmpVal = new byte[attr.getLength()];
            System.arraycopy(bf.value, tupleOfs, tmpVal, 0, attr.getLength());
            tupleOfs += attr.getLength();
            String tmpStr = rm.toString(tmpVal, attr.getType(), attr.getLength());
            if (!attr.getIndexName().equals("")) getIndex(attr.getIndexName()).delete(tmpStr);
        }
        fileInfo.decRecordNum();
        return 1;
    }

    public ArrayList<Hashtable<String, String>> selectByIndex(String indexName, Condition condition, RecordManager rm, Table fileInfo) throws IOException {
        ArrayList<Hashtable<String, String>> resList = new ArrayList<>();
        Hashtable<String, String> resTable = new Hashtable<>();
        BPTree index = getIndex(indexName);
        String eleVal = condition.getValue();
        BufferManager bm = rm.bm;

        int ofs = index.find(eleVal);
        if (ofs == -1) return resList;
        int blockOfs = ofs / Buffer.BLOCKSIZE;

        Buffer bf = bm.bufferArray[bm.getBufferNum(fileInfo.getName()+".table", blockOfs)];
        int tupleOfs = ofs % Buffer.BLOCKSIZE + 1;
        for (Attribute attr : fileInfo.getAttributes()) {
            byte[] tmpVal = new byte[attr.getLength()];
            System.arraycopy(bf.value, tupleOfs, tmpVal, 0, attr.getLength());
            tupleOfs += attr.getLength();
            String tmpStr = rm.toString(tmpVal, attr.getType(), attr.getLength());
            resTable.put(attr.getName(), tmpStr);
        }

        resList.add(resTable);
        return resList;
    }

    public void printTree(BPTree index) {
        index.printTree(index.root);
    }
}
