package structures;

import java.io.Serializable;

/**
 * Created by Administrator on 2015/10/6.
 */
public class Attribute implements Serializable{

    private static final long serialVersionUID = 19940714L;

    String name;
    String indexName;
    DATA_TYPE type;
    boolean isPrimKey;
    boolean isUnique;
    int length;

    public Attribute () {
        name = "";
        indexName = "";
        type = DATA_TYPE.MYINT;
        isPrimKey = false;
        isUnique = false;
    }

    public Attribute (String attrName) {
        name = attrName;
        indexName = "";
        type = DATA_TYPE.MYINT;
        isPrimKey = false;
        isUnique = false;
    }

    public DATA_TYPE getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public boolean isPrimKey() {
        return isPrimKey;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getName() {
        return name;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public void setIsPrimKey(boolean isPrimKey) {
        this.isPrimKey = isPrimKey;
    }

    public void setIsUnique(boolean isUnique) {
        this.isUnique = isUnique;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(DATA_TYPE type) {
        this.type = type;
    }
}
