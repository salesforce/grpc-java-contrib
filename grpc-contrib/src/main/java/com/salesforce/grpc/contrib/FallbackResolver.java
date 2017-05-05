/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.base.Preconditions;
import io.grpc.*;
import io.grpc.internal.DnsNameResolverProvider;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * FallbackResolver is a {@link NameResolver.Factory} that allows multiple {@link NameResolver} instances to be
 * queried in sequence when attempting to resolve an Authority by URI scheme. If the first name resolver cannot handle
 * the Authority's scheme, the remaining resolvers will be tried in turn.
 * <p>
 * FallbackResolver is particularly useful when setting an explicit client resolver via
 * {@link ManagedChannelBuilder#nameResolverFactory(NameResolver.Factory)}, rather than relying on gRPC's default
 * resolver discovery system. For example, the {@link StaticResolver#provider(java.net.InetSocketAddress)}.
 * <p>
 * FallbackResolver assumes the default scheme of the first resolver in the sequence.
 */
public final class FallbackResolver extends NameResolver.Factory {
    /**
     * A shortcut for accessing the default {@link DnsNameResolverProvider}.
     */
    public static final NameResolverProvider DNS = new DnsNameResolverProvider();

    private final List<NameResolverProvider> providers = new LinkedList<>();

    /**
     * Establishes the first {@link NameResolverProvider} to check.
     */
    public static FallbackResolver startWith(NameResolverProvider first) {
        Preconditions.checkNotNull(first, "first");
        return new FallbackResolver(first);
    }

    private FallbackResolver(@Nonnull NameResolverProvider first) {
        thenCheck(first);
    }

    /**
     * Establishes a subsequent {@link NameResolverProvider} to check.
     */
    public FallbackResolver thenCheck(@Nonnull NameResolverProvider provider) {
        Preconditions.checkNotNull(provider, "provider");
        providers.add(provider);

        return this;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        for (NameResolverProvider provider : providers) {
            NameResolver resolver = provider.newNameResolver(targetUri, params);
            if (resolver != null) {
                return resolver;
            }
        }
        return null;
    }

    @Override
    public String getDefaultScheme() {
        return providers.get(0).getDefaultScheme();
    }
}
