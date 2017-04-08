/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.grpc.contrib;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.*;

/**
 * Utility methods for converting between Protocol Buffer {@link com.google.protobuf.Duration} objects and JDK 8
 * java.time classes.
 *
 * <p>This class intentionally omits conversions to and from {@link java.time.Period}. Converting to and from Period
 * requires context-specific knowledge about how to handle things like daylight savings time and different calendars.
 */
public final class MoreDurations {
    private MoreDurations() {
        // private static constructor
    }

    public static java.time.Duration toJdkDuration(@Nonnull com.google.protobuf.Duration pbDuration) {
        checkNotNull(pbDuration, "pbDuration");
        return java.time.Duration.ofSeconds(pbDuration.getSeconds(), pbDuration.getNanos());
    }

    public static com.google.protobuf.Duration fromJdkDuration(@Nonnull java.time.Duration jdkDuration) {
        checkNotNull(jdkDuration, "jdkDuration");
        return com.google.protobuf.Duration.newBuilder()
                .setSeconds(jdkDuration.getSeconds())
                .setNanos(jdkDuration.getNano())
                .build();
    }
}