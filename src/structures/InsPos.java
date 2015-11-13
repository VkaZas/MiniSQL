package structures;

/**
 * Created by Administrator on 2015/10/6.
 */
public class InsPos {
    int blockNum;
    int blockOfs;

    public InsPos() {blockNum = 0; blockOfs = 0;}

    public int getBlockNum() {
        return blockNum;
    }

    public int getBlockOfs() {
        return blockOfs;
    }

    public void setBlockNum(int blockNum) {
        this.blockNum = blockNum;
    }

    public void setBlockOfs(int blockOfs) {
        this.blockOfs = blockOfs;
    }
}
