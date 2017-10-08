/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.grpc.*;

/**
 * {@code AmbientContext} is entry point for working with the ambient context managed by {@link AmbientContextClientInterceptor}
 * and {@link AmbientContextServerInterceptor}.
 *
 * See package javadoc for more info.
 */
public final class AmbientContext {
    private AmbientContext() { }

    static final Context.Key<Metadata> KEY = Context.key("AmbientContext");

    /**
     * Attaches an empty ambient context to the provided gRPC {@code Context}.
     *
     * @throws IllegalStateException  if an ambient context has already been attached to the
     * provided gRPC {@code Context}.
     */
    public static Context initialize(Context context) {
        Preconditions.checkNotNull(context, "context");
        Preconditions.checkState(KEY.get(context) == null,
                "AmbientContext has already been created in the scope of the current context");
        return context.withValue(KEY, new Metadata());
    }

    /**
     * Returns the ambient context attached to the current gRPC {@code Context}.
     *
     * @throws  IllegalStateException  if no ambient context is attached to the current gRPC {@code Context}.
     */
    public static Metadata current() {
        return current(Context.current());
    }

    @VisibleForTesting
    static Metadata current(Context context) {
        Preconditions.checkNotNull(context, "context");
        Preconditions.checkState(KEY.get(context) != null,
                "AmbientContext has not yet been created in the scope of the current context");

        return KEY.get(context);
    }
}
