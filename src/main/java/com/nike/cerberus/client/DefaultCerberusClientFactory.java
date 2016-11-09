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
import com.nike.vault.client.VaultClient;
import com.nike.vault.client.VaultClientFactory;

/**
 * Client factory for creating a Vault client with a URL resolver and credentials provider specific to Cerberus.
 */
public final class DefaultCerberusClientFactory {

    /**
     * Creates a new {@link VaultClient} with the {@link DefaultCerberusUrlResolver} for URL resolving
     * and {@link DefaultCerberusCredentialsProviderChain} for obtaining credentials.
     *
     * @return Vault client
     */
    public static VaultClient getClient() {
        return VaultClientFactory.getClient(new DefaultCerberusUrlResolver(),
                new DefaultCerberusCredentialsProviderChain());
    }
}
