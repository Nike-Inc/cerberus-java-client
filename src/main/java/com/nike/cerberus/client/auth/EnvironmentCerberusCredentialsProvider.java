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

/**
 * {@link CerberusCredentialsProvider} implementation that attempts to acquire the token
 * via the environment variable, <code>CERBERUS_TOKEN</code>.
 */
public class EnvironmentCerberusCredentialsProvider implements CerberusCredentialsProvider {

    public static final String CERBERUS_TOKEN_ENV_PROPERTY = "CERBERUS_TOKEN";

    /**
     * Attempts to acquire credentials from an environment variable.
     *
     * @return credentials
     */
    @Override
    public CerberusCredentials getCredentials() {
        final String token = System.getenv(CERBERUS_TOKEN_ENV_PROPERTY);

        if (StringUtils.isNotBlank(token)) {
            return new TokenCerberusCredentials(token);
        }

        throw new CerberusClientException("Cerberus token not found in the environment property: " + CERBERUS_TOKEN_ENV_PROPERTY);
    }
}
