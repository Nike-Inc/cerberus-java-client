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

package com.nike.cerberus.client.factory;

import java.util.Map;

import com.nike.cerberus.client.CerberusV2Client;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;

import okhttp3.OkHttpClient;

/**
 * Convenience factory for creating instances of Cerberus clients.
 */
public class CerberusV2ClientFactory extends BaseClientFactory{

    /**
     * Factory method that allows for a user defined Cerberus URL resolver and credentials provider.
     *
     * @param cerberusUrl                 URL for Cerberus
     * @param cerberusCredentialsProvider Credential provider for acquiring a token for interacting with Cerberus
     * @return Cerberus client
     */
    public static CerberusV2Client getClient(final String cerberusUrl,
                                           final CerberusCredentialsProvider cerberusCredentialsProvider) {

        return getClient(cerberusUrl, cerberusCredentialsProvider, DEFAULT_HEADERS);
    }

    /**
     * Factory method that allows a user to define default HTTP defaultHeaders to be added to every HTTP request made from the
     * CerberusV2Client. The user can also define their Cerberus URL resolver and credentials provider.
     *
     * @param cerberusUrl                 URL for Cerberus
     * @param cerberusCredentialsProvider Credential provider for acquiring a token for interacting with Cerberus
     * @param defaultHeaders              Map of default header names and values to add to every HTTP request
     * @return Cerberus client
     */
    public static CerberusV2Client getClient(final String cerberusUrl,
                                           final CerberusCredentialsProvider cerberusCredentialsProvider,
                                           final Map<String, String> defaultHeaders) {

        return getClient(
                cerberusUrl,
                cerberusCredentialsProvider,
                defaultHeaders,
                new OkHttpClient.Builder()
                        .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .connectionSpecs(getConnectionSpecs())
                        .build()
        );
    }

    /**
     * Factory method that allows for a user defined Cerberus URL resolver and credentials provider.
     *
     * @param cerberusUrl                 URL for Cerberus
     * @param cerberusCredentialsProvider Credential provider for acquiring a token for interacting with Cerberus
     * @param maxRequestsPerHost          Max Requests per Host used by the dispatcher
     * @return Cerberus admin client
     */
    public static CerberusV2Client getClient(final String cerberusUrl,
                                           final CerberusCredentialsProvider cerberusCredentialsProvider,
                                           final int maxRequestsPerHost) {

        return getClient(cerberusUrl,
                cerberusCredentialsProvider,
                DEFAULT_MAX_REQUESTS,
                maxRequestsPerHost,
                DEFAULT_TIMEOUT,
                DEFAULT_TIMEOUT,
                DEFAULT_TIMEOUT,
                DEFAULT_HEADERS);
    }

    /**
     * Factory method that allows a user to define the OkHttpClient to be used.
     *
     * @param cerberusUrl                 URL for Cerberus
     * @param cerberusCredentialsProvider Credential provider for acquiring a token for interacting with Cerberus
     * @param defaultHeaders              Map of default header names and values to add to every HTTP request
     * @param httpClient                  the client to use for auth
     * @return Cerberus client
     */
    public static CerberusV2Client getClient(final String cerberusUrl,
                                           final CerberusCredentialsProvider cerberusCredentialsProvider,
                                           final Map<String, String> defaultHeaders,
                                           final OkHttpClient httpClient) {

        return new CerberusV2Client(cerberusUrl,
                cerberusCredentialsProvider,
                httpClient,
                getHeaders(defaultHeaders).build());
    }

    /**
     * Factory method that allows the user to completely configure the CerberusV2Client.
     *
     * @param cerberusUrl                 URL for Cerberus
     * @param cerberusCredentialsProvider Credential provider for acquiring a token for interacting with Cerberus
     * @param maxRequestsPerHost          Max Requests per Host used by the dispatcher
     * @param defaultHeaders              Map of default header names and values to add to every HTTP request
     * @return Cerberus admin client
     */
    public static CerberusV2Client getClient(final String cerberusUrl,
                                           final CerberusCredentialsProvider cerberusCredentialsProvider,
                                           final int maxRequestsPerHost,
                                           final Map<String, String> defaultHeaders) {

        return getClient(cerberusUrl,
                cerberusCredentialsProvider,
                DEFAULT_MAX_REQUESTS,
                maxRequestsPerHost,
                DEFAULT_TIMEOUT,
                DEFAULT_TIMEOUT,
                DEFAULT_TIMEOUT,
                defaultHeaders);
    }

    /**
     * Factory method that allows the user to completely configure the CerberusV2Client.
     *
     * @param cerberusUrl                 URL for Cerberus
     * @param cerberusCredentialsProvider Credential provider for acquiring a token for interacting with Cerberus
     * @param maxRequests                 Max HTTP Requests allowed in-flight
     * @param maxRequestsPerHost          Max HTTP Requests per Host
     * @param connectTimeoutMillis        HTTP connect timeout in milliseconds
     * @param readTimeoutMillis           HTTP read timeout in milliseconds
     * @param writeTimeoutMillis          HTTP write timeout in milliseconds
     * @param defaultHeaders              Map of default header names and values to add to every HTTP request
     * @return Cerberus admin client
     */
    public static CerberusV2Client getClient(final String cerberusUrl,
                                           final CerberusCredentialsProvider cerberusCredentialsProvider,
                                           final int maxRequests,
                                           final int maxRequestsPerHost,
                                           final int connectTimeoutMillis,
                                           final int readTimeoutMillis,
                                           final int writeTimeoutMillis,
                                           final Map<String, String> defaultHeaders) {

        return new CerberusV2Client(cerberusUrl,
                cerberusCredentialsProvider,
                new OkHttpClient.Builder()
                        .connectTimeout(connectTimeoutMillis, DEFAULT_TIMEOUT_UNIT)
                        .writeTimeout(writeTimeoutMillis, DEFAULT_TIMEOUT_UNIT)
                        .readTimeout(readTimeoutMillis, DEFAULT_TIMEOUT_UNIT)
                        .dispatcher(getDispatcher(maxRequests, maxRequestsPerHost))
                        .connectionSpecs(getConnectionSpecs())
                        .build(),
                getHeaders(defaultHeaders).build());
    }
}
