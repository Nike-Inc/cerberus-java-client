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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nike.cerberus.client.exception.CerberusClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Tests the EnvironmentCerberusCredentialsProvider class
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({EnvironmentCerberusCredentialsProvider.class})
public class EnvironmentCerberusCredentialsProviderTest {

    private static final String TOKEN = "TOKEN";

    private EnvironmentCerberusCredentialsProvider credentialsProvider;

    @Before
    public void setup() {
        credentialsProvider = new EnvironmentCerberusCredentialsProvider();
    }

    @Test
    public void getCredentials_returns_creds_from_env_when_set() {
        mockStatic(System.class);
        when(System.getenv(EnvironmentCerberusCredentialsProvider.CERBERUS_TOKEN_ENV_PROPERTY)).thenReturn(TOKEN);

        CerberusCredentials credentials = credentialsProvider.getCredentials();

        assertThat(credentials).isNotNull();
        assertThat(credentials.getToken()).isEqualTo(TOKEN);
    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_throws_client_exception_when_not_set() {
        mockStatic(System.class);
        when(System.getenv(EnvironmentCerberusCredentialsProvider.CERBERUS_TOKEN_ENV_PROPERTY)).thenReturn(null);

        credentialsProvider.getCredentials();
    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_returns_empty_creds_object_when_env_variable_is_blank() {
        mockStatic(System.class);
        when(System.getenv(EnvironmentCerberusCredentialsProvider.CERBERUS_TOKEN_ENV_PROPERTY)).thenReturn("");

        credentialsProvider.getCredentials();
    }
}