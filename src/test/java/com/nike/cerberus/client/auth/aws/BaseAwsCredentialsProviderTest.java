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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kms.AWSKMSClient;
import com.nike.cerberus.client.CerberusClientException;
import com.nike.cerberus.client.CerberusServerException;
import com.nike.cerberus.client.DefaultCerberusUrlResolver;
import com.nike.cerberus.client.UrlResolver;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.client.CredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.nike.cerberus.client.auth.aws.BaseAwsCredentialsProvider.DEFAULT_AUTH_RETRIES;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class BaseAwsCredentialsProviderTest extends BaseCredentialsProviderTest{
    public static final Region REGION = RegionUtils.getRegion("us-west-2");
    public static final String CERBERUS_TEST_ARN = "arn:aws:iam::123456789012:role/cerberus-test-role";
    public static final String ERROR_RESPONSE = "Error calling cerberus";
    public static final String SUCCESS_RESPONSE = "{\"auth_data\": \"\"}";

    protected static final String MISSING_AUTH_DATA = "{}";


    private BaseAwsCredentialsProvider provider;
    private UrlResolver urlResolver;
    private String cerberusUrl;
    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        urlResolver = mock(UrlResolver.class);

        provider = new TestAwsCredentialsProvider(urlResolver);

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        cerberusUrl = "http://localhost:" + mockWebServer.getPort();
    }

    @After
    public void tearDown() throws Exception {
        reset(urlResolver);
    }

    @Test(expected = CerberusClientException.class)
    public void getEncryptedAuthData_blank_url_throws_exception() throws Exception {
        when(urlResolver.resolve()).thenReturn("");

        provider.getEncryptedAuthData(CERBERUS_TEST_ARN, REGION);
    }

    @Test(expected = CerberusClientException.class)
    public void decryptToken_throws_exception_when_non_encrypted_data_provided() {
        provider.decryptToken(mock(AWSKMSClient.class), "non-encrypted-token");
    }

    @Test(expected = CerberusServerException.class)
    public void getEncryptedAuthData_throws_exception_on_bad_response_code() throws IOException {
        when(urlResolver.resolve()).thenReturn(cerberusUrl);

        System.setProperty(DefaultCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY, cerberusUrl);
        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody(ERROR_RESPONSE));

        provider.getEncryptedAuthData(CERBERUS_TEST_ARN, REGION);
    }

    @Test(expected = CerberusClientException.class)
    public void getEncryptedAuthData_throws_exception_on_missing_auth_data() throws IOException {
        when(urlResolver.resolve()).thenReturn(cerberusUrl);

        System.setProperty(DefaultCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY, cerberusUrl);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(MISSING_AUTH_DATA));

        provider.getEncryptedAuthData(CERBERUS_TEST_ARN, REGION);
    }

    @Test
    public void test_that_getEncryptedAuthData_retries_on_500_errors() {
        when(urlResolver.resolve()).thenReturn(cerberusUrl);

        for (int i = 0; i < DEFAULT_AUTH_RETRIES - 1; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody(ERROR_RESPONSE));
        }
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(SUCCESS_RESPONSE));

        provider.getEncryptedAuthData(CERBERUS_TEST_ARN, REGION);
    }

    @Test
    public void test_that_getEncryptedAuthData_retries_on_IOException_errors() throws IOException {
        when(urlResolver.resolve()).thenReturn(cerberusUrl);

        OkHttpClient httpClient = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        when(call.execute()).thenThrow(new IOException());
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        TestAwsCredentialsProvider provider = new TestAwsCredentialsProvider(urlResolver, httpClient);

        try {
            provider.getEncryptedAuthData(CERBERUS_TEST_ARN, REGION);
        } catch(CerberusClientException cce) {  // catch this error so that the remaining tests will run
            // ensure that error is thrown because of mocked IOException
            if ( !(cce.getCause() instanceof IOException) ) {
                throw new AssertionError("Expected error cause to be IOException, but was " + cce.getCause().getClass());
            }
        }

        verify(httpClient, times(DEFAULT_AUTH_RETRIES)).newCall(any(Request.class));
    }

    @Test
    public void test_that_getEncryptedAuthData_does_not_retry_on_200() {
        when(urlResolver.resolve()).thenReturn(cerberusUrl);

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(SUCCESS_RESPONSE));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody(ERROR_RESPONSE));

        provider.getEncryptedAuthData(CERBERUS_TEST_ARN, REGION);
    }

    class TestAwsCredentialsProvider extends BaseAwsCredentialsProvider {
        /**
         * Constructor to setup credentials provider using the specified
         * implementation of {@link UrlResolver}
         *
         * @param urlResolver Resolver for resolving the Cerberus URL
         */
        public TestAwsCredentialsProvider(UrlResolver urlResolver) {
            super(urlResolver);
        }

        public TestAwsCredentialsProvider(UrlResolver urlResolver, OkHttpClient client) {
            super(urlResolver, client);
        }

        @Override
        protected void authenticate() {

        }
    }

}