import structures.InsPos;
import structures.Table;


import java.io.*;

class Buffer {
    public final static int BLOCKSIZE = 4096;
    public final static byte EMPTY = -1;

    String fileName;
    int blockOfs;
    int LRU;
    public byte[] value = new byte[BLOCKSIZE + 1];
    boolean isValid;
    boolean isWritten;

    public void initialize() {
        isValid = false;
        isWritten = false;
        LRU = 0;
        blockOfs = 0;
        fileName = "";
        for (int i = 0; i<=BLOCKSIZE; i++) { value[i] = EMPTY; }
    }

    public byte getValue(int pos) {
        if (pos>=0 && pos<BLOCKSIZE) return value[pos];
        return -1;
    }

    public int getBlockOfs() {
        return blockOfs;
    }

    public String getFileName() {
        return fileName;
    }
}

public class BufferManager {
    public final static int MAXBLOCKNUM = 1024*10;

    Buffer[] bufferArray = new Buffer[MAXBLOCKNUM];

    public BufferManager() {
        for (int i = 0; i<MAXBLOCKNUM; i++) {
            bufferArray[i] = new Buffer();
            bufferArray[i].initialize();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        for (int i = 0; i<MAXBLOCKNUM; i++) writeBack(i);
        super.finalize();
    }

     void writeBack(int bufferNum) throws IOException {
         Buffer tmpBuffer = bufferArray[bufferNum];
         if (!tmpBuffer.isWritten || !tmpBuffer.isValid) return;

         File file = new File("./buffer/" + tmpBuffer.fileName);
         if (!file.exists()) file.createNewFile();
         RandomAccessFile raf = new RandomAccessFile( file, "rw");
         raf.seek(Buffer.BLOCKSIZE * tmpBuffer.blockOfs);
         raf.write(tmpBuffer.value, 0, Buffer.BLOCKSIZE);
         raf.close();
         bufferArray[bufferNum].initialize();
    }

    public int getBufferNum(String fileName, int blockOfs) throws IOException {
        int i = getBufferNumExist(fileName,blockOfs);
        if (i == -1) {
            i = getEmptyBufferExcept(fileName);
            readBlock(fileName, blockOfs, i);
        }
        return i;
    }

    public int getBufferNumExist(String fileName, int blockOfs) {
        for (int i=0; i<MAXBLOCKNUM; i++)
            if (bufferArray[i].fileName.equals(fileName) && bufferArray[i].blockOfs == blockOfs) return i;
        return -1;
    }

    public int getEmptyBuffer() throws IOException {
        int bfNum = 0;
        int maxLRU = bufferArray[0].LRU;
        for (int i = 0; i<MAXBLOCKNUM; i++) {
            if (!bufferArray[i].isValid) {
                bufferArray[i].initialize();
                bufferArray[i].isValid = true;
                return i;
            } else if (maxLRU < bufferArray[i].LRU) {
                maxLRU = bufferArray[i].LRU;
                bfNum = i;
            }
        }
        writeBack(bfNum);
        bufferArray[bfNum].isValid = true;
        return bfNum;
    }

    public int getEmptyBufferExcept(String fileName) throws IOException {
        int bfNum = -1;
        int maxLRU = bufferArray[0].LRU;
        for (int i = 0; i<MAXBLOCKNUM; i++) {
            if (!bufferArray[i].isValid) {
                bufferArray[i].initialize();
                bufferArray[i].isValid = true;
                return i;
            } else if (maxLRU < bufferArray[i].LRU && !bufferArray[i].fileName.equals(fileName) ) {
                maxLRU = bufferArray[i].LRU;
                bfNum = i;
            }
        }
        if (bfNum == -1) {
            System.out.println("Need more space for buffer!");
            return 0;
        }
        writeBack(bfNum);
        bufferArray[bfNum].isValid = true;
        return bfNum;
    }

    public void readBlock(String fileName, int blockOfs, int bfNum) throws IOException {
        bufferArray[bfNum].isValid = true;
        bufferArray[bfNum].isWritten = false;
        bufferArray[bfNum].fileName = fileName;
        bufferArray[bfNum].blockOfs = blockOfs;
        File file = new File("./buffer/" + fileName);
        if (!file.exists()) file.createNewFile();
        RandomAccessFile raf = new RandomAccessFile(file , "r");
        raf.seek(Buffer.BLOCKSIZE * blockOfs);
        raf.read(bufferArray[bfNum].value, 0, Buffer.BLOCKSIZE);
        raf.close();
    }

    public void writeBlock(int bfNum) {
        bufferArray[bfNum].isWritten = true;
        LRU(bfNum);
    }

    public void LRU(int bfNum) {
        for (int i = 0; i<MAXBLOCKNUM; i++) {
            if (i == bfNum) {
                bufferArray[bfNum].LRU = 0;
                bufferArray[i].isValid = true;
            } else {
                bufferArray[bfNum].LRU++;
            }
        }
    }

    public int addBlockInFile(Table fileInfo) throws IOException {
        int bfNum = getEmptyBuffer();
        bufferArray[bfNum].initialize();
        bufferArray[bfNum].isValid = true;
        bufferArray[bfNum].isWritten = true;
        bufferArray[bfNum].fileName = fileInfo.getName() + ".table";
        bufferArray[bfNum].blockOfs = fileInfo.getBlockNum();
        fileInfo.addBlockNum();
        return bfNum;
    }

    public InsPos getInsertPosition(Table fileInfo) throws IOException {
        InsPos insPos = new InsPos();
        if (fileInfo.getBlockNum() == 0) {
            insPos.setBlockNum(addBlockInFile(fileInfo));
            insPos.setBlockOfs(0);
            writeBlock(insPos.getBlockNum());
            return insPos;
        }
        String fileName = fileInfo.getName() + ".table";
        int length = fileInfo.getTupleLength() + 1;
        int blockOfs = fileInfo.getBlockNum() - 1;
        int bfNum = getBufferNumExist(fileName, blockOfs);
        if (bfNum == -1) {
            bfNum = getEmptyBuffer();
            readBlock(fileName, blockOfs, bfNum);
        }
        final int recordNum = Buffer.BLOCKSIZE / length;
        for (int ofs = 0; ofs < recordNum; ofs++) {
            int position = ofs * length;
            byte isEmpty = bufferArray[bfNum].value[position];
            if (isEmpty == Buffer.EMPTY) {
                insPos.setBlockNum(bfNum);
                insPos.setBlockOfs(position);
                writeBlock(insPos.getBlockNum());
                return insPos;
            }
        }
        insPos.setBlockNum(addBlockInFile(fileInfo));
        insPos.setBlockOfs(0);
        writeBlock(insPos.getBlockNum());
        return  insPos;
    }

    public void scanTable(Table fileInfo) throws IOException {
        String fileName = fileInfo.getName() + ".table";
        for (int blockOfs = 0; blockOfs<fileInfo.getBlockNum(); blockOfs++) {
            if (getBufferNumExist(fileName, blockOfs) == -1) {
                int bfNum = getEmptyBufferExcept(fileName);
                readBlock(fileName, blockOfs, bfNum);
            }
        }
    }

    public void freeTable(String fileName) {
        for (int i = 0; i<MAXBLOCKNUM; i++) {
            if (bufferArray[i].fileName.equals(fileName)) {
                bufferArray[i].isValid = false;
                bufferArray[i].isWritten = false;
            }
        }
    }
}
