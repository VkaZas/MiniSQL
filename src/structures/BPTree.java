package structures;

import com.sun.istack.internal.Nullable;

import java.io.Serializable;
import java.util.ArrayList;

class Node implements Serializable{

    private static final long serialVersionUID = 19940715L;

    ArrayList<String> keys = new ArrayList<>();
    ArrayList<Node> children = new ArrayList<>();
    Node father;
    int childNum;

    Node() {
       childNum = 0;
       for (int i=0; i<BPTree.ORDER; i++) {
           keys.add(null);
           children.add(null);
       }
    }

    public void setChildNum(int childNum) {
        this.childNum = childNum;
    }

    public void setFather(Node father) {
        this.father = father;
    }
}

class LeafNode extends Node {
    ArrayList<Integer> offsets = new ArrayList<>();

    LeafNode() {
        super();
        childNum = 0;
        for (int i=0; i<BPTree.ORDER; i++) offsets.add(-1);
    }
    LeafNode(Integer ofs, String ele) {
        super();
        childNum = 1;
        for (int i=0; i<BPTree.ORDER; i++) offsets.add(-1);
        offsets.set(0, ofs);
        keys.set(0, ele);
    }
}

public class BPTree implements Serializable{

    private static final long serialVersionUID = 19940715L;

    public final static int ORDER = 21;
    public final static int LEFT = (ORDER + 1) / 2;
    public final static int RIGHT = ORDER + 1 - LEFT;
    public final static int LIMIT = (ORDER + 1) /2;
    public Node root;
    public String indexName;
    public DATA_TYPE type;
    public Attribute belongsToAttr;

    public BPTree() {
        root = new Node();
    }

    public Integer find(String ele) {
        return find(root, ele);
    }

    private Integer find(Node node, String ele) {
        if (node.childNum == 0) return -1;
        if (node instanceof LeafNode) {
            for (int i=0; i<node.childNum; i++)
                if (node.keys.get(i).equals(ele)) return ((LeafNode) node).offsets.get(i);
            return -1;
        } else {
            for (int i=1; i<node.childNum; i++)
                if (compareTo(node.keys.get(i),ele) == 1) return find(node.children.get(i-1), ele);
            return find(node.children.get(node.childNum-1), ele);
        }
    }

    public void insert(String ele, Integer ofs) {  // make sure find(ele) == -1
        if (root.childNum == 0) {
            root.children.set(0, new LeafNode(ofs, ele));
            root.childNum++;
            root.keys.set(0, ele);
            root.children.get(0).setFather(root);
            return;
        }
        Node newNode = insert(root, ele, ofs);
        if (newNode == null) return;
        Node tmpNode = new Node();
        tmpNode.setChildNum(2);
        tmpNode.keys.set(0, root.keys.get(0));
        tmpNode.keys.set(1, newNode.keys.get(0));
        tmpNode.children.set(0, root);
        tmpNode.children.set(1, newNode);
        root = tmpNode;
        root.children.get(0).setFather(root);
        root.children.get(1).setFather(root);
    }

    @Nullable
    private Node insert(Node node, String ele, Integer ofs) {
        int index = findInsertIndex(node, ele);
        if (node instanceof LeafNode) {
            if (node.childNum == ORDER) {
                LeafNode resNode = new LeafNode();
                if (index < LEFT) {  // insert to left part
                    copyArray(node.keys, LEFT - 1, resNode.keys, 0, LEFT);
                    copyArray(((LeafNode) node).offsets, LEFT - 1, resNode.offsets, 0, LEFT);
                    shiftInsertArray(node.keys, LEFT, index, ele);
                    shiftInsertArray(((LeafNode) node).offsets, LEFT, index, ofs);
                } else {  // insert to right part
                    copyArray(node.keys, LEFT, resNode.keys, 0, ORDER-LEFT);
                    copyArray(((LeafNode) node).offsets, LEFT, resNode.offsets, 0, ORDER-LEFT);
                    shiftInsertArray(resNode.keys, RIGHT, index - LEFT, ele);
                    shiftInsertArray(resNode.offsets, RIGHT, index - LEFT,  ofs);
                }
                node.setChildNum(LEFT);
                resNode.setChildNum(RIGHT);
                return resNode;
            } else {
                node.childNum++;
                shiftInsertArray(node.keys, node.childNum, index, ele);
                shiftInsertArray(((LeafNode) node).offsets, node.childNum, index, ofs);
                return null;
            }
        } else {
            Node newNode = insert(node.children.get(index), ele, ofs);
            if (newNode == null) {  //inserted to one of children, and no new node
                upDateKey(node, index);
                return null;
            }
            //there's a new node
            if (compareTo(newNode.keys.get(0), node.children.get(index).keys.get(0)) > 0) index++;
            if (node.childNum == ORDER) {  
                Node resNode = new Node();
                if (index < LEFT) {
                    copyArray(node.keys, LEFT - 1, resNode.keys, 0, LEFT);
                    copyArray(node.children, LEFT - 1, resNode.children, 0, LEFT);
                    shiftInsertArray(node.keys, LEFT, index, newNode.keys.get(0));
                    shiftInsertArray(node.children, LEFT, index, newNode);
                    for (int i=0; i<LEFT; i++) resNode.children.get(i).setFather(resNode);
                } else {
                    copyArray(node.keys, LEFT, resNode.keys, 0, ORDER-LEFT);
                    copyArray(node.children, LEFT, resNode.children, 0, ORDER-LEFT);
                    shiftInsertArray(resNode.keys, RIGHT, index - LEFT, newNode.keys.get(0));
                    shiftInsertArray(resNode.children, RIGHT, index - LEFT,  newNode);
                    for (int i=0; i<RIGHT; i++) resNode.children.get(i).setFather(resNode);
                }
                node.setChildNum(LEFT);
                resNode.setChildNum(RIGHT);
                return resNode;
            } else {
                node.childNum++;
                newNode.setFather(node);
                shiftInsertArray(node.keys, node.childNum, index, newNode.keys.get(0));
                shiftInsertArray(node.children, node.childNum, index, newNode);
                return null;
            }
        }
    }

