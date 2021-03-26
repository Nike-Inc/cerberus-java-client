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

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

/**
 * Tests the DefaultCerberusClientFactory class
 */

public class DefaultCerberusClientFactoryTest {

    @Test
    public void test_that_getClient_adds_client_version_as_a_default_header() {
        String region = "us-west-2";
        String url = "url";
        CerberusClient result = DefaultCerberusClientFactory.getClient(url, region);
        assertEquals(
                ClientVersion.getClientHeaderValue(),
                result.getDefaultHeaders().get(ClientVersion.CERBERUS_CLIENT_HEADER));
    }

    @Test
    public void test_that_getClient_adds_client_version_as_a_default_header_and_returns_CerberusClientFactory() throws NoSuchAlgorithmException, KeyStoreException {
        String region = "us-west-2";
        String url = "url";
        SSLSocketFactory sslSocketFactory =(SSLSocketFactory) SSLSocketFactory.getDefault();
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        CerberusClient result = DefaultCerberusClientFactory.getClient(url, region, sslSocketFactory, (X509TrustManager)trustManagerFactory.getTrustManagers()[0]);

        assertEquals(
                ClientVersion.getClientHeaderValue(),
                result.getDefaultHeaders().get(ClientVersion.CERBERUS_CLIENT_HEADER));
    }


}