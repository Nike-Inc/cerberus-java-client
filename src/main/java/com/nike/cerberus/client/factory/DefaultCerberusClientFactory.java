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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.CerberusV2Client;
import com.nike.cerberus.client.ClientVersion;
import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

/**
 * Client factory for creating a Cerberus client with a URL resolver and credentials provider specific to Cerberus.
 */
public final class DefaultCerberusClientFactory {


    public static CerberusClient getClient(String cerberusUrl, String region) {
        return CerberusClientFactory.getClient(
                cerberusUrl,
                new DefaultCerberusCredentialsProviderChain(cerberusUrl, region),
                getDefaultHeaders());
    }
    
    public static CerberusV2Client getV2Client(String cerberusUrl, String region) {
        return CerberusV2ClientFactory.getClient(
                cerberusUrl,
                new DefaultCerberusCredentialsProviderChain(cerberusUrl, region),
                getDefaultHeaders());
    }
    
    // ----------------------------------------------------------------------------------------
    
    public static CerberusClient getClient(String cerberusUrl, String region, SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
    	OkHttpClient httpClient = getHttpClient(sslSocketFactory, trustManager);
        return CerberusClientFactory.getClient(
                cerberusUrl,
                new DefaultCerberusCredentialsProviderChain(cerberusUrl, region, httpClient),
                getDefaultHeaders(),
                httpClient);
    }
    
    public static CerberusV2Client getV2Client(String cerberusUrl, String region, SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
    	OkHttpClient httpClient = getHttpClient(sslSocketFactory, trustManager);
        return CerberusV2ClientFactory.getClient(
                cerberusUrl,
                new DefaultCerberusCredentialsProviderChain(cerberusUrl, region, httpClient),
                getDefaultHeaders(),
                httpClient);
    }

    /*
     * Helpers
     */
    
    private static Map<String, String> getDefaultHeaders(){
    	final Map<String, String> defaultHeaders = new HashMap<>();
    	defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, ClientVersion.getClientHeaderValue());
    	return defaultHeaders;
    }
    
    private static  List<ConnectionSpec> getConnectionSpecs(){
    	List<ConnectionSpec> connectionSpecs = new ArrayList<>();
    	connectionSpecs.add(CerberusClientFactory.TLS_1_2_OR_NEWER);
    	return connectionSpecs;
    }
    
	private static OkHttpClient getHttpClient(SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
		return new OkHttpClient.Builder()
                .connectTimeout(CerberusClientFactory.DEFAULT_TIMEOUT, CerberusClientFactory.DEFAULT_TIMEOUT_UNIT)
                .writeTimeout(CerberusClientFactory.DEFAULT_TIMEOUT, CerberusClientFactory.DEFAULT_TIMEOUT_UNIT)
                .readTimeout(CerberusClientFactory.DEFAULT_TIMEOUT, CerberusClientFactory.DEFAULT_TIMEOUT_UNIT)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .connectionSpecs(getConnectionSpecs())
                .build();
	}
    
}
