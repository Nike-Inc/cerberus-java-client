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
import com.nike.cerberus.client.domain.Role;
import com.nike.cerberus.client.exception.CerberusClientException;
import com.nike.cerberus.client.exception.CerberusServerApiException;
import com.nike.cerberus.client.factory.CerberusClientFactory;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class CerberusClientRoleTest extends AbstractClientTest{

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
     * getRoles
     */
    
    @Test
    public void getRoles_returns_list_of_roles() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("role","list-role"));
        mockWebServer.enqueue(response);

        List<Role> responce = cerberusClient.getRoles();

        assertThat(responce).isNotNull();
        assertThat(responce).isNotEmpty();
        
        Role role = responce.get(0);
        
        assertThat(role).isNotNull();
        assertThat(role.getId()).isNotNull();
        assertThat(role.getName()).isNotNull();
        
        assertThat(role.getCreatedTs()).isNotNull();
        assertThat(role.getCreatedBy()).isNotNull();
        
        assertThat(role.getLastUpdatedBy()).isNotNull();
        assertThat(role.getLastUpdatedTs()).isNotNull();
    }

    @Test(expected = CerberusServerApiException.class)
    public void getRoles_throws_server_exception_if_response_is_not_ok() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        mockWebServer.enqueue(response);

        cerberusClient.getRoles();
    }
    
    @Test(expected = CerberusClientException.class)
    public void getRoles_throws_client_exception_if_response_is_not_ok_with_data() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        response.setBody("non-json");
        mockWebServer.enqueue(response);

        cerberusClient.getRoles();
    }
    
    /*
     * getRole
     */
    
    @Test
    public void getRole_returns_role_by_id() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(getResponseJson("role","get-role"));
        mockWebServer.enqueue(response);

        Role role = cerberusClient.getRole("some-id");

        assertThat(role).isNotNull();
        assertThat(role.toString()).isNotNull();
        
        assertThat(role.getId()).isNotNull();
        assertThat(role.getName()).isNotNull();
        
        assertThat(role.getCreatedTs()).isNotNull();
        assertThat(role.getCreatedBy()).isNotNull();
        
        assertThat(role.getLastUpdatedBy()).isNotNull();
        assertThat(role.getLastUpdatedTs()).isNotNull();
    }

    @Test(expected = CerberusServerApiException.class)
    public void getRole_throws_server_exception_if_response_is_not_ok() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        mockWebServer.enqueue(response);

        cerberusClient.getRole("some-id");
    }
    
    @Test(expected = CerberusClientException.class)
    public void getRole_throws_client_exception_if_response_is_not_ok_with_data() {
        final MockResponse response = new MockResponse();
        response.setResponseCode(404);
        response.setBody("non-json");
        mockWebServer.enqueue(response);

        cerberusClient.getRole("some-id");
    }

    
}