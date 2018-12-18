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

import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client factory for creating a Cerberus client with a URL resolver and credentials provider specific to Cerberus.
 */
public final class DefaultCerberusClientFactory {


    /**
     * Creates a new {@link CerberusClient} for the supplied Cerberus URL
     * and {@link DefaultCerberusCredentialsProviderChain} for obtaining credentials.
     *
     * @param cerberusUrl URL for Cerberus
     * @param region      AWS region
     * @return Cerberus client
     */
    public static CerberusClient getClient(String cerberusUrl, String region) {

        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        return CerberusClientFactory.getClient(
                cerberusUrl,
                new DefaultCerberusCredentialsProviderChain(cerberusUrl, region),
                defaultHeaders);
    }

    /**
     * Creates a new {@link CerberusClient} with the specified SSLSocketFactory and TrustManager.
     * <p>
     * This factory method is generally not recommended unless you have a specific need
     * to configure your TLS for your httpClient differently than the default, e.g. Java 7.
     *
     * @param cerberusUrl      URL for Cerberus
     * @param region           AWS region
     * @param sslSocketFactory the factory to use for TLS
     * @param trustManager     the trust manager to use for TLS
     * @return Cerberus client
     */
    public static CerberusClient getClient(String cerberusUrl, String region, SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {

        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        List<ConnectionSpec> connectionSpecs = new ArrayList<>();
        connectionSpecs.add(CerberusClientFactory.TLS_1_2_OR_NEWER);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(CerberusClientFactory.DEFAULT_TIMEOUT, CerberusClientFactory.DEFAULT_TIMEOUT_UNIT)
                .writeTimeout(CerberusClientFactory.DEFAULT_TIMEOUT, CerberusClientFactory.DEFAULT_TIMEOUT_UNIT)
                .readTimeout(CerberusClientFactory.DEFAULT_TIMEOUT, CerberusClientFactory.DEFAULT_TIMEOUT_UNIT)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .connectionSpecs(connectionSpecs)
                .build();

        return CerberusClientFactory.getClient(
                cerberusUrl,
                new DefaultCerberusCredentialsProviderChain(cerberusUrl, region, httpClient),
                defaultHeaders,
                httpClient);
    }
}
