package com.salesforce.grpc.contrib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.grpc.Server;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ServersTest {

    @Test
    public void shutdownGracefullyThrowsIfServerIsNull() {
        final long maxWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);

        assertThatThrownBy(() -> Servers.shutdownGracefully(null, maxWaitTimeInMillis))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("server");
    }

    @Test
    public void shutdownGracefullyThrowsIfMaxWaitTimeInMillisIsZero() {
        final Server server = mock(Server.class);

        assertThatThrownBy(() -> Servers.shutdownGracefully(server, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeout must be greater than 0");
    }

    @Test
    public void shutdownGracefullyThrowsIfMaxWaitTimeInMillisIsLessThanZero() {
        final long maxWaitTimeInMillis = ThreadLocalRandom.current().nextLong(Long.MIN_VALUE, 0);
        final Server server = mock(Server.class);

        assertThatThrownBy(() -> Servers.shutdownGracefully(server, maxWaitTimeInMillis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeout must be greater than 0");
    }

    @Test
    public void shutdownGracefullyCausesServerToProperlyShutdown() throws Exception {
        final Server server = mock(Server.class);
        final long maxWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1, 10000);

        // Also assert that chaining works as expected.
        assertThat(Servers.shutdownGracefully(server, maxWaitTimeInMillis)).isSameAs(server);

        InOrder inOrder = Mockito.inOrder(server);

        inOrder.verify(server).shutdown();
        inOrder.verify(server).awaitTermination(eq(maxWaitTimeInMillis), eq(TimeUnit.MILLISECONDS));
        inOrder.verify(server).shutdownNow();
    }

    @Test
    public void shutdownGracefullyPropagatesAwaitInterrupt() throws Exception {
        final Server server = mock(Server.class);
        final long maxWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1, 10000);

        final InterruptedException interruptedException = new InterruptedException();

        when(server.awaitTermination(anyLong(), any())).thenThrow(interruptedException);

        assertThatThrownBy(() -> Servers.shutdownGracefully(server, maxWaitTimeInMillis))
                .isSameAs(interruptedException);

        InOrder inOrder = Mockito.inOrder(server);

        inOrder.verify(server).shutdown();
        inOrder.verify(server).awaitTermination(eq(maxWaitTimeInMillis), eq(TimeUnit.MILLISECONDS));
        inOrder.verify(server).shutdownNow();
    }
}
