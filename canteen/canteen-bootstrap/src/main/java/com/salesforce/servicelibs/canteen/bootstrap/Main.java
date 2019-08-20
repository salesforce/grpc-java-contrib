package com.salesforce.servicelibs.canteen.bootstrap;

public class Main {
    public static void main(String[] args) {
        String choice;

        if (args.length == 1) {
            choice = args[0];
        } else {
            System.out.print("0 = Success, 1 = Failure > ");
            choice = System.console().readLine();
        }

        if (choice.equals("0")) {
            System.out.println("success");
            System.exit(0);
        } else {
            System.err.println("failure");
            System.exit(42);
        }
    }
}
