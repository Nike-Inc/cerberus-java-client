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
import com.nike.cerberus.client.domain.SDBMetadata;
import com.nike.cerberus.client.domain.SDBMetadataResult;
import com.nike.cerberus.client.exception.CerberusClientException;
import com.nike.cerberus.client.exception.CerberusServerApiException;
import com.nike.cerberus.client.factory.CerberusClientFactory;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class CerberusClientMetadataTest extends AbstractClientTest {

	private CerberusClient cerberusClient;
	private MockWebServer mockWebServer;
	private String cerberusUrl;

	@Before
	public void setup() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		cerberusUrl = "http://localhost:" + mockWebServer.getPort();
		final CerberusCredentialsProvider cerberusCredentialsProvider = mock(CerberusCredentialsProvider.class);
		cerberusClient = CerberusClientFactory.getClient(cerberusUrl, cerberusCredentialsProvider);

		when(cerberusCredentialsProvider.getCredentials()).thenReturn(new TestCerberusCredentials());
	}

	@After
	public void teardown() throws IOException {
		mockWebServer.shutdown();
		mockWebServer.close();
	}

	/*
	 * getMetadata
	 */

	@Test
	public void getMetadata_returns_metadata() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(200);
		response.setBody(getResponseJson("metadata", "list-metadata"));
		mockWebServer.enqueue(response);

		SDBMetadataResult responce = cerberusClient.getMetadata();

		assertThat(responce).isNotNull();
		assertThat(responce.toString()).isNotNull();

		assertThat(responce.isHasNext()).isFalse();
		assertThat(responce.getNextOffset()).isEqualTo(0);
		assertThat(responce.getLimit()).isEqualTo(10);
		assertThat(responce.getOffset()).isEqualTo(0);
		assertThat(responce.getSdbCountInResult()).isEqualTo(3);
		assertThat(responce.getTotalSDBCount()).isEqualTo(3);

		assertThat(responce.getSafeDepositBoxMetadata()).isNotNull();
		assertThat(responce.getSafeDepositBoxMetadata()).isNotEmpty();
	}

	@Test(expected = CerberusServerApiException.class)
	public void getRoles_throws_server_exception_if_response_is_not_ok() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		mockWebServer.enqueue(response);

		cerberusClient.getMetadata();
	}

	@Test(expected = CerberusClientException.class)
	public void getRoles_throws_client_exception_if_response_is_not_ok_with_data() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		response.setBody("non-json");
		mockWebServer.enqueue(response);

		cerberusClient.getMetadata();
	}

	/*
	 * getMetadata by sdbname
	 */

	@Test
	public void getMetadata_by_sdb_name_returns_metadata() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(200);
		response.setBody(getResponseJson("metadata", "get-metadata"));
		mockWebServer.enqueue(response);

		SDBMetadataResult responce = cerberusClient.getMetadata("dev demo");

		assertThat(responce).isNotNull();
		assertThat(responce.toString()).isNotNull();

		assertThat(responce.isHasNext()).isFalse();
		assertThat(responce.getNextOffset()).isEqualTo(0);
		assertThat(responce.getLimit()).isEqualTo(10);
		assertThat(responce.getOffset()).isEqualTo(0);
		assertThat(responce.getSdbCountInResult()).isEqualTo(1);
		assertThat(responce.getTotalSDBCount()).isEqualTo(1);

		assertThat(responce.getSafeDepositBoxMetadata()).isNotNull();
		assertThat(responce.getSafeDepositBoxMetadata()).isNotEmpty();

		SDBMetadata metadata = responce.getSafeDepositBoxMetadata().get(0);
		assertThat(metadata).isNotNull();
		assertThat(metadata.toString()).isNotNull();

		assertThat(metadata.getCategory()).isNotNull();
		assertThat(metadata.getCreatedBy()).isNotNull();
		assertThat(metadata.getCreatedTs()).isNotNull();
		assertThat(metadata.getDescription()).isNotNull();
		assertThat(metadata.getId()).isNotNull();
		assertThat(metadata.getLastUpdatedBy()).isNotNull();
		assertThat(metadata.getLastUpdatedTs()).isNotNull();
		assertThat(metadata.getName()).isNotNull();
		assertThat(metadata.getOwner()).isNotNull();
		assertThat(metadata.getPath()).isNotNull();
		
		assertThat(metadata.getIamRolePermissions()).isNotNull();
		assertThat(metadata.getIamRolePermissions()).isNotEmpty();
		
		assertThat(metadata.getUserGroupPermissions()).isNotNull();
		assertThat(metadata.getUserGroupPermissions()).isNotEmpty();
		
		assertThat(metadata.getData()).isNotNull();
		assertThat(metadata.getData()).isNotEmpty();
	}

	@Test(expected = CerberusServerApiException.class)
	public void getMetadata_by_sdb_name_throws_server_exception_if_response_is_not_ok() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		mockWebServer.enqueue(response);

		cerberusClient.getMetadata();
	}

	@Test(expected = CerberusClientException.class)
	public void getMetadata_by_sdb_name_throws_client_exception_if_response_is_not_ok_with_data() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		response.setBody("non-json");
		mockWebServer.enqueue(response);

		cerberusClient.getMetadata();
	}
	
	/*
	 * getMetadata by sdbname with limit and offset
	 */

	@Test
	public void getMetadata_by_sdb_name_and_limit_and_offset_returns_metadata() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(200);
		response.setBody(getResponseJson("metadata", "get-metadata"));
		mockWebServer.enqueue(response);

		SDBMetadataResult responce = cerberusClient.getMetadata("dev demo", 10,0);
		assertThat(responce).isNotNull();
		assertThat(responce.toString()).isNotNull();
	}
	
	@Test
	public void getMetadata_by_sdb_name_and_limit_returns_metadata() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(200);
		response.setBody(getResponseJson("metadata", "get-metadata"));
		mockWebServer.enqueue(response);

		SDBMetadataResult responce = cerberusClient.getMetadata("dev demo", 10,-1);
		assertThat(responce).isNotNull();
		assertThat(responce.toString()).isNotNull();
	}
	
	@Test
	public void getMetadata_by_sdb_name_and_offset_returns_metadata() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(200);
		response.setBody(getResponseJson("metadata", "get-metadata"));
		mockWebServer.enqueue(response);

		SDBMetadataResult responce = cerberusClient.getMetadata("dev demo", 0,0);
		assertThat(responce).isNotNull();
		assertThat(responce.toString()).isNotNull();
	}

	@Test(expected = CerberusServerApiException.class)
	public void getMetadata_by_sdb_name_and_limit_and_offset_throws_server_exception_if_response_is_not_ok() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		mockWebServer.enqueue(response);

		cerberusClient.getMetadata("dev demo", 10,0);
	}

	@Test(expected = CerberusClientException.class)
	public void getMetadata_by_sdb_name_and_limit_and_offset_throws_client_exception_if_response_is_not_ok_with_data() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		response.setBody("non-json");
		mockWebServer.enqueue(response);

		cerberusClient.getMetadata("dev demo", 10,0);
	}

}