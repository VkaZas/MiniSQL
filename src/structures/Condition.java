package structures;

/**
 * Created by Administrator on 2015/10/6.
 */
public class Condition {
    String attrName;
    String value;
    RELATION_TYPE relationType;

    public Condition() {}


    public RELATION_TYPE getRelationType() {
        return relationType;
    }

    public String getValue() {
        return value;
    }

    public void setRelationType(RELATION_TYPE relationType) {
        this.relationType = relationType;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }
}
