/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.servicelibs.canteen.it;

import com.google.common.base.Strings;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class Main {
    private Main() { }

    private static final int THE_ANSWER_TO_EVERYTHING = 42;

    public static void main(String[] args) throws Exception {
        String choice;

        if (args.length == 1) {
            choice = args[0];
        } else {
            System.out.print("1 = Success, 0 = Failure >" + Strings.repeat(" ", 2));
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            choice = br.readLine();
        }

        if (choice.equals("1")) {
            System.out.println("success");
            System.exit(0);
        } else {
            System.err.println("failure");
            System.exit(THE_ANSWER_TO_EVERYTHING);
        }
    }
}
