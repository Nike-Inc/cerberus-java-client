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

package com.nike.cerberus.client.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.domain.SecureDataVersionSummary;
import com.nike.cerberus.client.domain.SecureDataVersionsResult;
import com.nike.cerberus.client.exception.CerberusClientException;
import com.nike.cerberus.client.exception.CerberusServerApiException;
import com.nike.cerberus.client.factory.CerberusClientFactory;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class CerberusClientSecretVersionsTest extends AbstractClientTest{

    private CerberusClient cerberusClient;
    private MockWebServer mockWebServer;
    private String cerberusUrl;

    @Before
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        cerberusUrl = "http://localhost:" + mockWebServer.getPort();
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

    /*
     * getVersionPathsForSdb
     */
    
    @Test
    public void getVersionPathsForSdb_returns_securedataversionsresult() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("versions","get-secure-data-versions-result"));
        mockWebServer.enqueue(response);

        SecureDataVersionsResult responce = cerberusClient.getVersionPathsForSdb("some-category", "some-sdb", "some-path");

        assertThat(responce).isNotNull();
        assertThat(responce.toString()).isNotNull();
        assertThat(responce.isHasNext()).isFalse();
        
        assertThat(responce.getLimit()).isEqualTo(100);
        assertThat(responce.getOffset()).isEqualTo(0);
        assertThat(responce.getNextOffset()).isEqualTo(10);
        
        assertThat(responce.getVersionCountInResult()).isEqualTo(4);
        assertThat(responce.getTotalVersionCount()).isEqualTo(4);
        
        SecureDataVersionSummary version = responce.getSecureDataVersionSummaries().get(0);
        
        assertThat(version).isNotNull();
        assertThat(version.toString()).isNotNull();
        assertThat(version.getAction()).isNotNull();
        assertThat(version.getActionPrincipal()).isNotNull();
		assertThat(version.getActionTs()).isNotNull();
		assertThat(version.getClass()).isNotNull();
		assertThat(version.getId()).isNotNull();
		assertThat(version.getPath()).isNotNull();
		assertThat(version.getSdboxId()).isNotNull();
		assertThat(version.getSizeInBytes()).isGreaterThan(0);
		assertThat(version.getType()).isNotNull();
		assertThat(version.getVersionCreatedBy()).isNotNull();
		assertThat(version.getVersionCreatedTs()).isNotNull();
    }
    
    @Test
    public void getVersionPathsForSdb_returns_securedataversionsresult_with_limit_and_offset() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("versions","get-secure-data-versions-result"));
        mockWebServer.enqueue(response);

        SecureDataVersionsResult responce = cerberusClient.getVersionPathsForSdb("some-category", "some-sdb", "some-path", 50,0);

        assertThat(responce).isNotNull();
        assertThat(responce.toString()).isNotNull();
    }
    
    @Test
    public void getVersionPathsForSdb_returns_securedataversionsresult_with_invalid_limit() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("versions","get-secure-data-versions-result"));
        mockWebServer.enqueue(response);

        SecureDataVersionsResult responce = cerberusClient.getVersionPathsForSdb("some-category", "some-sdb", "some-path", -1,0);

        assertThat(responce).isNotNull();
        assertThat(responce.toString()).isNotNull();
    }
    
    @Test
    public void getVersionPathsForSdb_returns_securedataversionsresult_with_invalid_offset() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("versions","get-secure-data-versions-result"));
        mockWebServer.enqueue(response);

        SecureDataVersionsResult responce = cerberusClient.getVersionPathsForSdb("some-category", "some-sdb", "some-path", 50,-1);

        assertThat(responce).isNotNull();
        assertThat(responce.toString()).isNotNull();
    }

    @Test(expected = CerberusServerApiException.class)
    public void getVersionPathsForSdb_throws_server_exception_if_response_is_not_ok() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        mockWebServer.enqueue(response);

        cerberusClient.getVersionPathsForSdb("some-category", "some-sdb", "some-path");
    }
    
    @Test(expected = CerberusClientException.class)
    public void getVersionPathsForSdb_throws_client_exception_if_response_is_not_ok_with_data() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        response.setBody("non-json");
        mockWebServer.enqueue(response);

        cerberusClient.getVersionPathsForSdb("some-category", "some-sdb", "some-path");
    }
    
	@Test(expected = CerberusClientException.class)
	public void  getVersionPathsForSdb_by_null_category() {
		cerberusClient.getVersionPathsForSdb(null, "some-sdb", "some-path");
	}
	
	@Test(expected = CerberusClientException.class)
	public void  getVersionPathsForSdb_by_null_sdbname() {
		cerberusClient.getVersionPathsForSdb("some-category", null, "some-path");
	}
	
	@Test(expected = CerberusClientException.class)
	public void  getVersionPathsForSdb_by_null_path() {
		cerberusClient.getVersionPathsForSdb("some-category", "some-sdb", null);
	}
    
}