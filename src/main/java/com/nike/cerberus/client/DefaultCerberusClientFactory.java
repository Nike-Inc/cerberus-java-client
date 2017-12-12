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

import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;
import com.nike.cerberus.client.auth.EnvironmentCerberusCredentialsProvider;
import com.nike.cerberus.client.auth.SystemPropertyCerberusCredentialsProvider;
import com.nike.cerberus.client.auth.aws.LambdaRoleVaultCredentialsProvider;
import com.nike.cerberus.client.auth.aws.StaticIamRoleVaultCredentialsProvider;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClient;
import com.nike.vault.client.VaultClientFactory;
import com.nike.vault.client.auth.VaultCredentialsProviderChain;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client factory for creating a Vault client with a URL resolver and credentials provider specific to Cerberus.
 */
public final class DefaultCerberusClientFactory {

    /**
     * Creates a new {@link VaultClient} with the {@link DefaultCerberusUrlResolver} for URL resolving
     * and {@link DefaultCerberusCredentialsProviderChain} for obtaining credentials.
     *
     * @return Vault client
     */
    public static VaultClient getClient() {
        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        return VaultClientFactory.getClient(
                new DefaultCerberusUrlResolver(),
                new DefaultCerberusCredentialsProviderChain(),
                defaultHeaders);
    }

    /**
     * Creates a new {@link VaultClient} for the supplied Cerberus URL
     * and {@link DefaultCerberusCredentialsProviderChain} for obtaining credentials.
     *
     * @param cerberusUrl e.g. https://dev.cerberus.example.com
     * @return Vault client
     */
    public static VaultClient getClient(String cerberusUrl) {

        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        UrlResolver urlResolver = new StaticVaultUrlResolver(cerberusUrl);

        return VaultClientFactory.getClient(
                urlResolver,
                new DefaultCerberusCredentialsProviderChain(urlResolver),
                defaultHeaders);

    }

    /**
     * Creates a new {@link VaultClient} with the specified SSLSocketFactory and TrustManager.
     * <p>
     * This factory method is generally not recommended unless you have a specific need
     * to configure your TLS for your httpClient differently than the default, e.g. Java 7.
     *
     * @param cerberusUrl      e.g. https://dev.cerberus.example.com
     * @param sslSocketFactory the factory to use for TLS
     * @param trustManager     the trust manager to use for TLS
     * @return Vault client
     */
    public static VaultClient getClient(String cerberusUrl, SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {

        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        UrlResolver urlResolver = new StaticVaultUrlResolver(cerberusUrl);

        List<ConnectionSpec> connectionSpecs = new ArrayList<>();
        connectionSpecs.add(VaultClientFactory.TLS_1_2_OR_NEWER);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(VaultClientFactory.DEFAULT_TIMEOUT, VaultClientFactory.DEFAULT_TIMEOUT_UNIT)
                .writeTimeout(VaultClientFactory.DEFAULT_TIMEOUT, VaultClientFactory.DEFAULT_TIMEOUT_UNIT)
                .readTimeout(VaultClientFactory.DEFAULT_TIMEOUT, VaultClientFactory.DEFAULT_TIMEOUT_UNIT)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .connectionSpecs(connectionSpecs)
                .build();

        return VaultClientFactory.getClient(
                urlResolver,
                new DefaultCerberusCredentialsProviderChain(urlResolver, httpClient),
                defaultHeaders,
                httpClient);
    }

    /**
     * Creates a new {@link VaultClient} for the supplied Cerberus URL and a credentials provider chain
     * that includes the {@link StaticIamRoleVaultCredentialsProvider} for obtaining credentials.
     * <p>
     * This method is used when you want to use a particular iamPrincipalArn during authentication rather
     * than auto-determining the ARN to use.  Generally, it is simpler to use the {@code getClient()} or the
     * {@code getClient(cerberusUrl)} factory methods.  This method is ONLY needed when those methods aren't
     * producing the desired behavior.
     *
     * @param cerberusUrl     e.g. https://dev.cerberus.example.com
     * @param iamPrincipalArn the IAM principal to use in authentication, e.g. "arn:aws:iam::123456789012:role/some-role"
     * @param region          the Region for the KMS key used in auth.  Usually, this is your current region.
     * @return Vault client
     */
    public static VaultClient getClient(String cerberusUrl, String iamPrincipalArn, String region) {

        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        UrlResolver urlResolver = new StaticVaultUrlResolver(cerberusUrl);

        return VaultClientFactory.getClient(
                urlResolver,
                new VaultCredentialsProviderChain(
                        new EnvironmentCerberusCredentialsProvider(),
                        new SystemPropertyCerberusCredentialsProvider(),
                        new StaticIamRoleVaultCredentialsProvider(urlResolver, iamPrincipalArn, region)),
                defaultHeaders);
    }

    /**
     * Creates a new {@link VaultClient} with the {@link DefaultCerberusUrlResolver} for URL resolving
     * and a credentials provider chain that includes the {@link LambdaRoleVaultCredentialsProvider} for obtaining
     * credentials.
     *
     * @param invokedFunctionArn The ARN for the AWS Lambda function being invoked.
     * @return Vault client
     */
    public static VaultClient getClientForLambda(final String invokedFunctionArn) {
        final Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());

        final DefaultCerberusUrlResolver urlResolver = new DefaultCerberusUrlResolver();
        return VaultClientFactory.getClient(
                urlResolver,
                new VaultCredentialsProviderChain(
                        new EnvironmentCerberusCredentialsProvider(),
                        new SystemPropertyCerberusCredentialsProvider(),
                        new LambdaRoleVaultCredentialsProvider(urlResolver, invokedFunctionArn)),
                defaultHeaders);
    }
}
