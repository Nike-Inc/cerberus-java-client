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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Tests the DefaultCerberusCredentialsProviderChain class
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({EnvironmentCerberusCredentialsProvider.class, SystemPropertyCerberusCredentialsProvider.class})
@PowerMockIgnore("javax.net.ssl.*")
public class DefaultCerberusCredentialsProviderChainTest {

    private static final String ENV_VALUE = "ENV";

    private static final String SYS_VALUE = "SYS";

    private DefaultCerberusCredentialsProviderChain credentialsProviderChain;

    @Before
    public void setup() {
        credentialsProviderChain = new DefaultCerberusCredentialsProviderChain();
    }

    @Test
    public void env_set_credentials_always_returned_when_sys_property_is_also_set() {
        mockStatic(System.class);
        when(System.getenv(EnvironmentCerberusCredentialsProvider.CERBERUS_TOKEN_ENV_PROPERTY)).thenReturn(ENV_VALUE);
        when(System.getProperty(SystemPropertyCerberusCredentialsProvider.CERBERUS_TOKEN_SYS_PROPERTY)).thenReturn(SYS_VALUE);

        CerberusCredentials credentials = credentialsProviderChain.getCredentials();

        assertThat(credentials).isNotNull();
        assertThat(credentials.getToken()).isEqualTo(ENV_VALUE);
    }

    @Test
    public void sys_value_set_if_env_is_not_set() {
        mockStatic(System.class);
        when(System.getenv(EnvironmentCerberusCredentialsProvider.CERBERUS_TOKEN_ENV_PROPERTY)).thenReturn("");
        when(System.getProperty(SystemPropertyCerberusCredentialsProvider.CERBERUS_TOKEN_SYS_PROPERTY)).thenReturn(SYS_VALUE);

        CerberusCredentials credentials = credentialsProviderChain.getCredentials();

        assertThat(credentials).isNotNull();
        assertThat(credentials.getToken()).isEqualTo(SYS_VALUE);
    }
}