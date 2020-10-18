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

package com.nike.cerberus.client.auth.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicSessionCredentials;
import com.nike.cerberus.client.exception.CerberusClientException;
import com.nike.cerberus.client.model.response.CerberusAuthResponse;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.tngtech.java.junit.dataprovider.DataProvider;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests the StsCerberusCredentialsProvider class
 */
@RunWith(DataProviderRunner.class)
public class StsCerberusCredentialsProviderTest {

    private static final String REGION_STRING_EAST = "us-east-1";
    private static final String REGION_STRING_WEST = "us-west-2";

    protected static final String DECODED_AUTH_DATA = "{\"client_token\":\"6632cb5f-f10c-4572-9545-e52f47f6a3fd\", \"lease_duration\":\"3600\"}";
    protected static final String ERROR_RESPONSE = "Invalid credentials";

    private String cerberusUrl;
    private AWSCredentialsProviderChain chain;
    private AWSCredentials credentials;

    @Before
    public void setUp() {
        cerberusUrl = mock(String.class);
        chain = mock(AWSCredentialsProviderChain.class);
        credentials = new BasicSessionCredentials("foo", "bar", "cat");
    }

    @Test
    public void test_sts_creds_provider_constructor() {

        StsCerberusCredentialsProvider credentialsProvider = new StsCerberusCredentialsProvider(cerberusUrl, REGION_STRING_EAST);
        assertThat(credentialsProvider.getCerberusUrl()).isEqualTo(cerberusUrl);
        assertThat(credentialsProvider.regionName).isEqualTo(REGION_STRING_EAST);
    }

    @Test
    @DataProvider(value = {
            REGION_STRING_EAST,
            REGION_STRING_WEST})
    public void test_get_signed_headers(String testRegion) throws IOException {

        when(chain.getCredentials()).thenReturn(credentials);

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        try{
	        final String cerberusUrl = "http://localhost:" + mockWebServer.getPort();
	        StsCerberusCredentialsProvider credentialsProvider = new StsCerberusCredentialsProvider(cerberusUrl, testRegion, chain);
	
	        Map<String, String> headers = credentialsProvider.getSignedHeaders();
	        assertThat(headers).isNotNull();
	        assertThat(headers.get("Authorization")).isNotEmpty();
	        assertThat(headers.get("X-Amz-Date")).isNotEmpty();
	        assertThat(headers.get("X-Amz-Security-Token")).isNotEmpty();
	        assertThat(headers.get("Host")).isNotEmpty();
        }finally {
        	mockWebServer.close();
		}
    }


    @Test
    public void get_token_returns_token() throws IOException {

        when(chain.getCredentials()).thenReturn(credentials);

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        try {
	        final String cerberusUrl = "http://localhost:" + mockWebServer.getPort();
	        StsCerberusCredentialsProvider credentialsProvider = new StsCerberusCredentialsProvider(cerberusUrl, REGION_STRING_EAST, chain);
	
	        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(DECODED_AUTH_DATA));
	        CerberusAuthResponse token = credentialsProvider.getToken();
	        assertThat(token).isNotNull();
	        assertThat(StringUtils.isNotEmpty(token.getClientToken()));
	        
	    }finally {
	    	mockWebServer.close();
		}
    }

    /*
    @Test(expected = CerberusClientException.class)
    public void get_token_throws_exception_timeout() throws IOException {

        when(chain.getCredentials()).thenReturn(credentials);

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        try {
	        final String cerberusUrl = "http://localhost:" + mockWebServer.getPort();
	        StsCerberusCredentialsProvider credentialsProvider = new StsCerberusCredentialsProvider(cerberusUrl, REGION_STRING_EAST, chain);
	
	        CerberusAuthResponse token = credentialsProvider.getToken();
	        assertThat(token).isNotNull();
	        assertThat(StringUtils.isNotEmpty(token.getClientToken()));
	    }finally {
	    	mockWebServer.close();
		}
    }
    */

    @Test(expected = CerberusClientException.class)
    public void get_token_throws_exception_when_url_is_blank(){

        StsCerberusCredentialsProvider credentialsProvider = new StsCerberusCredentialsProvider(cerberusUrl, REGION_STRING_EAST, chain);
        CerberusAuthResponse token = credentialsProvider.getToken();
        assertThat(token).isNotNull();
        assertThat(StringUtils.isNotEmpty(token.getClientToken()));
    }

    @Test(expected = CerberusClientException.class)
    public void get_token_throws_exception_when_response_is_bad() throws IOException {

        when(chain.getCredentials()).thenReturn(credentials);

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        try {
	        final String cerberusUrl = "http://localhost:" + mockWebServer.getPort();
	        StsCerberusCredentialsProvider credentialsProvider = new StsCerberusCredentialsProvider(cerberusUrl, REGION_STRING_EAST, chain);
	        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody(ERROR_RESPONSE));
	
	        CerberusAuthResponse token = credentialsProvider.getToken();
	        assertThat(token).isNotNull();
	        assertThat(StringUtils.isNotEmpty(token.getClientToken()));
	    }finally {
	    	mockWebServer.close();
		}
    }

    @Test(expected = CerberusClientException.class)
    public void authenticate_throws_exception_when_token_is_null() {

        StsCerberusCredentialsProvider credentialsProvider = new StsCerberusCredentialsProvider(cerberusUrl, REGION_STRING_EAST);
        CerberusAuthResponse token = mock(CerberusAuthResponse.class);
        when(token.getClientToken()).thenReturn(null);
        credentialsProvider.authenticate();
    }
}

