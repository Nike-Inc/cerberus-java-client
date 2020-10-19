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
import com.nike.cerberus.client.domain.AuthKmsKeyMetadata;
import com.nike.cerberus.client.domain.AuthKmsKeyMetadataResult;
import com.nike.cerberus.client.exception.CerberusClientException;
import com.nike.cerberus.client.exception.CerberusServerApiException;
import com.nike.cerberus.client.factory.CerberusClientFactory;
import com.nike.cerberus.client.model.AdminOverrideOwner;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class CerberusClientAdminTest extends AbstractClientTest{

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
     * getAuthenticationKmsMetadata
     */
    
    @Test
    public void getAuthenticationKmsMetadata_returns_authkmskeymetadataresult() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("admin","get-authentication-kms-metadata"));
        mockWebServer.enqueue(response);

        AuthKmsKeyMetadataResult responce = cerberusClient.getAuthenticationKmsMetadata();

        assertThat(responce).isNotNull();
        assertThat(responce.getAuthenticationKmsKeyMetadata()).isNotEmpty();
        
        AuthKmsKeyMetadata metadata = responce.getAuthenticationKmsKeyMetadata().get(0);
        
        assertThat(metadata.getAwsIamRoleArn()).isNotNull();
        assertThat(metadata.getAwsKmsKeyId()).isNotNull();
		assertThat(metadata.getAwsRegion()).isNotNull();
		assertThat(metadata.getCreatedTs()).isNotNull();
		assertThat(metadata.getLastUpdatedTs()).isNotNull();
		assertThat(metadata.getLastValidatedTs()).isNotNull();
    }

    @Test(expected = CerberusServerApiException.class)
    public void getRoles_throws_server_exception_if_response_is_not_ok() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        mockWebServer.enqueue(response);

        cerberusClient.getAuthenticationKmsMetadata();
    }
    
    @Test(expected = CerberusClientException.class)
    public void getRoles_throws_client_exception_if_response_is_not_ok_with_data() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        response.setBody("non-json");
        mockWebServer.enqueue(response);

        cerberusClient.getAuthenticationKmsMetadata();
    }
    
    /*
     * overrideOwner
     */
	@Test
	public void overrideOwnery_by_adminoverride_owner() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(204);
		mockWebServer.enqueue(response);
		
		AdminOverrideOwner owner = new AdminOverrideOwner();
		owner.setName("some-name");
		owner.setOwner("some-owner");
		
		cerberusClient.overrideOwner(owner);
	}

	@Test(expected = CerberusServerApiException.class)
	public void overrideOwnery_throws_server_exception_if_response_is_not_ok() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(403);
		mockWebServer.enqueue(response);
		
		AdminOverrideOwner owner = new AdminOverrideOwner();
		owner.setName("some-name");
		owner.setOwner("some-owner");
		
		cerberusClient.overrideOwner(owner);
	}
	
	@Test(expected = CerberusClientException.class)
	public void overrideOwnery_by_null() {
		cerberusClient.overrideOwner(null);
	}
    
}