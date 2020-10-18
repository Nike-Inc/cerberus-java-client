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

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.nike.cerberus.client.ClientVersion;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests ClientVersion class
 */
public class ClientVersionTest {

    @Test
    public void test_that_version_file_exists() {
        getClass().getClassLoader().getResourceAsStream(ClientVersion.CLIENT_VERSION_PROPERTY_FILE);
    }

    @Test
    public void test_that_version_is_not_null() {
        assertNotNull(ClientVersion.getVersion());
    }

    @Test
    public void test_that_header_value_includes_right_prefix() {

        String result = ClientVersion.getClientHeaderValue();
        assertTrue(StringUtils.contains(result, ClientVersion.HEADER_VALUE_PREFIX));
        assertTrue(StringUtils.contains(result, ClientVersion.getVersion()));
    }
}
