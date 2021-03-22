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
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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
    public void test_that_getClient_adds_client_version_as_a_default_header_and_returns_CerberusClientFactory() {
        String region = "us-west-2";
        String url = "url";
        SSLSocketFactory sslSocketFactory = new SSLSocketFactory() {
            @Override
            public String[] getDefaultCipherSuites() {
                return new String[0];
            }

            @Override
            public String[] getSupportedCipherSuites() {
                return new String[0];
            }

            @Override
            public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
                return null;
            }

            @Override
            public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
                return null;
            }

            @Override
            public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
                return null;
            }

            @Override
            public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
                return null;
            }

            @Override
            public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
                return null;
            }
        };

        X509TrustManager x509TrustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        CerberusClient result = DefaultCerberusClientFactory.getClient(url, region, sslSocketFactory, x509TrustManager);

        assertEquals(
                ClientVersion.getClientHeaderValue(),
                result.getDefaultHeaders().get(ClientVersion.CERBERUS_CLIENT_HEADER));
    }


}