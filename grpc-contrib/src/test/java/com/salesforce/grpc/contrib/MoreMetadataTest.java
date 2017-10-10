/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.base.Objects;
import io.grpc.Metadata;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MoreMetadataTest {
    @Test
    public void jsonMarshallerRoundtrip() {
        Foo foo = new Foo();
        foo.country = "France";
        List<Bar> bars = new ArrayList<>();
        Bar bar1 = new Bar();
        bar1.cheese = "Brë";
        bar1.age = 2;
        bars.add(bar1);
        Bar bar2 = new Bar();
        bar2.cheese = "Guda<>'";
        bar2.age = 4;
        bars.add(bar2);
        foo.bars = bars;

        Metadata.AsciiMarshaller<Foo> marshaller = MoreMetadata.JSON_MARSHALLER(Foo.class);
        String str = marshaller.toAsciiString(foo);
        assertThat(str).doesNotContain("ë");

        Foo foo2 = marshaller.parseAsciiString(str);
        assertThat(foo2).isEqualTo(foo);
    }

    private class Foo {
        String country;
        List<Bar> bars;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Foo foo = (Foo) o;
            return Objects.equal(country, foo.country) &&
                    Objects.equal(bars, foo.bars);
        }
    }

    private class Bar {
        String cheese;
        int age;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bar bar = (Bar) o;
            return age == bar.age &&
                    Objects.equal(cheese, bar.cheese);
        }
    }

    @Test
    public void protobufMarshallerRoundtrip() {
        HelloRequest request = HelloRequest.newBuilder().setName("World").build();

        Metadata.BinaryMarshaller<HelloRequest> marshaller = MoreMetadata.PROTOBUF_MARSHALLER(HelloRequest.class);
        byte[] bytes = marshaller.toBytes(request);
        HelloRequest request2 = marshaller.parseBytes(bytes);

        assertThat(request2).isEqualTo(request);
    }

    @Test
    public void booleanMarshallerRountrip() {
        Metadata.AsciiMarshaller<Boolean> marshaller = MoreMetadata.BOOLEAN_MARSHALLER;
        String s = marshaller.toAsciiString(Boolean.TRUE);
        assertThat(s).isEqualTo("true");

        Boolean b = marshaller.parseAsciiString(s);
        assertThat(b).isTrue();
    }

    @Test
    public void longMarshallerRountrip() {
        Metadata.AsciiMarshaller<Long> marshaller = MoreMetadata.LONG_MARSHALLER;
        String s = marshaller.toAsciiString(42L);
        assertThat(s).isEqualTo("42");

        Long l = marshaller.parseAsciiString(s);
        assertThat(l).isEqualTo(42L);
    }

    @Test
    public void doubleMarshallerRountrip() {
        Metadata.AsciiMarshaller<Double> marshaller = MoreMetadata.DOUBLE_MARSHALLER;
        String s = marshaller.toAsciiString(42.42);
        assertThat(s).isEqualTo("42.42");

        Double d = marshaller.parseAsciiString(s);
        assertThat(d).isEqualTo(42.42);
    }
}
