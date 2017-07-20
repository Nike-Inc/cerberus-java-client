/*
 * Copyright (c) 2017 Nike, Inc.
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

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationResult;
import com.nike.cerberus.client.DefaultCerberusUrlResolver;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.auth.VaultCredentials;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSKMSClient.class, Regions.class, AWSLambdaClient.class, LambdaRoleVaultCredentialsProvider.class})
@PowerMockIgnore({"javax.management.*", "javax.net.*"})
public class LambdaRoleVaultCredentialsProviderTest extends BaseCredentialsProviderTest {
    private static final String VALID_LAMBDA_ARN = "arn:aws:lambda:us-west-2:123456789012:function:lambda-test:1.1.0";
    private static final String VALID_LAMBDA_ARN_NO_QUALIFIER = "arn:aws:lambda:us-west-2:012345678912:function:lambda-test";
    private static final String VALID_IAM_ARN = "arn:aws:iam::123456789012:role/cerberus-role";
    private static final String INVALID_ARN = "invalid-arn";

    private AWSKMSClient kmsClient;
    private UrlResolver urlResolver;
    private AWSLambdaClient lambdaClient;
    private MockWebServer mockWebServer;
    private String vaultUrl;

    @Before
    public void setup() throws Exception {
        kmsClient = mock(AWSKMSClient.class);
        urlResolver = mock(UrlResolver.class);
        lambdaClient = mock(AWSLambdaClient.class);

        mockWebServer = new MockWebServer();
        mockWebServer.start();
        vaultUrl = "http://localhost:" + mockWebServer.getPort();

        when(urlResolver.resolve()).thenReturn(vaultUrl);


        mockStatic(Regions.class);

        when(Regions.getCurrentRegion()).thenReturn(RegionUtils.getRegion("us-west-2"));
        whenNew(AWSLambdaClient.class).withNoArguments().thenReturn(lambdaClient);
        whenNew(AWSKMSClient.class).withAnyArguments().thenReturn(kmsClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void provider_creation_fails_on_invalid_arn() {
        LambdaRoleVaultCredentialsProvider provider = new LambdaRoleVaultCredentialsProvider(urlResolver, "invalid-lambda-arn");
    }

    @Test
    public void valid_arn_and_no_qualifier_matched_properly_on_provider_creation() {
        LambdaRoleVaultCredentialsProvider provider = new LambdaRoleVaultCredentialsProvider(urlResolver, VALID_LAMBDA_ARN_NO_QUALIFIER);
    }

    @Test
    public void getCredentials_returns_valid_creds() throws Exception {
        final LambdaRoleVaultCredentialsProvider provider = PowerMockito.spy(new LambdaRoleVaultCredentialsProvider(urlResolver, VALID_LAMBDA_ARN));
        final GetFunctionConfigurationRequest request = new GetFunctionConfigurationRequest().withFunctionName("lambda-test").withQualifier("1.1.0");

        when(urlResolver.resolve()).thenReturn(vaultUrl);

        System.setProperty(DefaultCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY, vaultUrl);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(AUTH_RESPONSE));

        mockDecrypt(kmsClient, DECODED_AUTH_DATA);

        when(lambdaClient.getFunctionConfiguration(request)).thenReturn(new GetFunctionConfigurationResult().withRole(VALID_IAM_ARN));

        final VaultCredentials credentials = provider.getCredentials();

        assertThat(credentials.getToken()).isEqualTo(AUTH_TOKEN);
        verify(lambdaClient, times(1)).getFunctionConfiguration(request);
    }

    @Test(expected = VaultClientException.class)
    public void VaultClientException_thrown_when_bad_json_returned() throws Exception {
        final LambdaRoleVaultCredentialsProvider provider = PowerMockito.spy(new LambdaRoleVaultCredentialsProvider(urlResolver, VALID_LAMBDA_ARN));
        final GetFunctionConfigurationRequest request = new GetFunctionConfigurationRequest().withFunctionName("lambda-test").withQualifier("1.1.0");


        System.setProperty(DefaultCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY, vaultUrl);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(BAD_AUTH_RESPONSE_JSON));

        mockDecrypt(kmsClient, DECODED_AUTH_DATA);

        when(lambdaClient.getFunctionConfiguration(request)).thenReturn(new GetFunctionConfigurationResult().withRole(VALID_IAM_ARN));

        provider.getCredentials();
    }

    @Test(expected = IllegalStateException.class)
    public void authenticate_fails_when_lambda_has_invalid_assigned_role() throws Exception {
        final LambdaRoleVaultCredentialsProvider provider = new LambdaRoleVaultCredentialsProvider(urlResolver, VALID_LAMBDA_ARN);
        final GetFunctionConfigurationRequest request = new GetFunctionConfigurationRequest().withFunctionName("lambda-test").withQualifier("1.1.0");

        when(lambdaClient.getFunctionConfiguration(request)).thenReturn(new GetFunctionConfigurationResult().withRole(INVALID_ARN));

        provider.authenticate();
    }


    @Test(expected = IllegalStateException.class)
    public void authenticate_fails_when_lambda_has_no_assigned_role() throws Exception {
        final LambdaRoleVaultCredentialsProvider provider = new LambdaRoleVaultCredentialsProvider(urlResolver, VALID_LAMBDA_ARN);
        final GetFunctionConfigurationRequest request = new GetFunctionConfigurationRequest().withFunctionName("lambda-test").withQualifier("1.1.0");

        when(lambdaClient.getFunctionConfiguration(request)).thenReturn(new GetFunctionConfigurationResult().withRole(""));

        provider.authenticate();
    }

    @After
    public void resetMocks() {
        reset(lambdaClient, kmsClient);
    }


}