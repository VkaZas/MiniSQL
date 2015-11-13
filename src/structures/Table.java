package structures;

import java.io.Serializable;
import java.util.ArrayList;

public class Table implements Serializable{

    private static final long serialVersionUID = 19940713L;

    String name;
    int blockNum;
    int recordNum;
    int attrNum;
    int tupleLength;
    String primKey;
    public ArrayList<Attribute> attributes;

    public Table() {
        blockNum = 0;
        recordNum = 0;
        attrNum = 0;
        tupleLength = 0;
    }

    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public Attribute getAttr(String attrName) {
        for (Attribute attribute : attributes) if (attribute.getName().equals(attrName)) return attribute;
        return null;
    }

    public int getAttrNum() {
        return attributes.size();
    }

    public int getBlockNum() {
        return blockNum;
    }

    public int getRecordNum() {
        return recordNum;
    }

    public int getTupleLength() {
        return tupleLength;
    }

    public String getName() {
        return name;
    }

    public String getPrimKey() {
        return primKey;
    }

    public void setBlockNum(int blockNum) {
        this.blockNum = blockNum;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAttributes(ArrayList<Attribute> attributes) {
        this.attributes = attributes;
    }

    public void setAttrNum(int attrNum) {
        this.attrNum = attrNum;
    }

    public void setPrimKey(String primKey) {
        this.primKey = primKey;
    }

    public void setRecordNum(int recordNum) {
        this.recordNum = recordNum;
    }

    public void setTupleLength(int tupleLength) {
        this.tupleLength = tupleLength;
    }

    public void addBlockNum() {
        this.blockNum++;
    }

    public void addRecordNum() {
        this.recordNum++;
    }

    public void decRecordNum() {
        this.recordNum--;
    }
}
