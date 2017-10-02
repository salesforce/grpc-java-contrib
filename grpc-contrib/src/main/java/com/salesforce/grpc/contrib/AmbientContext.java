/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import io.grpc.*;

public class AmbientContext {
    public static final Context.Key<Metadata> KEY = Context.key("AmbientContext");

    public static Metadata get() {
        return KEY.get();
    }
}
