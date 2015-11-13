import structures.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

public class RecordManager {
    BufferManager bm = new BufferManager();


    @Override
    protected void finalize() throws Throwable {
        bm.finalize();
        super.finalize();
    }

    public boolean dropTable(String tableName) {
        bm.freeTable(tableName + ".table");
        File file = new File("./buffer/" + tableName + ".table");
        return file.delete();
    }

    public ArrayList<Hashtable<String,String>> select(Table table, ArrayList<Condition> conditions) throws IOException {
        ArrayList<Hashtable<String,String>> resList = new ArrayList<>();

        bm.scanTable(table);
        int readLength = table.getTupleLength() + 1; //one byte for visible bit


        for (int i=0; i<BufferManager.MAXBLOCKNUM; i++)  //search all bufferblocks
            if (bm.bufferArray[i].getFileName().equals(table.getName() + ".table") && bm.bufferArray[i].isValid) { //find one belongs to current table
                for (int blockOfs=0; Buffer.BLOCKSIZE-readLength >= blockOfs; blockOfs+=readLength) {  //search all tuples in this block
                    byte visible = bm.bufferArray[i].value[blockOfs];
                    Hashtable<String, String> attrTable = new Hashtable<>();
                    if (visible == 1) {
                        boolean valid = true;
                        int tupleOfs = 1;
                        attrTable.put("fileofs", String.valueOf(blockOfs));
                        for (int j=0; j<table.getAttrNum(); j++) {  //record all <attribute,value> pairs in this tuple
                            Attribute tmpAttr = table.getAttributes().get(j);
                            byte[] tmpArray = new byte[tmpAttr.getLength()];
                            System.arraycopy(bm.bufferArray[i].value, blockOfs+tupleOfs, tmpArray, 0, tmpAttr.getLength());
                            tupleOfs += tmpAttr.getLength();
                            String tmpValue = toString(tmpArray, tmpAttr.getType(), tmpAttr.getLength());
                            attrTable.put(tmpAttr.getName(), tmpValue);
                        }

                        if (conditions != null) for (Condition condition : conditions) {  //use conditions to filter and decide valid or not
                            if (!judge(condition, attrTable.get(condition.getAttrName()), table.getAttr(condition.getAttrName()).getType())) {
                                valid = false;
                                break;
                            }
                        }
                        if (valid) {
                            resList.add(attrTable);
//                            for (Attribute attribute : selectAttributes) {
//                                System.out.print(attribute.getName() + ":" + attrTable.get(attribute.getName()) + "  ");  //print
//                            }
//                            System.out.println();
                        }
                    }

                }
            }
        return resList;
    }

    public int delete(Table table, ArrayList<Condition> conditions, IndexManager im) throws IOException {
        bm.scanTable(table);
        int res = 0;
        int readLength = table.getTupleLength() + 1;

        for (int i=0; i<BufferManager.MAXBLOCKNUM; i++)
            if (bm.bufferArray[i].getFileName().equals(table.getName() + ".table"))
                for (int blockOfs=0; Buffer.BLOCKSIZE-readLength >= blockOfs; blockOfs+=readLength) {
                    byte visible = bm.bufferArray[i].value[blockOfs];
                    Hashtable<String, String> attrTable = new Hashtable<>();
                    if (visible == 1) {
                        boolean valid = true;
                        int tupleOfs = 1;
                        for (int j=0; j<table.getAttrNum(); j++) {  //record all <attribute,value> pairs in this tuple
                            Attribute tmpAttr = table.getAttributes().get(j);
                            byte[] tmpArray = new byte[tmpAttr.getLength()];
                            System.arraycopy(bm.bufferArray[i].value, blockOfs+tupleOfs, tmpArray, 0, tmpAttr.getLength());
                            tupleOfs += tmpAttr.getLength();
                            String tmpValue = toString(tmpArray, tmpAttr.getType(), tmpAttr.getLength());
                            attrTable.put(tmpAttr.getName(), tmpValue);
                        }
                        if (conditions != null) for (Condition condition : conditions) {  //use conditions to filter and decide valid or not
                            if (!judge(condition, attrTable.get(condition.getAttrName()), table.getAttr(condition.getAttrName()).getType())) {
                                valid = false;
                                break;
                            }
                        }
                        if (valid) {
                            bm.bufferArray[i].value[blockOfs] = Buffer.EMPTY;
                            res++;
                            table.decRecordNum();

                            for (Attribute attr : table.getAttributes())  //delete in index
                                if (!attr.getIndexName().equals("")) {
                                    BPTree index = im.getIndex(attr.getIndexName());
                                    index.delete(attrTable.get(attr.getName()));
                                }
                        }
                    }
                }
        return res;
    }

    public String toString(byte[] value, DATA_TYPE type, int length) {
        String str;
        byte[] tmpVal = new byte[length];
        int strlen=-1;
        switch (type) {
            case MYINT:
                return Integer.toString(BtoI(value));
            case MYFLOAT:
                return Float.toString(BtoF(value));
            case MYCHAR:
                for (int i=0; i<value.length; i++) if (value[i] == -1) {
                    if (strlen == -1) strlen = i;
                    value[i] = 0;
                }
                if (strlen == -1) strlen = length-1;
                System.arraycopy(value,0,tmpVal,0,strlen+1);
                str =  new String(tmpVal);
                for (int i=0; i<value.length; i++) if (value[i] == 0) value[i] = -1;
                while (str.contains("\u0000")) str=str.trim();
                return str;
            default:return "";
        }
    }

