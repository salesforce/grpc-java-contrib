/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Statuses is a collection of utility methods for working with gRPC {@code Status} responses.
 *
 * <p>gRPC statuses are annoying to work with because they tend to come bundled in either a {@code StatusException} or a
 * {@code StatusRuntimeException}, but {@code StatusException} and {@code StatusRuntimeException} are unrelated classes,
 * so working with them requires frequent {@code instanceof} checks and downcasting. The operations in this class try
 * to smooth over these minor annoyances.
 *
 * <p>For example:
 * <pre>
 *     import static com.salesforce.grpc.contrib.Statuses;
 *
 *     try {
 *         stub.someServiceOperation(...);
 *     } catch (Throwable t) {
 *         if (hasStatusCode(t, Status.Code.UNAUTHENTICATED) {
 *             doWithStatus(t, (status, metadata) -> showLoginPrompt();
 *         } else if (Statuses.hasStatus(t) {
 *             doWithStatus(t, (status, metadata) -> handleGrpcProblem(status);
 *         } else {
 *             throw t;
 *         }
 *     }
 * </pre>
 */
public final class Statuses {
    private Statuses() { }

    /**
     * Evaluates a throwable to determine it if has a gRPC status. Particularly, is this throwable a
     * {@code StatusException} or a {@code StatusRuntimeException}.
     *
     * @param t A throwable to evaluate
     * @return {@code true} if {@code t} is a {@code StatusException} or a {@code StatusRuntimeException}
     */
    public static boolean hasStatus(Throwable t) {
        return t instanceof StatusException || t instanceof StatusRuntimeException;
    }

    /**
     * Evaluates a throwable to determine if it has a gRPC status, and then if so, evaluates the throwable's
     * status code.
     *
     * @param t A throwable to evaluate
     * @param code A {@code Status.Code} to look for
     * @return {@code true} if {@code t} is a {@code StatusException} or a {@code StatusRuntimeException} with
     * {@code Status.Code} equal to {@code code}
     */
    public static boolean hasStatusCode(Throwable t, Status.Code code) {
        if (!hasStatus(t)) {
            return false;
        } else {
            return doWithStatus(t, (status, metadata) -> status.getCode() == code);
        }
    }

    /**
     * Executes an action on a {@code StatusException} or a {@code StatusRuntimeException}, passing in the exception's
     * metadata and trailers.
     *
     * @param t a {@code StatusException} or a {@code StatusRuntimeException}
     * @param action the action to execute, given the exception's status and trailers
     *
     * @throws IllegalArgumentException if {@code t} is not a {@code StatusException} or a {@code StatusRuntimeException}
     */
    public static void doWithStatus(Throwable t, BiConsumer<Status, Metadata> action) {
        doWithStatus(t, (status, metadata) -> {
            action.accept(status, metadata);
            return true;
        });
    }

    /**
     * Executes a function on a {@code StatusException} or a {@code StatusRuntimeException}, passing in the exception's
     * metadata and trailers.
     *
     * @param t a {@code StatusException} or a {@code StatusRuntimeException}
     * @param function the function to execute, given the exception's status and trailers
     * @param <T> the function's return type
     *
     * @throws IllegalArgumentException if {@code t} is not a {@code StatusException} or a {@code StatusRuntimeException}
     */
    public static <T> T doWithStatus(Throwable t, BiFunction<Status, Metadata, T> function) {
        if (t instanceof StatusException) {
            return function.apply(((StatusException) t).getStatus(), ((StatusException) t).getTrailers());
        }
        if (t instanceof StatusRuntimeException) {
            return function.apply(((StatusRuntimeException) t).getStatus(), ((StatusRuntimeException) t).getTrailers());
        }

        throw new IllegalArgumentException("Throwable " + t.getClass().getSimpleName() + " is neither a " +
                "StatusException nor a StatusRuntimeException");
    }
}
