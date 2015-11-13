import structures.SQLstatement;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by Administrator on 2015/10/6.
 */
public class MiniSQL {
    public static void main(String args[]) throws Throwable {
        Controller controller = new Controller();
        Scanner in = new Scanner(System.in);
        String sqlOrder;
        BufferedReader bfr = new BufferedReader(new FileReader("./input.txt"));

        System.out.println("Welcome to MiniSQL!");
        System.out.println("*****************************************");
        System.out.println("*****************************************");
        System.out.println("Please input your sql strictly:");

        while (true) {
            String str;
            sqlOrder = "";
            while ((str = bfr.readLine())!=null) {
                sqlOrder += str.trim();
                if (str.contains(";")) break;
            }
            SQLstatement sqLstatement = new SQLstatement(sqlOrder);
            controller.executeSQL(sqLstatement);
        }
    }

}