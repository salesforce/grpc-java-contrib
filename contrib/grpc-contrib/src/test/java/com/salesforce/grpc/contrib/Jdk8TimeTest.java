/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import org.junit.Test;

import java.time.*;

import static org.junit.Assert.*;

public class Jdk8TimeTest {
    @Test
    public void instantTest() {
        Instant now1 = Instant.now(Clock.systemUTC());
        Instant now2 = MoreTimestamps.toInstantUtc(MoreTimestamps.fromInstantUtc(now1));
        assertEquals(now1, now2);
    }

    @Test
    public void offsetDateTimeTest() {
        OffsetDateTime odt1 = OffsetDateTime.now(Clock.systemUTC());
        OffsetDateTime odt2 = MoreTimestamps.toOffsetDateTimeUtc(MoreTimestamps.fromOffsetDateTimeUtc(odt1));
        assertEquals(odt1, odt2);
    }

    @Test
    public void zonedDateTimeTest() {
        ZonedDateTime zdt1 = ZonedDateTime.now(Clock.systemUTC());
        ZonedDateTime zdt2 = MoreTimestamps.toZonedDateTimeUtc(MoreTimestamps.fromZonedDateTimeUtc(zdt1));
        assertEquals(zdt1, zdt2);
    }

    @Test
    public void durationTest() {
        Duration d1 = Duration.ofSeconds(10, 500);
        Duration d2 = MoreDurations.toJdkDuration(MoreDurations.fromJdkDuration(d1));
        assertEquals(d1, d2);
    }
}
