package com.salesforce.servicelibs.canteen.it;

public class Main {
    public static void main(String[] args) {
        String choice;

        if (args.length == 1) {
            choice = args[0];
        } else {
            System.out.print("1 = Success, 0 = Failure > ");
            choice = System.console().readLine();
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
