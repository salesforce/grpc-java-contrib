/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.grpc.contrib;

import com.google.protobuf.Timestamp;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static com.google.common.base.Preconditions.*;

/**
 * Utility methods for converting between Protocol Buffer {@link Timestamp} objects and JDK 8 java.time classes.
 */
public final class MoreTimestamps {
    private MoreTimestamps() {
        // private static constructor
    }

    public static Instant toInstantUtc(@Nonnull Timestamp timestamp) {
        checkNotNull(timestamp, "timestamp");
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    public static OffsetDateTime toOffsetDateTimeUtc(@Nonnull Timestamp timestamp) {
        checkNotNull(timestamp, "timestamp");
        return toInstantUtc(timestamp).atOffset(ZoneOffset.UTC);
    }

    public static ZonedDateTime toZonedDateTimeUtc(@Nonnull Timestamp timestamp) {
        checkNotNull(timestamp, "timestamp");
        return toOffsetDateTimeUtc(timestamp).toZonedDateTime();
    }

    public static Timestamp fromInstantUtc(@Nonnull Instant instant) {
        checkNotNull(instant, "instant");
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public static Timestamp fromOffsetDateTimeUtc(@Nonnull OffsetDateTime offsetDateTime) {
        checkNotNull(offsetDateTime, "offsetDateTime");
        return fromInstantUtc(offsetDateTime.toInstant());
    }

    public static Timestamp fromZonedDateTimeUtc(@Nonnull ZonedDateTime zonedDateTime) {
        checkNotNull(zonedDateTime, "zonedDateTime");
        return fromOffsetDateTimeUtc(zonedDateTime.toOffsetDateTime());
    }
}