    public void delete(String ele) {
        if (root.childNum == 1 && root.children.get(0).childNum == 1) {
            root.childNum = 0;
            return;
        }
        Node delNode = delete(root, ele);
        if (delNode != null) {
            if (delNode.keys.get(0).equals(root.children.get(0).keys.get(0))) root = root.children.get(1);
            else root = root.children.get(0);
        }

    }

    @Nullable
    private Node delete(Node node, String ele) {
        int index = findDeleteIndex(node, ele);
        if (node instanceof LeafNode) {
            LeafNode leafNode = (LeafNode) node;
            if (node.childNum <= LIMIT) {  //  'less than' is possible at the beginning of building the tree
                LeafNode leftNode = (LeafNode) getLeft(leafNode);
                LeafNode rightNode = (LeafNode) getRight(leafNode);

                leafNode.childNum--;
                shiftDeleteArray(leafNode.keys, leafNode.childNum, index);
                shiftDeleteArray(leafNode.offsets, leafNode.childNum, index);

                if (leftNode != null) {
                    if (leftNode.childNum > LIMIT) {  //  lend one key from left
                        leafNode.childNum++;
                        shiftInsertArray(leafNode.keys, leafNode.childNum, 0, leftNode.keys.get(leftNode.childNum-1));
                        shiftInsertArray(leafNode.offsets, leafNode.childNum, 0, leftNode.offsets.get(leftNode.childNum-1));
                        leftNode.childNum--;
                        return null;
                    } else {  //  union with left
                        copyArray(leafNode.keys, 0, leftNode.keys, leftNode.childNum, leafNode.childNum);
                        copyArray(leafNode.offsets, 0, leftNode.offsets, leftNode.childNum, leafNode.childNum);
                        leftNode.childNum += leafNode.childNum;
                        return leafNode;
                    }
                }
                else if (rightNode != null) {  //  lend one key from right
                    if (rightNode.childNum > LIMIT) {
                        leafNode.childNum++;
                        shiftInsertArray(leafNode.keys, leafNode.childNum, leafNode.childNum-1, rightNode.keys.get(0));
                        shiftInsertArray(leafNode.offsets, leafNode.childNum, leafNode.childNum-1, rightNode.offsets.get(0));
                        rightNode.childNum--;
                        shiftDeleteArray(rightNode.keys, rightNode.childNum, 0);
                        shiftDeleteArray(rightNode.offsets, rightNode.childNum, 0);
                        return null;
                    } else {  //  union with right (do not return original node)
                        copyArray(rightNode.keys, 0, leafNode.keys, leafNode.childNum, rightNode.childNum);
                        copyArray(rightNode.offsets, 0, leafNode.offsets, leafNode.childNum, rightNode.childNum);
                        leafNode.childNum += rightNode.childNum;
                        return rightNode;
                    }
                } else {  //  root with one leafNode
                    if (leafNode.childNum == 0) return leafNode;
                    return null;
                }
            } else {
                leafNode.childNum--;
                shiftDeleteArray(leafNode.keys, leafNode.childNum, index);
                shiftDeleteArray(leafNode.offsets, leafNode.childNum, index);
                return null;
            }
        } else {  // none leafNode
            Node delNode = delete(node.children.get(index), ele);
            if (delNode == null) {
                upDateKey(node, index);
                if (index < node.childNum-1) upDateKey(node, index+1);
                return null;
            }
            // there is a deleted node
            if (!node.children.get(index).equals(delNode)) index++;

            if (node.childNum <= LIMIT) {  //'less than' is theoretically impossible
                Node leftNode = getLeft(node);
                Node rightNode = getRight(node);

                node.childNum--;
                shiftDeleteArray(node.keys, node.childNum, index);
                shiftDeleteArray(node.children, node.childNum, index);

                if (leftNode != null) {
                    if (leftNode.childNum > LIMIT) {  //  lend one key from left
                        node.childNum++;
                        shiftInsertArray(node.keys, node.childNum, 0, leftNode.keys.get(leftNode.childNum-1));
                        shiftInsertArray(node.children, node.childNum, 0, leftNode.children.get(leftNode.childNum-1));
                        node.children.get(0).setFather(node);
                        leftNode.childNum--;
                        return null;
                    } else {  //  union with left
                        copyArray(node.keys, 0, leftNode.keys, leftNode.childNum, node.childNum);
                        copyArray(node.children, 0, leftNode.children, leftNode.childNum, node.childNum);
                        leftNode.childNum += node.childNum;
                        for (int i=0; i<leftNode.childNum; i++) leftNode.children.get(i).setFather(leftNode);
                        return node;
                    }
                }
                else if (rightNode != null) {  //  lend one key from right
                    if (rightNode.childNum > LIMIT) {
                        node.childNum++;
                        shiftInsertArray(node.keys, node.childNum, node.childNum-1, rightNode.keys.get(0));
                        shiftInsertArray(node.children, node.childNum, node.childNum-1, rightNode.children.get(0));
                        node.children.get(node.childNum-1).setFather(node);
                        rightNode.childNum--;
                        shiftDeleteArray(rightNode.keys, rightNode.childNum, 0);
                        shiftDeleteArray(rightNode.children, rightNode.childNum, 0);
                        return null;
                    } else {  //  union with right (do not return original node)
                        copyArray(rightNode.keys, 0, node.keys, node.childNum, rightNode.childNum);
                        copyArray(rightNode.children, 0, node.children, node.childNum, rightNode.childNum);
                        node.childNum += rightNode.childNum;
                        for (int i=0; i<node.childNum; i++) node.children.get(i).setFather(node);
                        return rightNode;
                    }
                }
                else {  // root
                    if (node.childNum == 0) return node;
                    return null;
                }
            } else {
                node.childNum--;
                shiftDeleteArray(node.keys, node.childNum, index);
                shiftDeleteArray(node.children, node.childNum, index);
                return null;
            }
        }
    }

//    private int compare(T ele1, T ele2) {
//        if (ele1 instanceof Integer) return ((Integer) ele1).compareTo((Integer) ele2);
//        else if (ele1 instanceof Float) return ((Float) ele1).compareTo((Float) ele2);
//        else return ((String) ele1).compareTo((String) ele2);
//    }

