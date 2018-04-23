/*
 * Copyright (c) 2017 Nike, Inc.
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

import com.nike.cerberus.client.auth.CerberusCredentialsProviderChain;
import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;
import com.nike.cerberus.client.auth.EnvironmentCerberusCredentialsProvider;
import com.nike.cerberus.client.auth.SystemPropertyCerberusCredentialsProvider;
import com.nike.cerberus.client.auth.aws.LambdaRoleCerberusCredentialsProvider;
import com.nike.cerberus.client.auth.aws.StaticIamRoleCerberusCredentialsProvider;
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
     * Creates a new {@link CerberusClient} with the {@link DefaultCerberusUrlResolver} for URL resolving
     * and {@link DefaultCerberusCredentialsProviderChain} for obtaining credentials.
     *
     * @return Cerberus client
     */
    public static CerberusClient getClient() {
        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        return CerberusClientFactory.getClient(
                new DefaultCerberusUrlResolver(),
                new DefaultCerberusCredentialsProviderChain(),
                defaultHeaders);
    }

    /**
     * Creates a new {@link CerberusClient} for the supplied Cerberus URL
     * and {@link DefaultCerberusCredentialsProviderChain} for obtaining credentials.
     *
     * @param cerberusUrl e.g. https://dev.cerberus.example.com
     * @return Cerberus client
     */
    public static CerberusClient getClient(String cerberusUrl) {

        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        UrlResolver urlResolver = new StaticCerberusUrlResolver(cerberusUrl);

        return CerberusClientFactory.getClient(
                urlResolver,
                new DefaultCerberusCredentialsProviderChain(urlResolver),
                defaultHeaders);

    }

    /**
     * Creates a new {@link CerberusClient} with the specified SSLSocketFactory and TrustManager.
     * <p>
     * This factory method is generally not recommended unless you have a specific need
     * to configure your TLS for your httpClient differently than the default, e.g. Java 7.
     *
     * @param cerberusUrl      e.g. https://dev.cerberus.example.com
     * @param sslSocketFactory the factory to use for TLS
     * @param trustManager     the trust manager to use for TLS
     * @return Cerberus client
     */
    public static CerberusClient getClient(String cerberusUrl, SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {

        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        UrlResolver urlResolver = new StaticCerberusUrlResolver(cerberusUrl);

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
                urlResolver,
                new DefaultCerberusCredentialsProviderChain(urlResolver, httpClient),
                defaultHeaders,
                httpClient);
    }

    /**
     * Creates a new {@link CerberusClient} for the supplied Cerberus URL and a credentials provider chain
     * that includes the {@link StaticIamRoleCerberusCredentialsProvider} for obtaining credentials.
     * <p>
     * This method is used when you want to use a particular iamPrincipalArn during authentication rather
     * than auto-determining the ARN to use.  Generally, it is simpler to use the {@code getClient()} or the
     * {@code getClient(cerberusUrl)} factory methods.  This method is ONLY needed when those methods aren't
     * producing the desired behavior.
     *
     * @param cerberusUrl     e.g. https://dev.cerberus.example.com
     * @param iamPrincipalArn the IAM principal to use in authentication, e.g. "arn:aws:iam::123456789012:role/some-role"
     * @param region          the Region for the KMS key used in auth.  Usually, this is your current region.
     * @return Cerberus client
     */
    public static CerberusClient getClient(String cerberusUrl, String iamPrincipalArn, String region) {

        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        UrlResolver urlResolver = new StaticCerberusUrlResolver(cerberusUrl);

        return CerberusClientFactory.getClient(
                urlResolver,
                new CerberusCredentialsProviderChain(
                        new EnvironmentCerberusCredentialsProvider(),
                        new SystemPropertyCerberusCredentialsProvider(),
                        new StaticIamRoleCerberusCredentialsProvider(urlResolver, iamPrincipalArn, region)),
                defaultHeaders);
    }

    /**
     * Creates a new {@link CerberusClient} with the {@link DefaultCerberusUrlResolver} for URL resolving
     * and a credentials provider chain that includes the {@link LambdaRoleCerberusCredentialsProvider} for obtaining
     * credentials.
     *
     * @param invokedFunctionArn The ARN for the AWS Lambda function being invoked.
     * @return Cerberus client
     */
    public static CerberusClient getClientForLambda(final String invokedFunctionArn) {
        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        final DefaultCerberusUrlResolver urlResolver = new DefaultCerberusUrlResolver();
        return CerberusClientFactory.getClient(
                urlResolver,
                new CerberusCredentialsProviderChain(
                        new EnvironmentCerberusCredentialsProvider(),
                        new SystemPropertyCerberusCredentialsProvider(),
                        new LambdaRoleCerberusCredentialsProvider(urlResolver, invokedFunctionArn)),
                defaultHeaders);
    }
}
