/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.client;

import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClient;
import com.nike.vault.client.VaultClientFactory;

/**
 * Client factory for creating a Cerberus client.
 */
public final class DefaultCerberusClientFactory {

    private DefaultCerberusClientFactory() {
        // Don't instantiate factory class.
    }

    /**
     * Resolves the Vault/Cerberus URL via the {@link DefaultCerberusUrlResolver} and creates a new {@link VaultClient}
     * with the {@link DefaultCerberusCredentialsProviderChain}.
     *
     * @return Vault client
     */
    public static VaultClient getClient() {
        final UrlResolver urlResolver = new DefaultCerberusUrlResolver();
        return VaultClientFactory.getClient(urlResolver, new DefaultCerberusCredentialsProviderChain());
    }
}