    private void upDateKey(Node node, int index) {
        node.keys.set(index, node.children.get(index).keys.get(0));
    }

    private int findInsertIndex(Node node, String ele) {
        boolean isLeaf = node instanceof LeafNode;
        if (!isLeaf) {
            for (int i=1; i<node.childNum; i++)
                if (compareTo(node.keys.get(i), ele) > 0) return i - 1;
            return node.childNum - 1;
        } else {
            for (int i=0; i<node.childNum; i++)
                if (compareTo(node.keys.get(i), ele) > 0) return i;
            return node.childNum;
        }
    }

    private int findDeleteIndex(Node node, String ele) {
        for (int i=1; i<node.childNum; i++)
            if (compareTo(node.keys.get(i),ele) > 0) return i - 1;
        return node.childNum - 1;
    }

    private static <K> void copyArray(ArrayList<K> src, int srcPos, ArrayList<K> des, int desPos, int length) {
        for (int i=0; i<length; i++) des.set(i+desPos, src.get(i+srcPos));
    }

    private static <K> void shiftInsertArray(ArrayList<K> arr, int length, int index, K ele) {  //length is the length of array after insertion
        for (int i=length-1; i>index; i--) arr.set(i, arr.get(i-1));
        arr.set(index, ele);
    }
    
    private static <K> void shiftDeleteArray(ArrayList<K> arr, int length, int index) {  //length is the length of array after deletion
        for (int i=index; i<length; i++) arr.set(i, arr.get(i+1));
    }

    private Node getLeft(Node node) {
        Node father = node.father;
        for (int i=0; i<father.childNum;i++)
            if (father.children.get(i).equals(node))
                return i==0 ? null : father.children.get(i-1);
        return null;  //impossible
    }

    private Node getRight(Node node) {
        Node father = node.father;
        for (int i=0; i<father.childNum;i++)
            if (father.children.get(i).equals(node))
                return i==father.childNum-1 ? null : father.children.get(i+1);
        return null;  //impossible
    }

    int compareTo(String str1, String str2) {
        switch (type) {
            case MYINT:
                return Integer.valueOf(str1).compareTo(Integer.valueOf(str2));
            case MYFLOAT:
                return Float.valueOf(str1).compareTo(Float.valueOf(str2));
            case MYCHAR:
                return str1.compareTo(str2);
            default:
                return 0; //impossible
        }
    }

    public void printTree(Node node) {
        for (int i=0; i<node.childNum; i++) {
            System.out.print(" | " + (node.keys.get(i)));
        }
        for (int i=node.childNum; i<5; i++) System.out.print(" | -1" );
        System.out.println();
        for (int i=0; i<5; i++) if (node.children.get(i)!=null) printTree(node.children.get(i));
        System.out.println("<---NODE END--->");
    }
}
