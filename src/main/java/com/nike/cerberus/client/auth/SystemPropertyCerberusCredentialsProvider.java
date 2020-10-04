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

import org.apache.commons.lang3.StringUtils;

import com.nike.cerberus.client.exception.CerberusClientException;

/**
 * {@link CerberusCredentialsProvider} implementation that attempts to acquire the token
 * via the system property, <code>cerberus.token</code>.
 */
public class SystemPropertyCerberusCredentialsProvider implements CerberusCredentialsProvider {

    public static final String CERBERUS_TOKEN_SYS_PROPERTY = "cerberus.token";

    /**
     * Attempts to acquire credentials from an java system property.
     *
     * @return credentials
     */
    @Override
    public CerberusCredentials getCredentials() {
        final String token = System.getProperty(CERBERUS_TOKEN_SYS_PROPERTY);

        if (StringUtils.isNotBlank(token)) {
            return new TokenCerberusCredentials(token);
        }

        throw new CerberusClientException("Cerberus token not found in the java system property: " + CERBERUS_TOKEN_SYS_PROPERTY);
    }
}
