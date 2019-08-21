package com.salesforce.servicelibs.canteen.it;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws Exception {
        String choice;

        if (args.length == 1) {
            choice = args[0];
        } else {
            System.out.print("1 = Success, 0 = Failure > ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            choice = br.readLine();
        }

        if (choice.equals("1")) {
            System.out.println("success");
            System.exit(0);
        } else {
            System.err.println("failure");
            System.exit(42);
        }
    }
}
