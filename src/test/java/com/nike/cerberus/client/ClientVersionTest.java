package com.nike.cerberus.client;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

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
        assertTrue(StringUtils.contains(result, com.nike.vault.client.ClientVersion.getVersion()));
    }
}
