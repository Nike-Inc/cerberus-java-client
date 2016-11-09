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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests the resolve methods on DefaultCerberusUrlResolver
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DefaultCerberusUrlResolver.class, System.class})
public class DefaultCerberusUrlResolverTest {

    private final String url = "http://localhost:8080";

    private DefaultCerberusUrlResolver subject;

    @Before
    public void setup() {
        mockStatic(System.class);
        subject = new DefaultCerberusUrlResolver();
    }

    @Test
    public void lookupVaultUrl_returns_url_if_env_variable_is_set() {
        when(System.getenv(DefaultCerberusUrlResolver.CERBERUS_ADDR_ENV_PROPERTY)).thenReturn(url);

        assertThat(subject.resolve()).isEqualTo(url);
    }

    @Test
    public void lookupVaultUrl_returns_url_if_sys_property_is_set() {
        when(System.getProperty(DefaultCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY)).thenReturn(url);

        assertThat(subject.resolve()).isEqualTo(url);
    }

    @Test
    public void lookupVaultUrl_returns_null_if_env_and_sys_not_set() {
        assertThat(subject.resolve()).isNull();
    }
}
