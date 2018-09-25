package com.nike.cerberus.client.util;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnvironmentUtilsTest {

    @Test
    public void test_that_canGetSuccessfully_can_get_google_index() {
        assertTrue(EnvironmentUtils.canGetSuccessfully("http://www.google.com"));
        assertTrue(EnvironmentUtils.canGetSuccessfully("http://www.google.com"));
    }

    @Test
    public void test_that_canGetSuccessfully_can_not_get_random_http_address() {
        assertFalse(EnvironmentUtils.canGetSuccessfully("http://" + UUID.randomUUID().toString() + ".com/" + UUID.randomUUID().toString()));
    }

}
