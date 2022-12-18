/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import io.grpc.*;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;

/**
 * StaticResolver is a gRPC NameResolverProvider and NameResolver Factory that resolves every request
 * to the same static address. StaticResolverProvider is useful when name resolution is being delegated to an outside
 * proxy such as Linkerd or Envoy, typically running on localhost. Connecting directly to the proxy is insufficient
 * because each request will have the Authority header for the proxy instead of for the destination service. You can
 * also use {@link ManagedChannelBuilder#overrideAuthority(String)}, but this must be done manually for every request.
 * <p>
 * By default, StaticResolverProvider only acts on request URIs with the mesh:// scheme. This setting can be changed
 * in the constructor.
 * <p>
 * StaticResolverProvider is best used in conjunction with the {@link FallbackResolver} and
 * {@link ManagedChannelBuilder#nameResolverFactory(NameResolver.Factory)}
 */
public final class StaticResolver {
    private StaticResolver() {

    }

    public static final String DEFAULT_SCHEME = "mesh";

    /**
     * Constructs a StaticResolver provider that routes the mesh:// URI scheme to a static address.
     *
     * @param staticAddress The static address to route all requests to.
     */
    public static NameResolverProvider provider(InetSocketAddress staticAddress) {
        return provider(DEFAULT_SCHEME, staticAddress);
    }

    /**
     * Constructs a StaticResolver provider tha routes a configurable URI scheme to a static address.
     *
     * @param scheme        The URI scheme to route.
     * @param staticAddress The static address to route all requests to.
     */
    public static NameResolverProvider provider(String scheme, InetSocketAddress staticAddress) {
        return new StaticResolverProvider(scheme, staticAddress);
    }

    /**
     * Constructs a StaticResolver factory that routes the mesh:// URI scheme to a static address.
     *
     * @param staticAddress The static address to route all requests to.
     */
    public static NameResolver.Factory factory(InetSocketAddress staticAddress) {
        return factory(DEFAULT_SCHEME, staticAddress);
    }

    /**
     * Constructs a StaticResolver factory tha routes a configurable URI scheme to a static address.
     *
     * @param scheme        The URI scheme to route.
     * @param staticAddress The static address to route all requests to.
     */
    public static NameResolver.Factory factory(String scheme, InetSocketAddress staticAddress) {
        return new StaticResolverFactory(scheme, staticAddress);
    }

    /**
     * A provider of static name resolution.
     */
    private static class StaticResolverProvider extends NameResolverProvider {
        private final String scheme;
        private final InetSocketAddress staticAddress;

        /**
         * Constructs a StaticResolverProvider tha routes a configurable URI scheme to a static address.
         *
         * @param scheme        The URI scheme to route.
         * @param staticAddress The static address to route all requests to.
         */
        StaticResolverProvider(String scheme, InetSocketAddress staticAddress) {
            this.scheme = scheme;
            this.staticAddress = staticAddress;
        }

        @Nullable
        @Override
        public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
            if (scheme.equals(targetUri.getScheme())) {
                final String authority = targetUri.getAuthority();

                return new NameResolver() {
                    @Override
                    public String getServiceAuthority() {
                        return authority;
                    }

                    @Override
                    public void start(NameResolver.Listener listener) {
                        try {
                            listener.onAddresses(
                                    Collections.singletonList(new EquivalentAddressGroup(staticAddress)),
                                    Attributes.EMPTY
                            );
                        } catch (Throwable e) {
                            listener.onError(Status.UNKNOWN);
                        }
                    }

                    @Override
                    public void shutdown() {

                    }
                };
            } else {
                return null;
            }
        }

        @Override
        protected boolean isAvailable() {
            return true;
        }

        @Override
        protected int priority() {
            return 0;
        }

        @Override
        public String getDefaultScheme() {
            return scheme;
        }
    }

    /**
     * A factory for using StaticResolverProvider directly.
     */
    private static class StaticResolverFactory extends NameResolver.Factory {
        private String scheme;
        private StaticResolverProvider provider;

        StaticResolverFactory(String scheme, InetSocketAddress staticAddress) {
            this.scheme = scheme;
            this.provider = new StaticResolverProvider(scheme, staticAddress);
        }

        @Nullable
        @Override
        public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
            return provider.newNameResolver(targetUri, args);
        }

        @Override
        public String getDefaultScheme() {
            return scheme;
        }
    }
}
