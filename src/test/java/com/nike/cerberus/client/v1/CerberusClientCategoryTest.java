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
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.domain.Category;
import com.nike.cerberus.client.exception.CerberusClientException;
import com.nike.cerberus.client.exception.CerberusServerApiException;
import com.nike.cerberus.client.factory.CerberusClientFactory;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class CerberusClientCategoryTest extends AbstractClientTest {

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
	 * getCategories
	 */

	@Test
	public void getCategories_returns_list_of_categories() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(200);
		response.setBody(getResponseJson("category", "list-category"));
		mockWebServer.enqueue(response);

		List<Category> responce = cerberusClient.getCategories();

		assertThat(responce).isNotNull();
		assertThat(responce).isNotEmpty();

		Category category = responce.get(0);

		assertThat(category).isNotNull();
		assertThat(category.getId()).isNotNull();
		assertThat(category.getDisplayName()).isNotNull();
		assertThat(category.getPath()).isNotNull();

		assertThat(category.getCreatedTs()).isNotNull();
		assertThat(category.getCreatedBy()).isNotNull();

		assertThat(category.getLastUpdatedBy()).isNotNull();
		assertThat(category.getLastUpdatedTs()).isNotNull();
	}

	@Test(expected = CerberusServerApiException.class)
	public void getCategories_throws_server_exception_if_response_is_not_ok() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		mockWebServer.enqueue(response);

		cerberusClient.getCategories();
	}

	@Test(expected = CerberusClientException.class)
	public void getCategories_throws_client_exception_if_response_is_not_ok_with_data() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		response.setBody("non-json");
		mockWebServer.enqueue(response);

		cerberusClient.getCategories();
	}

	/*
	 * getCategory
	 */

	@Test
	public void getCategory_returns_category_by_id() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(200);
		response.setBody(getResponseJson("category", "get-category"));
		mockWebServer.enqueue(response);

		Category category = cerberusClient.getCategory("some-id");

		assertThat(category).isNotNull();
        assertThat(category.toString()).isNotNull();
		
		assertThat(category.getId()).isNotNull();
		assertThat(category.getDisplayName()).isNotNull();
		assertThat(category.getPath()).isNotNull();

		assertThat(category.getCreatedTs()).isNotNull();
		assertThat(category.getCreatedBy()).isNotNull();

		assertThat(category.getLastUpdatedBy()).isNotNull();
		assertThat(category.getLastUpdatedTs()).isNotNull();
	}
	
	@Test(expected = CerberusClientException.class)
	public void getCategory_by_null() {
		cerberusClient.getCategory(null);
	}

	@Test(expected = CerberusServerApiException.class)
	public void getCategory_throws_server_exception_if_response_is_not_ok() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		mockWebServer.enqueue(response);

		cerberusClient.getCategory("some-id");
	}

	@Test(expected = CerberusClientException.class)
	public void getCategory_throws_client_exception_if_response_is_not_ok_with_data() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		response.setBody("non-json");
		mockWebServer.enqueue(response);

		cerberusClient.getCategory("some-id");
	}

	/*
	 * deleteCategory
	 */

	@Test
	public void deleteCategory_by_id() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(200);
		mockWebServer.enqueue(response);
		cerberusClient.deleteCategory("some-id");
	}

	@Test
	public void deleteCategory_by_id_not_found() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		mockWebServer.enqueue(response);
		cerberusClient.deleteCategory("some-id");
	}

	@Test(expected = CerberusServerApiException.class)
	public void deleteCategory_throws_server_exception_if_response_is_not_ok() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(403);
		mockWebServer.enqueue(response);
		cerberusClient.deleteCategory("some-id");
	}
	
	@Test(expected = CerberusClientException.class)
	public void deleteCategory_by_null() {
		cerberusClient.deleteCategory(null);
	}

	/*
	 * createCategory
	 */

	@Test
	public void createCategory_returns_category_by_id() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(200);
		response.setBody(getResponseJson("category", "get-category"));
		mockWebServer.enqueue(response);

		Category category = cerberusClient.createCategory(buildSample());

		assertThat(category).isNotNull();
		assertThat(category.getId()).isNotNull();
		assertThat(category.getDisplayName()).isNotNull();
		assertThat(category.getPath()).isNotNull();

		assertThat(category.getCreatedTs()).isNotNull();
		assertThat(category.getCreatedBy()).isNotNull();

		assertThat(category.getLastUpdatedBy()).isNotNull();
		assertThat(category.getLastUpdatedTs()).isNotNull();
	}

	@Test(expected = CerberusClientException.class)
	public void createCategory_by_null() {
		cerberusClient.createCategory(null);
	}

	@Test(expected = CerberusServerApiException.class)
	public void createCategory_throws_server_exception_if_response_is_not_ok() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		mockWebServer.enqueue(response);

		cerberusClient.createCategory(buildSample());
	}

	@Test(expected = CerberusClientException.class)
	public void createCategory_throws_client_exception_if_response_is_not_ok_with_data() {
		final MockResponse response = new MockResponse();
		response.setResponseCode(404);
		response.setBody("non-json");
		mockWebServer.enqueue(response);
		
		cerberusClient.createCategory(buildSample());
	}

	private Category buildSample() {
		Category input = new Category();
		input.setCreatedBy("owner");
		input.setCreatedTs(OffsetDateTime.now());
		input.setDisplayName("find-me");
		input.setId("f7fff4d6-faaa-11e5-a8a9-7fa3b294cd46");
		input.setLastUpdatedBy("owner");
		input.setLastUpdatedTs(OffsetDateTime.now());
		input.setPath("some/path");
		return input;
	}

}