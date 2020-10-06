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

import com.nike.cerberus.client.auth.CerberusCredentials;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;
import com.nike.cerberus.client.exception.CerberusClientException;
import com.nike.cerberus.client.exception.CerberusServerApiException;
import com.nike.cerberus.client.factory.CerberusClientFactory;
import com.nike.cerberus.client.model.CerberusListResponse;
import com.nike.cerberus.client.model.CerberusResponse;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.nike.cerberus.client.CerberusClient.DEFAULT_NUM_RETRIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the CerberusClient class
 */
public class CerberusClientTest {

    private CerberusClient cerberusClient;

    private MockWebServer mockWebServer;

    private String cerberusUrl;

    private String region;

    @Before
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        cerberusUrl = "http://localhost:" + mockWebServer.getPort();
        region = "us-west-2";
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        cerberusClient = CerberusClientFactory.getClient(
                cerberusUrl,
                cerberusCredentialsProvider);

        when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());
    }

    @After
    public void teardown() throws IOException {
        mockWebServer.shutdown();
        mockWebServer.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_throws_error_if_no_url_set() {
        new CerberusClient(null,
                new DefaultCerberusCredentialsProviderChain(cerberusUrl, region),
                new OkHttpClient.Builder().build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_throws_error_if_no_creds_provider() {
        new CerberusClient(cerberusUrl,
                null,
                new OkHttpClient.Builder().build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_throws_error_if_no_http_client() {
        new CerberusClient(cerberusUrl,
                new DefaultCerberusCredentialsProviderChain(cerberusUrl, region),
                null);
    }

    @Test
    public void list_returns_map_of_keys_for_specified_path_if_exists() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("list"));
        mockWebServer.enqueue(response);

        CerberusListResponse cerberusListResponse = cerberusClient.list("app/demo");

        assertThat(cerberusListResponse).isNotNull();
        assertThat(cerberusListResponse.getKeys()).isNotEmpty();
        assertThat(cerberusListResponse.getKeys()).contains("foo", "foo/");
    }

    @Test
    public void list_returns_an_empty_response_if_cerberus_returns_a_404() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        mockWebServer.enqueue(response);

        CerberusListResponse cerberusListResponse = cerberusClient.list("app/demo");

        assertThat(cerberusListResponse).isNotNull();
        assertThat(cerberusListResponse.getKeys()).isEmpty();
    }

    @Test
    public void read_returns_map_of_data_for_specified_path_if_exists() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("secret"));
        mockWebServer.enqueue(response);

        CerberusResponse cerberusResponse = cerberusClient.read("app/api-key");

        assertThat(cerberusResponse).isNotNull();
        assertThat(cerberusResponse.getData().containsKey("value")).isTrue();
        assertThat(cerberusResponse.getData().get("value")).isEqualToIgnoringCase("world");
    }

    @Test
    public void read_does_not_retry_on_200() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(getResponseJson("secret")));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody(getResponseJson("error")));

        CerberusResponse cerberusResponse = cerberusClient.read("app/api-key");

        assertThat(cerberusResponse).isNotNull();
        assertThat(cerberusResponse.getData().containsKey("value")).isTrue();
        assertThat(cerberusResponse.getData().get("value")).isEqualToIgnoringCase("world");
    }

    @Test
    public void read_retries_on_500_errors() {
        for (int i = 0; i < DEFAULT_NUM_RETRIES - 1; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody(getResponseJson("error")));
        }
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(getResponseJson("secret")));

        CerberusResponse cerberusResponse = cerberusClient.read("app/api-key");

        assertThat(cerberusResponse).isNotNull();
        assertThat(cerberusResponse.getData().containsKey("value")).isTrue();
        assertThat(cerberusResponse.getData().get("value")).isEqualToIgnoringCase("world");
    }

    @Test
    public void read_retries_on_IOException() throws IOException {

        String url = "http://localhost:" + mockWebServer.getPort();

        OkHttpClient httpClient = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        when(call.execute()).thenThrow(new IOException());
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());

        CerberusClient cerberusClient = new CerberusClient(url, cerberusCredentialsProvider, httpClient);
        try {
            cerberusClient.read("app/api-key");

            // code should not reach this point, throw an error if it does
            throw new AssertionError("Expected CerberusClientException, but was not thrown");
        } catch(CerberusClientException cce) {  // catch this error so that the remaining tests will run
            // ensure that error is thrown because of mocked IOException
            if ( !(cce.getCause() instanceof IOException) ) {
                throw new AssertionError("Expected error cause to be IOException, but was " + cce.getCause().getClass());
            }
        }

        verify(httpClient, times(DEFAULT_NUM_RETRIES)).newCall(any(Request.class));
    }

    @Test
    public void read_throws_cerberus_server_exception_if_response_is_not_ok() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        response.setBody(getResponseJson("error"));
        mockWebServer.enqueue(response);

        try {
            cerberusClient.read("app/not-found-path");
        } catch (CerberusServerApiException se) {
            assertThat(se.getCode()).isEqualTo(404);
            assertThat(se.getErrors()).hasSize(1);
        }
    }

    @Test(expected = CerberusClientException.class)
    public void read_throws_runtime_exception_if_unexpected_error_encountered() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        final String cerberusUrl = "http://localhost:" + serverSocket.getLocalPort();
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        final OkHttpClient httpClient = buildHttpClient(1, TimeUnit.SECONDS);
        CerberusClient cerberusClient = new CerberusClient(cerberusUrl, cerberusCredentialsProvider, httpClient);

        when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());

        cerberusClient.read("app/api-key");
    }

    @Test
    public void write_returns_gives_no_error_if_write_204_returned() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(204);
        mockWebServer.enqueue(response);

        Map<String, String> data = new HashMap<>();
        data.put("key", "value");
        cerberusClient.write("app/api-key", data);
    }

    @Test
    public void write_throws_cerberus_server_exception_if_response_is_not_204() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(403);
        response.setBody(getResponseJson("error"));
        mockWebServer.enqueue(response);

        try {
            Map<String, String> data = new HashMap<>();
            data.put("key", "value");
            cerberusClient.write("app/not-allowed", data);
        } catch (CerberusServerApiException se) {
            assertThat(se.getCode()).isEqualTo(403);
            assertThat(se.getErrors()).hasSize(1);
        }
    }

    @Test(expected = CerberusClientException.class)
    public void write_throws_runtime_exception_if_unexpected_error_encountered() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        final String cerberusUrl = "http://localhost:" + serverSocket.getLocalPort();
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        final OkHttpClient httpClient = buildHttpClient(1, TimeUnit.SECONDS);
        cerberusClient = new CerberusClient(cerberusUrl, cerberusCredentialsProvider, httpClient);

        when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());

        Map<String, String> data = new HashMap<>();
        data.put("key", "value");
        cerberusClient.write("app/api-key", data);
    }

    @Test
    public void delete_returns_gives_no_error_if_write_204_returned() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(204);
        mockWebServer.enqueue(response);

        cerberusClient.delete("app/api-key");
    }

    @Test
    public void delete_throws_cerberus_server_exception_if_response_is_not_204() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(403);
        response.setBody(getResponseJson("error"));
        mockWebServer.enqueue(response);

        try {
            cerberusClient.delete("app/not-allowed");
        } catch (CerberusServerApiException se) {
            assertThat(se.getCode()).isEqualTo(403);
            assertThat(se.getErrors()).hasSize(1);
        }
    }

    @Test(expected = CerberusClientException.class)
    public void delete_throws_runtime_exception_if_unexpected_error_encountered() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        final String cerberusUrl = "http://localhost:" + serverSocket.getLocalPort();
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        final OkHttpClient httpClient = buildHttpClient(1, TimeUnit.SECONDS);
        cerberusClient = new CerberusClient(cerberusUrl, cerberusCredentialsProvider, httpClient);

        when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());

        cerberusClient.delete("app/api-key");
    }

    @Test
    public void build_request_includes_default_headers() {
        final String headerKey = "headerKey";
        final String headerValue = "headerValue";
        final Headers headers = new Headers.Builder().add(headerKey, headerValue).build();

        final String cerberusUrl = "http://localhost:" + mockWebServer.getPort();
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());
        final OkHttpClient httpClient = buildHttpClient(1, TimeUnit.SECONDS);
        cerberusClient = new CerberusClient(cerberusUrl, cerberusCredentialsProvider, httpClient, headers);

        Request result = cerberusClient.buildRequest(HttpUrl.parse(cerberusUrl), "get", null);

        assertThat(result.headers().get(headerKey)).isEqualTo(headerValue);
    }

    @Test
    public void buildUrl_properly_adds_limit_and_offset() {
        String prefix = "prefix/";
        String path = "path";
        Integer limit = 1000;
        Integer offset = 2;
        HttpUrl urlWithNoLimitOrOffset = cerberusClient.buildUrl(prefix, path, null, null);
        HttpUrl urlWithLimitAndNoOffset = cerberusClient.buildUrl(prefix, path, limit, null);
        HttpUrl urlWithOffsetAndNoLimit = cerberusClient.buildUrl(prefix, path, null, offset);
        HttpUrl urlWithLimitAndOffset = cerberusClient.buildUrl(prefix, path, limit, offset);

        assertTrue(urlWithNoLimitOrOffset.toString().endsWith(String.format("%s%s", prefix, path)));
        assertTrue(urlWithLimitAndNoOffset.toString().endsWith(String.format("%s%s?limit=%s", prefix, path, limit)));
        assertTrue(urlWithOffsetAndNoLimit.toString().endsWith(String.format("%s%s?offset=%s", prefix, path, offset)));
        assertTrue(urlWithLimitAndOffset.toString().endsWith(String.format("%s%s?limit=%s&offset=%s", prefix, path, limit, offset)));
    }

    private OkHttpClient buildHttpClient(int timeout, TimeUnit timeoutUnit) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeout, timeoutUnit)
                .writeTimeout(timeout, timeoutUnit)
                .readTimeout(timeout, timeoutUnit)
                .build();
    }

    private String getResponseJson(final String title) {
        InputStream inputStream = getClass().getResourceAsStream(
                String.format("/com/nike/cerberus/client/%s.json", title));
        try {
            return IOUtils.toString(inputStream, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static class TestCerberusCredentials implements CerberusCredentials {
        @Override
        public String getToken() {
            return "TOKEN";
        }
    }
}