/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.client.auth;

import com.nike.cerberus.client.DefaultCerberusUrlResolver;
import com.nike.cerberus.client.UrlResolver;
import com.nike.cerberus.client.auth.aws.InstanceRoleCerberusCredentialsProvider;
import okhttp3.OkHttpClient;

/**
 * Default credentials provider chain that will attempt to retrieve a token in the following order:
 * <p>
 * <ul>
 * <li>Environment Variable - <code>CERBERUS_TOKEN</code></li>
 * <li>Java System Property - <code>cerberus.token</code></li>
 * <li>Custom lookup using instance profile from EC2 metadata service</li>
 * </ul>
 *
 * @see EnvironmentCerberusCredentialsProvider
 * @see SystemPropertyCerberusCredentialsProvider
 * @see InstanceRoleCerberusCredentialsProvider
 */
public class DefaultCerberusCredentialsProviderChain extends CerberusCredentialsProviderChain {

    /**
     * Default constructor that sets up a default provider chain.
     */
    public DefaultCerberusCredentialsProviderChain() {
        this(new DefaultCerberusUrlResolver());
    }

    /**
     * Constructor that sets up a provider chain using the specified implementation of UrlResolver.
     *
     * @param urlResolver Resolves the Cerberus URL
     */
    public DefaultCerberusCredentialsProviderChain(UrlResolver urlResolver) {
        super(new EnvironmentCerberusCredentialsProvider(),
                new SystemPropertyCerberusCredentialsProvider(),
                new InstanceRoleCerberusCredentialsProvider(urlResolver));
    }

    /**
     * Constructor that sets up a provider chain using the specified implementation of UrlResolver
     * and OkHttpClient
     *
     * @param urlResolver Resolves the Cerberus URL
     * @param httpClient  the client to use
     */
    public DefaultCerberusCredentialsProviderChain(UrlResolver urlResolver, OkHttpClient httpClient) {
        super(new EnvironmentCerberusCredentialsProvider(),
                new SystemPropertyCerberusCredentialsProvider(),
                new InstanceRoleCerberusCredentialsProvider(urlResolver, httpClient));
    }

    /**
     * Constructor that sets up a provider chain using the specified implementation of UrlResolver.
     *
     * @param xCerberusClientOverride - Overrides the default header value for the 'X-Cerberus-Client' header
     */
    public DefaultCerberusCredentialsProviderChain(String xCerberusClientOverride) {
        this(new DefaultCerberusUrlResolver(), xCerberusClientOverride);
    }

    /**
     * Constructor that sets up a provider chain using the specified implementation of UrlResolver.
     *
     * @param urlResolver             Resolves the Cerberus URL
     * @param xCerberusClientOverride - Overrides the default header value for the 'X-Cerberus-Client' header
     */
    public DefaultCerberusCredentialsProviderChain(UrlResolver urlResolver, String xCerberusClientOverride) {
        super(new EnvironmentCerberusCredentialsProvider(),
                new SystemPropertyCerberusCredentialsProvider(),
                new InstanceRoleCerberusCredentialsProvider(urlResolver, xCerberusClientOverride));
    }
}
