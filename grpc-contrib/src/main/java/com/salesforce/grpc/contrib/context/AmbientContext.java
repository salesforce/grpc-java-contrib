/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.Context;
import io.grpc.Metadata;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Set;

import static com.google.common.base.Preconditions.*;


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
    static final Context.Key<AmbientContext> DATA_KEY = Context.key("AmbientContext");

    /**
     * Attaches an empty ambient context to the provided gRPC {@code Context}.
     *
     * @throws IllegalStateException  if an ambient context has already been attached to the
     * provided gRPC {@code Context}.
     */
    public static Context initialize(Context context) {
        checkNotNull(context, "context");
        checkState(DATA_KEY.get(context) == null,
                "AmbientContext has already been created in the scope of the current context");
        return context.withValue(DATA_KEY, new AmbientContext());
    }

    /**
     * Returns the ambient context attached to the current gRPC {@code Context}.
     *
     * @throws  IllegalStateException  if no ambient context is attached to the current gRPC {@code Context}.
     */
    public static AmbientContext current() {
        checkState(DATA_KEY.get() != null,
                "AmbientContext has not yet been created in the scope of the current context");
        return DATA_KEY.get();
    }

    /**
     * @return true if an {@code AmbientContext} is attached to the current gRPC context.
     */
    public static boolean isPresent() {
        return DATA_KEY.get() != null;
    }

    private Metadata contextMetadata;
    private Object freezeKey = null;

    AmbientContext() {
        this.contextMetadata = new Metadata();
    }

    /**
     * Copy constructor.
     */
    AmbientContext(AmbientContext other) {
        this();
        this.contextMetadata.merge(other.contextMetadata);
    }

    /**
     * Makes the AmbientContext as read-only, preventing any further modification. A "freeze key" is returned, which
     * can be used to {@link #thaw(Object)} the AmbientContext in the future.
     *
     * <p>{@code freeze()} and {@code thaw()} are typically used to mark the ambient context read-only when the
     * interceptor chain completes.
     *
     * @return  a "freeze key" that can be used passed to {@link #thaw(Object)}
     *
     * @throws IllegalStateException if the AmbientContext is already frozen
     */
    public Object freeze() {
        checkState(!isFrozen(), "AmbientContext already frozen. Cannot freeze() twice.");
        freezeKey = new Object();
        return freezeKey;
    }

    /**
     * Makes the AmbientContext mutable again, after {@link #freeze()} has been called. A "freeze key" is needed to
     * unfreeze the AmbientContext, ensuring only the code that froze the context can subsequently thaw it.
     *
     * <p>{@code freeze()} and {@code thaw()} are typically used to mark the ambient context read-only when the
     * interceptor chain completes.
     *
     * @param freezeKey the "freeze key" returned by {@link #freeze()}
     *
     * @throws IllegalStateException if the AmbientContext has not yet been frozen
     * @throws IllegalArgumentException if the {@code freezeKey} is incorrect
     */
    public void thaw(Object freezeKey) {
        checkState(isFrozen(), "AmbientContext is not frozen. Cannot thaw().");
        checkArgument(this.freezeKey == freezeKey,
                "The provided freezeKey is not the same object returned by freeze()");
        this.freezeKey = null;
    }

    /**
     * Similar to {@link #initialize(Context)}, {@code fork()} attaches a shallow clone of this {@code AmbientContext}
     * to a provided gRPC {@code Context}. Use {@code fork()} when you want create a temporary context scope.
     *
     * @param context
     * @return
     */
    public Context fork(Context context) {
        return context.withValue(DATA_KEY, new AmbientContext(this));
    }

    /**
     * @return true of the AmbientContext has been frozen
     */
    public boolean isFrozen() {
        return freezeKey != null;
    }

    private void checkFreeze() {
        checkState(freezeKey == null, "AmbientContext cannot be modified while frozen");
    }

    /**
     * Returns true if a value is defined for the given key.
     *
     * <p>This is done by linear search, so if it is followed by {@link #get} or {@link #getAll},
     * prefer calling them directly and checking the return value against {@code null}.
     */
    public boolean containsKey(Metadata.Key<?> key) {
        return contextMetadata.containsKey(key);
    }

    /**
     * Remove all values for the given key without returning them. This is a minor performance
     * optimization if you do not need the previous values.
     *
     * @throws IllegalStateException  if the AmbientContext is frozen
     */
    public <T> void discardAll(Metadata.Key<T> key) {
        checkFreeze();
        contextMetadata.discardAll(key);
    }

    /**
     * Returns the last ambient context entry added with the name 'name' parsed as T.
     *
     * @return the parsed metadata entry or null if there are none.
     */
    @Nullable
    public <T> T get(Metadata.Key<T> key) {
        return contextMetadata.get(key);
    }

    /**
     * Returns all the ambient context entries named 'name', in the order they were received, parsed as T, or
     * null if there are none. The iterator is not guaranteed to be "live." It may or may not be
     * accurate if the ambient context is mutated.
     */
    @Nullable
    public <T> Iterable<T> getAll(final Metadata.Key<T> key) {
        return contextMetadata.getAll(key);
    }

    /**
     * Returns set of all keys in store.
     *
     * @return unmodifiable Set of keys
     */
    public Set<String> keys() {
        return contextMetadata.keys();
    }

    /**
     * Adds the {@code key, value} pair. If {@code key} already has values, {@code value} is added to
     * the end. Duplicate values for the same key are permitted.
     *
     * @throws NullPointerException if key or value is null
     * @throws IllegalStateException  if the AmbientContext is frozen
     */
    public <T> void put(Metadata.Key<T> key, T value) {
        checkFreeze();
        contextMetadata.put(key, value);
    }

    /**
     * Removes the first occurrence of {@code value} for {@code key}.
     *
     * @param key key for value
     * @param value value
     * @return {@code true} if {@code value} removed; {@code false} if {@code value} was not present
     *
     * @throws NullPointerException if {@code key} or {@code value} is null
     * @throws IllegalStateException  if the AmbientContext is frozen
     */
    public <T> boolean remove(Metadata.Key<T> key, T value) {
        checkFreeze();
        return contextMetadata.remove(key, value);
    }

    /**
     * Remove all values for the given key. If there were no values, {@code null} is returned.
     *
     * @throws IllegalStateException  if the AmbientContext is frozen
     */
    public <T> Iterable<T> removeAll(Metadata.Key<T> key) {
        checkFreeze();
        return contextMetadata.removeAll(key);
    }

    @Override
    public String toString() {
        return (isFrozen() ? "[FROZEN] " : "[THAWED] ") + contextMetadata.toString();
    }
}
