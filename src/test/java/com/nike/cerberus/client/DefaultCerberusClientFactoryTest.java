package com.nike.cerberus.client;

import com.nike.vault.client.VaultClient;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the DefaultCerberusClientFactory class
 */
public class DefaultCerberusClientFactoryTest {

    @Test
    public void test_that_getClient_adds_client_version_as_a_default_header() {
        VaultClient result = DefaultCerberusClientFactory.getClient();
        assertEquals(
                ClientVersion.getClientHeaderValue(),
                result.getDefaultHeaders().get(ClientVersion.CERBERUS_CLIENT_HEADER));
    }

    @Test
    public void test_that_getClientForLambda_adds_client_version_as_a_default_header() {
        VaultClient result = DefaultCerberusClientFactory.getClientForLambda("arn:aws:lambda:us-west-2:000000000000:function:name:qualifier");
        assertEquals(
                ClientVersion.getClientHeaderValue(),
                result.getDefaultHeaders().get(ClientVersion.CERBERUS_CLIENT_HEADER));
    }

}