    public int insert(Table table, String values, IndexManager im) throws IOException {  //return the exact position of the tuple in the file for index using
        InsPos insPos;
        Hashtable<String, String> attrTable = new Hashtable<>();
        int writeLength = table.getTupleLength() + 1;
        byte[] tmpData = new byte[writeLength];
        int curPos = 1;
        int start = 0;
        int end = 0;

        for (int i = 0; i<writeLength; i++) tmpData[i] = -1;
        tmpData[0] = 1;
        for (int i = 0; i<table.getAttrNum(); i++) {
            end = values.indexOf(',', start);
            if (end == -1)
                end = values.length();
            String tmpValue = values.substring(start, end).trim();
            start = end + 1;
            Attribute attribute = table.getAttributes().get(i);
            switch (attribute.getType()) {
                case MYINT:
                    int tmpInt = Integer.parseInt(tmpValue);
                    attrTable.put(attribute.getName(), tmpValue);
                    System.arraycopy(ItoB(tmpInt), 0, tmpData, curPos, 4);
                    curPos += 4;
                    break;
                case MYFLOAT:
                    float tmpFloat = Float.parseFloat(tmpValue);
                    attrTable.put(attribute.getName(), tmpValue);
                    System.arraycopy(FtoB(tmpFloat), 0, tmpData, curPos, 4);
                    curPos += 4;
                    break;
                case MYCHAR:
                    String tmpString = tmpValue.substring(1, tmpValue.length()-1);
                    attrTable.put(attribute.getName(), tmpString);
                    System.arraycopy(tmpString.getBytes(), 0, tmpData, curPos, tmpString.length());
                    curPos += attribute.getLength();
                    break;
                default:break;
            }
        }

        if (validCheck(tmpData, table, im)) {
            insPos = bm.getInsertPosition(table);
            for (int i=0; i<writeLength; i++)
                bm.bufferArray[insPos.getBlockNum()].value[insPos.getBlockOfs() + i] = tmpData[i];
            table.addRecordNum();
            System.out.println(table.getRecordNum());
            int ofs = bm.bufferArray[insPos.getBlockNum()].blockOfs*Buffer.BLOCKSIZE + insPos.getBlockOfs();

            for (Attribute attr : table.getAttributes())  //insert in index
                if (!attr.getIndexName().equals("")) {
                    BPTree index = im.getIndex(attr.getIndexName());
                    index.insert(attrTable.get(attr.getName()), ofs);
                }

            return ofs;
        } else return -1;
    }

    public boolean validCheck(byte[] tmpData, Table table, IndexManager im) throws IOException {
        ArrayList<Attribute> attrs = table.getAttributes();
        int ofs = 1;
        for (Attribute attr : attrs) {  //search every attribute in the table and see whether there is a duplicate
            boolean isUnique = attr.isUnique(), isPrimary = attr.isPrimKey();
            byte[] tmpValue = new byte[attr.getLength()];
            System.arraycopy(tmpData,ofs,tmpValue,0,attr.getLength());
            String key = toString(tmpValue, attr.getType(), attr.getLength());
            ofs += attr.getLength();
            if (!(isUnique || isPrimary)) continue;
            if (attr.getIndexName().equals("")) continue;
            int res = im.getIndex(attr.getIndexName()).find(key);
            if (res != -1) {
                System.out.println("<---ATTRIBUTE "+attr.getName()+" IS UNIQUE!--->");
                return false;
            }
        }
        return true;
    }


    public byte[] ItoB(int i) {
        byte[] res = new byte[4];
        res[0] = (byte)((i>>24) & 0xFF);
        res[1] = (byte)((i>>16) & 0xFF);
        res[2] = (byte)((i>>8) & 0xFF);
        res[3] = (byte)(i & 0xFF);
        return res;
    }

    public byte[] FtoB(float f) {
        int num = Float.floatToIntBits(f);
        return ItoB(num);
    }

    public int BtoI(byte[] bytes) {
        int res;
        res = (bytes[0]<<24) | (bytes[1]<<16) | (bytes[2]<<8) | bytes[3]&0xFF;
        return res;
    }

    public float BtoF(byte[] bytes) {
        return Float.intBitsToFloat(BtoI(bytes));
    }

    public boolean judge(Condition condition, String value, DATA_TYPE type) {
        String condValue = condition.getValue();

        switch (condition.getRelationType()) {
            case EQUAL:
                return value.equals(condValue);
            case NOT_EQUAL:
                return !value.equals(condValue);
            case GREATER:
                switch (type) {
                    case MYINT:
                        return Integer.valueOf(value) > Integer.valueOf(condValue);
                    case MYFLOAT:
                        return Float.valueOf(value) > Float.valueOf(condValue);
                    case MYCHAR:
                        return (value.compareTo(condValue)>0);
                }
            case GREATER_EQUAL:
                switch (type) {
                    case MYINT:
                        return Integer.valueOf(value) >= Integer.valueOf(condValue);
                    case MYFLOAT:
                        return Float.valueOf(value) >= Float.valueOf(condValue);
                    case MYCHAR:
                        return (value.compareTo(condValue)>=0);
                }
            case SMALLER:
                switch (type) {
                    case MYINT:
                        return Integer.valueOf(value) < Integer.valueOf(condValue);
                    case MYFLOAT:
                        return Float.valueOf(value) < Float.valueOf(condValue);
                    case MYCHAR:
                        return (value.compareTo(condValue)<0);
                }
            case SMALLER_EQUAL:
                switch (type) {
                    case MYINT:
                        return Integer.valueOf(value) <= Integer.valueOf(condValue);
                    case MYFLOAT:
                        return Float.valueOf(value) <= Float.valueOf(condValue);
                    case MYCHAR:
                        return (value.compareTo(condValue)<=0);
                }
            default:return true;
        }
    }

}
