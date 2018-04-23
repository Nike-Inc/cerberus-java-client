/*
 * Copyright (c) 2018 Nike, Inc.
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

import com.nike.cerberus.client.CerberusClientException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CerberusCredentialsProviderChain implements CerberusCredentialsProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CerberusCredentialsProviderChain.class);

    private final List<CerberusCredentialsProvider> credentialsProviderList = new LinkedList<>();

    private boolean reuseLastProvider = true;
    private CerberusCredentialsProvider lastUsedProvider;

    /**
     * Explicit constructor that takes a list of providers to use.
     *
     * @param credentialsProviderList List of providers
     */
    public CerberusCredentialsProviderChain(final List<? extends CerberusCredentialsProvider> credentialsProviderList) {
        if (credentialsProviderList == null || credentialsProviderList.size() == 0) {
            throw new IllegalArgumentException("No credentials providers specified");
        }

        this.credentialsProviderList.addAll(credentialsProviderList);
    }

    /**
     * Explicit constructor that takes an array of providers to use.
     *
     * @param credentialsProviders Array of providers
     */
    public CerberusCredentialsProviderChain(final CerberusCredentialsProvider... credentialsProviders) {
        if (credentialsProviders == null || credentialsProviders.length == 0) {
            throw new IllegalArgumentException("No credentials providers specified");
        }

        Collections.addAll(this.credentialsProviderList, credentialsProviders);
    }

    /**
     * Iterates over the chain of providers looking for one that returns credentials.  If this is a subsequent call
     * to the method and a successful provider has already be identified, that identified provider will be used instead
     * of iterating over the full chain.  This is the default behavior and can be disabled via
     * {@link #setReuseLastProvider(boolean)}.  If no provider is able to acquire credentials a client exception is
     * thrown.
     *
     * @return Credentials
     */
    @Override
    public CerberusCredentials getCredentials() {
        if (reuseLastProvider && lastUsedProvider != null) {
            return lastUsedProvider.getCredentials();
        }

        for (final CerberusCredentialsProvider credentialsProvider : credentialsProviderList) {
            try {
                final CerberusCredentials credentials = credentialsProvider.getCredentials();

                if (StringUtils.isNotBlank(credentials.getToken())) {
                    lastUsedProvider = credentialsProvider;
                    return credentials;
                }
            } catch (CerberusClientException sce) {
                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Failed to resolve Cerberus credentials with credential provider: {}. Moving " +
                            "on to next provider", credentialsProvider.getClass().toString(), sce);
                } else {
                    LOGGER.info("Failed to resolve Cerberus credentials with credential provider: {} for reason: {} moving " +
                            "on to next provider", credentialsProvider.getClass().toString(), sce.getMessage());
                }
            } catch (Exception e) {
                // The catch all is so that we don't break the chain of providers.
                // If we do get an unexpected exception, we should at least log it for review.
                LOGGER.warn("Unexpected error attempting to get credentials with provider: "
                        + credentialsProvider.getClass().getName(), e);
            }
        }

        throw new CerberusClientException("Unable to find credentials from any provider in the specified chain!");
    }


    /**
     * Returns the reuse last provider flag.
     *
     * @return reuse last provider flag
     */
    public boolean isReuseLastProvider() {
        return reuseLastProvider;
    }

    /**
     * Enables the ability to enable or disable the reuse of the last successful provider.
     *
     * @param reuseLastProvider Flag for usage of the last successful provider
     */
    public void setReuseLastProvider(final boolean reuseLastProvider) {
        this.reuseLastProvider = reuseLastProvider;
    }
}
