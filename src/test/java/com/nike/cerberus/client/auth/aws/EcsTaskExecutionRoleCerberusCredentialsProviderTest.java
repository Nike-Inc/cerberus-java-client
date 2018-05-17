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

import com.amazonaws.AmazonClientException;
import com.amazonaws.internal.EC2CredentialsUtils;
import com.amazonaws.services.kms.AWSKMSClient;
import com.nike.cerberus.client.CerberusClientException;
import com.nike.cerberus.client.DefaultCerberusUrlResolver;
import com.nike.cerberus.client.UrlResolver;
import com.nike.cerberus.client.auth.CerberusCredentials;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Tests the EcsTaskRoleCerberusCredentialsProvider class
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSKMSClient.class,
    EcsTaskRoleCerberusCredentialsProvider.class, EC2CredentialsUtils.class})
@PowerMockIgnore({"javax.management.*","javax.net.*"})
public class EcsTaskExecutionRoleCerberusCredentialsProviderTest extends BaseCredentialsProviderTest {

    private UrlResolver urlResolver;

    private AWSKMSClient kmsClient;

    private EcsTaskRoleCerberusCredentialsProvider provider;

    private EC2CredentialsUtils ec2CredentialsUtils;

    @Before
    public void setup() throws Exception {
        kmsClient = mock(AWSKMSClient.class);
        urlResolver = mock(UrlResolver.class);
        provider = new EcsTaskRoleCerberusCredentialsProvider(urlResolver);

        whenNew(AWSKMSClient.class).withAnyArguments().thenReturn(kmsClient);
        mockStatic(System.class);
        mockGetCredentialsRelativeUri();
        mockStatic(EC2CredentialsUtils.class);
        ec2CredentialsUtils = mock(EC2CredentialsUtils.class);
        when(EC2CredentialsUtils.getInstance()).thenReturn(ec2CredentialsUtils);
    }

    @Test
    public void getCredentials_returns_valid_credentials() throws IOException {

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        final String cerberusUrl = "http://localhost:" + mockWebServer.getPort();

        mockDecrypt(kmsClient, DECODED_AUTH_DATA);
        when(urlResolver.resolve()).thenReturn(cerberusUrl);

        System.setProperty(DefaultCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY, cerberusUrl);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(AUTH_RESPONSE));

        when(ec2CredentialsUtils.readResource(Mockito.any(URI.class)))
                .thenReturn("{\"RoleArn\":\"arn:aws:iam::123456789:role/ecsTaskExecutionRole\"}")
                .thenReturn("{\"TaskARN\":\"arn:aws:ecs:us-west-1:123456789:task/task-id\"}");

        CerberusCredentials credentials = provider.getCredentials();
        assertThat(credentials.getToken()).isEqualTo(AUTH_TOKEN);

    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_throws_client_exception_when_task_arn_is_missing() throws IOException {
        when(ec2CredentialsUtils.readResource(Mockito.any(URI.class)))
                .thenReturn("{}")
                .thenReturn("{\"TaskARN\":\"arn:aws:ecs:us-west-1:123456789:task/task-id\"}");
        provider.getCredentials();
    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_throws_client_exception_when_not_running_in_ecs_task() throws IOException{
        when(ec2CredentialsUtils.readResource(Mockito.any(URI.class))).thenThrow(new AmazonClientException("BAD"));
        provider.getCredentials();
    }

    private void mockGetCredentialsRelativeUri() {
        when(System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI")).thenReturn("/mockuri");
    }
}