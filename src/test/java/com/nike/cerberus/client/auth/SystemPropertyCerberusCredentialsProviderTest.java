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

import com.nike.cerberus.client.CerberusClientException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Tests the SystemPropertyCerberusCredentialsProvider class
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemPropertyCerberusCredentialsProvider.class})
public class SystemPropertyCerberusCredentialsProviderTest {

    private static final String TOKEN = "TOKEN";

    private SystemPropertyCerberusCredentialsProvider credentialsProvider;

    @Before
    public void setup() {
        credentialsProvider = new SystemPropertyCerberusCredentialsProvider();
    }

    @Test
    public void getCredentials_returns_creds_from_system_property_when_set() {
        mockStatic(System.class);
        when(System.getProperty(SystemPropertyCerberusCredentialsProvider.CERBERUS_TOKEN_SYS_PROPERTY)).thenReturn(TOKEN);

        CerberusCredentials credentials = credentialsProvider.getCredentials();

        assertThat(credentials).isNotNull();
        assertThat(credentials.getToken()).isEqualTo(TOKEN);
    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_returns_empty_creds_object_when_sys_property_not_set() {
        mockStatic(System.class);
        when(System.getProperty(SystemPropertyCerberusCredentialsProvider.CERBERUS_TOKEN_SYS_PROPERTY)).thenReturn(null);

        credentialsProvider.getCredentials();
    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_returns_empty_creds_object_when_sys_property_is_blank() {
        mockStatic(System.class);
        when(System.getProperty(SystemPropertyCerberusCredentialsProvider.CERBERUS_TOKEN_SYS_PROPERTY)).thenReturn("");

        credentialsProvider.getCredentials();
    }
}