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

package com.nike.cerberus.client;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the StaticCerberusUrlResolverTest class
 */
public class StaticCerberusUrlResolverTest {

    private final String testUrl = "https://localhost";

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_throws_error_if_vault_url_is_blank() {
        new StaticCerberusUrlResolver(" ");
    }

    @Test
    public void test_resolve_returns_url_that_was_set() {
        final UrlResolver urlResolver = new StaticCerberusUrlResolver(testUrl);

        assertThat(urlResolver.resolve()).isEqualTo(testUrl);
    }
}