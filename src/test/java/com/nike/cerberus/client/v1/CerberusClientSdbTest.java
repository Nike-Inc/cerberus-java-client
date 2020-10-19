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
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import com.nike.cerberus.client.domain.IamRolePermission;
import com.nike.cerberus.client.domain.SafeDepositBoxSummary;
import com.nike.cerberus.client.domain.SafeDepositBoxV1;
import com.nike.cerberus.client.domain.UserGroupPermission;
import com.nike.cerberus.client.exception.CerberusClientException;
import com.nike.cerberus.client.exception.CerberusServerApiException;
import com.nike.cerberus.client.factory.CerberusClientFactory;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class CerberusClientSdbTest extends AbstractClientTest{

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
     * getSafeDepositBoxes
     */
    
    @Test
    public void getSafeDepositBoxes_returns_list_safe_deposit_box_summary() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("sdb","list-v1-sdb"));
        mockWebServer.enqueue(response);

        List<SafeDepositBoxSummary> responce = cerberusClient.getSafeDepositBoxes();

        assertThat(responce).isNotNull();
        assertThat(responce).isNotEmpty();
        
        SafeDepositBoxSummary summary = responce.get(0);
        
        assertThat(summary.getId()).isNotNull();
        assertThat(summary.getName()).isNotNull();
        assertThat(summary.getPath()).isNotNull();
        assertThat(summary.getCategoryId()).isNotNull();
        
    }

    @Test(expected = CerberusServerApiException.class)
    public void getSafeDepositBoxes_throws_server_exception_if_response_is_not_ok() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        mockWebServer.enqueue(response);

        cerberusClient.getSafeDepositBoxes();
    }
    
    @Test(expected = CerberusClientException.class)
    public void getSafeDepositBoxes_throws_client_exception_if_response_is_not_ok_with_data() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        response.setBody("non-json");
        mockWebServer.enqueue(response);

        cerberusClient.getSafeDepositBoxes();
    }
    
    /*
     * getRole
     */
    
    @Test
    public void getSafeDepositBox_returns_sdb_by_id() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("sdb","get-v1-sdb"));
        mockWebServer.enqueue(response);

        SafeDepositBoxV1 sdb = cerberusClient.getSafeDepositBox("some-id");

        assertThat(sdb).isNotNull();
        assertThat(sdb.toString()).isNotNull();
        
		assertThat(sdb.getCategoryId()).isNotNull();
		assertThat(sdb.getCreatedBy()).isNotNull();
		assertThat(sdb.getCreatedTs()).isNotNull();
		assertThat(sdb.getDescription()).isNotNull();
		
		assertThat(sdb.getId()).isNotNull();
		assertThat(sdb.getLastUpdatedBy()).isNotNull();
		assertThat(sdb.getLastUpdatedTs()).isNotNull();
		assertThat(sdb.getName()).isNotNull();
		assertThat(sdb.getOwner()).isNotNull();
		assertThat(sdb.getPath()).isNotNull();
		
		assertThat(sdb.getUserGroupPermissions()).isNotNull();
		assertThat(sdb.getUserGroupPermissions()).isNotEmpty();
		
		// userGroupPermission
		UserGroupPermission userGroupPermission = sdb.getUserGroupPermissions().iterator().next();
		assertThat(userGroupPermission).isNotNull();
		
		assertThat(userGroupPermission.getCreatedBy()).isNotNull();
		assertThat(userGroupPermission.getCreatedTs()).isNotNull();
		assertThat(userGroupPermission.getId()).isNotNull();
		assertThat(userGroupPermission.getLastUpdatedBy()).isNotNull();
		assertThat(userGroupPermission.getLastUpdatedTs()).isNotNull();
		assertThat(userGroupPermission.getName()).isNotNull();
		assertThat(userGroupPermission.getRoleId()).isNotNull();
		
		// iamRolePermission
		assertThat(sdb.getIamRolePermissions()).isNotNull();
		assertThat(sdb.getIamRolePermissions()).isNotEmpty();
		
		IamRolePermission iamRolePermission = sdb.getIamRolePermissions().iterator().next();
		assertThat(iamRolePermission).isNotNull();
		
		assertThat(iamRolePermission.getAccountId()).isNotNull();
		assertThat(iamRolePermission.getCreatedBy()).isNotNull();
		assertThat(iamRolePermission.getCreatedTs()).isNotNull();
		assertThat(iamRolePermission.getIamRoleName()).isNotNull();
		assertThat(iamRolePermission.getId()).isNotNull();
		assertThat(iamRolePermission.getLastUpdatedBy()).isNotNull();
		assertThat(iamRolePermission.getLastUpdatedTs()).isNotNull();
		assertThat(iamRolePermission.getRoleId()).isNotNull();
		
    }

    @Test(expected = CerberusServerApiException.class)
    public void getSafeDepositBox_throws_server_exception_if_response_is_not_ok() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        mockWebServer.enqueue(response);

        cerberusClient.getSafeDepositBox("some-id");
    }
    
    @Test(expected = CerberusClientException.class)
    public void getSafeDepositBox_throws_client_exception_if_response_is_not_ok_with_data() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        response.setBody("non-json");
        mockWebServer.enqueue(response);

        cerberusClient.getSafeDepositBox("some-id");
    }
    
	@Test(expected = CerberusClientException.class)
	public void getSafeDepositBox_by_null() {
		cerberusClient.getSafeDepositBox(null);
	}
    
}