/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import com.google.common.base.Preconditions;
import io.grpc.Context;
import io.grpc.Metadata;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Set;

/**
 * {@code AmbientContext} is entry point for working with the ambient context managed by {@link AmbientContextClientInterceptor}
 * and {@link AmbientContextServerInterceptor}. The interface for this class is very similar to gRPC's {@code Metadata}
 * class.
 *
 * <p>This class is not thread safe, implementations should ensure that ambient context reads and writes do
 * not occur in multiple threads concurrently.
 *
 * <p>See package javadoc for more info.
 */
@NotThreadSafe
public final class AmbientContext {
    private AmbientContext() { }

    static final Context.Key<Metadata> DATA_KEY = Context.key("AmbientContext");
    private static final Metadata.Key<String> FREEZE_KEY =
            Metadata.Key.of("com.salesforce.grpc.contrib.context.frozen", Metadata.ASCII_STRING_MARSHALLER);

    private static final AmbientContext instance = new AmbientContext();

    /**
     * Attaches an empty ambient context to the provided gRPC {@code Context}.
     *
     * @throws IllegalStateException  if an ambient context has already been attached to the
     * provided gRPC {@code Context}.
     */
    public static Context initialize(Context context) {
        Preconditions.checkNotNull(context, "context");
        Preconditions.checkState(DATA_KEY.get(context) == null,
                "AmbientContext has already been created in the scope of the current context");
        return context.withValue(DATA_KEY, new Metadata());
    }

    /**
     * Returns the ambient context attached to the current gRPC {@code Context}.
     *
     * @throws  IllegalStateException  if no ambient context is attached to the current gRPC {@code Context}.
     */
    public static AmbientContext current() {
        internalCurrent();
        return instance;
    }

    private static Metadata internalCurrent() {
        Preconditions.checkState(DATA_KEY.get() != null,
                "AmbientContext has not yet been created in the scope of the current context");
        return DATA_KEY.get();
    }



    /**
     * Returns true if a value is defined for the given key.
     *
     * <p>This is done by linear search, so if it is followed by {@link #get} or {@link #getAll},
     * prefer calling them directly and checking the return value against {@code null}.
     */
    public boolean containsKey(Metadata.Key<?> key) {
        return internalCurrent().containsKey(key);
    }

    /**
     * Remove all values for the given key without returning them. This is a minor performance
     * optimization if you do not need the previous values.
     */
    public <T> void discardAll(Metadata.Key<T> key) {
        internalCurrent().discardAll(key);
    }

    /**
     * Returns the last ambient context entry added with the name 'name' parsed as T.
     *
     * @return the parsed metadata entry or null if there are none.
     */
    @Nullable
    public <T> T get(Metadata.Key<T> key) {
        return internalCurrent().get(key);
    }

    /**
     * Returns all the ambient context entries named 'name', in the order they were received, parsed as T, or
     * null if there are none. The iterator is not guaranteed to be "live." It may or may not be
     * accurate if the ambient context is mutated.
     */
    @Nullable
    public <T> Iterable<T> getAll(final Metadata.Key<T> key) {
        return internalCurrent().getAll(key);
    }

    /**
     * Returns set of all keys in store.
     *
     * @return unmodifiable Set of keys
     */
    public Set<String> keys() {
        return internalCurrent().keys();
    }

    /**
     * Adds the {@code key, value} pair. If {@code key} already has values, {@code value} is added to
     * the end. Duplicate values for the same key are permitted.
     *
     * @throws NullPointerException if key or value is null
     */
    public <T> void put(Metadata.Key<T> key, T value) {
        internalCurrent().put(key, value);
    }

    /**
     * Removes the first occurrence of {@code value} for {@code key}.
     *
     * @param key key for value
     * @param value value
     * @return {@code true} if {@code value} removed; {@code false} if {@code value} was not present
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    public <T> boolean remove(Metadata.Key<T> key, T value) {
        return internalCurrent().remove(key, value);
    }

    /**
     * Remove all values for the given key. If there were no values, {@code null} is returned.
     */
    public <T> Iterable<T> removeAll(Metadata.Key<T> key) {
        return internalCurrent().removeAll(key);
    }

    @Override
    public String toString() {
        Metadata ctx = DATA_KEY.get();
        if (ctx != null) {
            return ctx.toString();
        } else {
            return "[MISSING AMBIENT CONTEXT]";
        }
    }
}
