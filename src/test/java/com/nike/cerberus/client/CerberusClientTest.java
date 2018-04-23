/*
 * Copyright (c) 2016 Nike, Inc.
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
import com.nike.cerberus.client.auth.CerberusCredentials;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.http.HttpStatus;
import com.nike.cerberus.client.model.CerberusClientTokenResponse;
import com.nike.cerberus.client.model.CerberusListResponse;
import com.nike.cerberus.client.model.CerberusResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the CerberusClient class
 */
public class CerberusClientTest {

    private CerberusClient cerberusClient;

    private MockWebServer mockWebServer;

    @Before
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        final String cerberusUrl = "http://localhost:" + mockWebServer.getPort();
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        cerberusClient = CerberusClientFactory.getClient(
                new StaticCerberusUrlResolver(cerberusUrl),
                cerberusCredentialsProvider);

        when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());
    }

    @After
    public void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_throws_error_if_no_resolver_set() {
        new CerberusClient(null,
                new DefaultCerberusCredentialsProviderChain(),
                new OkHttpClient.Builder().build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_throws_error_if_no_creds_provider() {
        new CerberusClient(new DefaultCerberusUrlResolver(),
                null,
                new OkHttpClient.Builder().build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_throws_error_if_no_http_client() {
        new CerberusClient(new DefaultCerberusUrlResolver(),
                new DefaultCerberusCredentialsProviderChain(),
                null);
    }

    @Test
    public void list_returns_map_of_keys_for_specified_path_if_exists() throws IOException {
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
    public void list_returns_an_empty_response_if_cerberus_returns_a_404() throws IOException {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        mockWebServer.enqueue(response);

        CerberusListResponse cerberusListResponse = cerberusClient.list("app/demo");

        assertThat(cerberusListResponse).isNotNull();
        assertThat(cerberusListResponse.getKeys()).isEmpty();
    }

    @Test
    public void read_returns_map_of_data_for_specified_path_if_exists() throws IOException {
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
    public void read_throws_cerberus_server_exception_if_response_is_not_ok() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        response.setBody(getResponseJson("error"));
        mockWebServer.enqueue(response);

        try {
            cerberusClient.read("app/not-found-path");
        } catch (CerberusServerException se) {
            assertThat(se.getCode()).isEqualTo(404);
            assertThat(se.getErrors()).hasSize(2);
        }
    }

    @Test(expected = CerberusClientException.class)
    public void read_throws_runtime_exception_if_unexpected_error_encountered() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        final String cerberusUrl = "http://localhost:" + serverSocket.getLocalPort();
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        final OkHttpClient httpClient = buildHttpClient(1, TimeUnit.SECONDS);
        cerberusClient = new CerberusClient(new StaticCerberusUrlResolver(cerberusUrl), cerberusCredentialsProvider, httpClient);

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
        } catch (CerberusServerException se) {
            assertThat(se.getCode()).isEqualTo(403);
            assertThat(se.getErrors()).hasSize(2);
        }
    }

    @Test(expected = CerberusClientException.class)
    public void write_throws_runtime_exception_if_unexpected_error_encountered() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        final String cerberusUrl = "http://localhost:" + serverSocket.getLocalPort();
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        final OkHttpClient httpClient = buildHttpClient(1, TimeUnit.SECONDS);
        cerberusClient = new CerberusClient(new StaticCerberusUrlResolver(cerberusUrl), cerberusCredentialsProvider, httpClient);

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
        } catch (CerberusServerException se) {
            assertThat(se.getCode()).isEqualTo(403);
            assertThat(se.getErrors()).hasSize(2);
        }
    }

    @Test(expected = CerberusClientException.class)
    public void delete_throws_runtime_exception_if_unexpected_error_encountered() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(0);
        final String cerberusUrl = "http://localhost:" + serverSocket.getLocalPort();
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        final OkHttpClient httpClient = buildHttpClient(1, TimeUnit.SECONDS);
        cerberusClient = new CerberusClient(new StaticCerberusUrlResolver(cerberusUrl), cerberusCredentialsProvider, httpClient);

        when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());

        cerberusClient.delete("app/api-key");
    }

    @Test
    public void build_request_includes_default_headers() throws IOException {
        final String headerKey = "headerKey";
        final String headerValue = "headerValue";
        final Headers headers = new Headers.Builder().add(headerKey, headerValue).build();

        final String cerberusUrl = "http://localhost:" + mockWebServer.getPort();
        final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
        when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());
        final OkHttpClient httpClient = buildHttpClient(1, TimeUnit.SECONDS);
        cerberusClient = new CerberusClient(new StaticCerberusUrlResolver(cerberusUrl), cerberusCredentialsProvider, httpClient, headers);

        Request result = cerberusClient.buildRequest(HttpUrl.parse(cerberusUrl), "get", null);

        assertThat(result.headers().get(headerKey)).isEqualTo(headerValue);
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