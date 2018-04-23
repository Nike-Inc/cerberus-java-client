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

package com.nike.cerberus.client.auth;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the TokenCerberusCredentials class
 */
public class TokenCerberusCredentialsTest {

    @Test
    public void getToken_returns_the_token_set_during_construction() {
        final String token = "TOKEN";

        TokenCerberusCredentials credentials = new TokenCerberusCredentials(token);

        assertThat(credentials.getToken()).isEqualTo(token);
    }
}