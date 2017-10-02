/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.*;

/**
 * TODO.
 */
public final class AmbientContext {
    private AmbientContext() { }

    static final Context.Key<Metadata> KEY = Context.key("AmbientContext");

    /**
     * TODO.
     * @return
     */
    public static Context initialize(Context context) {
        return context.withValue(KEY, new Metadata());
    }

    /**
     * TODO.
     * @return
     */
    public static Metadata current() {
        return current(Context.current());
    }

    @VisibleForTesting
    static Metadata current(Context context) {
        Metadata current = KEY.get(context);
        if (current == null) {
            throw new IllegalStateException("AmbientContext has not yet been created in the scope of the current context");
        } else {
            return KEY.get();
        }
    }
}
