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
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.util.EC2MetadataUtils;
import com.nike.cerberus.client.CerberusClientException;
import com.nike.cerberus.client.DefaultCerberusUrlResolver;
import com.nike.cerberus.client.UrlResolver;
import com.nike.cerberus.client.auth.CerberusCredentials;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.nike.cerberus.client.auth.aws.InstanceRoleCerberusCredentialsProvider.buildIamRoleArns;
import static com.nike.cerberus.client.auth.aws.InstanceRoleCerberusCredentialsProvider.buildRoleArn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Tests the InstanceRoleCerberusCredentialsProvider class
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AWSKMSClient.class,
    EC2MetadataUtils.class, InstanceRoleCerberusCredentialsProvider.class})
@PowerMockIgnore({"javax.management.*","javax.net.*"})
public class InstanceRoleCerberusCredentialsProviderTest extends BaseCredentialsProviderTest {

    private static final String GOOD_INSTANCE_PROFILE_ARN = "arn:aws:iam::107274433934:instance-profile/rawr";

    private static final String DEFAULT_ROLE = "role";

    private UrlResolver urlResolver;

    private AWSKMSClient kmsClient;

    private InstanceRoleCerberusCredentialsProvider provider;

    @Before
    public void setup() throws Exception {
        kmsClient = mock(AWSKMSClient.class);
        urlResolver = mock(UrlResolver.class);
        provider = new InstanceRoleCerberusCredentialsProvider(urlResolver);

        whenNew(AWSKMSClient.class).withAnyArguments().thenReturn(kmsClient);
        mockStatic(EC2MetadataUtils.class);
        mockGetCurrentRegion();
    }

    @Test
    public void getCredentials_returns_valid_credentials() throws IOException {

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        final String vaultUrl = "http://localhost:" + mockWebServer.getPort();

        mockGetIamSecurityCredentials(DEFAULT_ROLE);
        mockGetIamInstanceProfileInfo(GOOD_INSTANCE_PROFILE_ARN);
        mockDecrypt(kmsClient, DECODED_AUTH_DATA);
        when(urlResolver.resolve()).thenReturn(vaultUrl);

        System.setProperty(DefaultCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY, vaultUrl);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(AUTH_RESPONSE));

        CerberusCredentials credentials = provider.getCredentials();
        assertThat(credentials.getToken()).isEqualTo(AUTH_TOKEN);

    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_throws_client_exception_when_accountId_missing() {
        mockGetIamSecurityCredentials(DEFAULT_ROLE);
        mockGetIamInstanceProfileInfo("arn:aws:iam:instance-profile/rawr");

        provider.getCredentials();
    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_throws_client_exception_when_no_roles_are_set() {
        when(EC2MetadataUtils.getIAMSecurityCredentials())
        .thenReturn(Collections.<String, EC2MetadataUtils.IAMSecurityCredential>emptyMap());
        mockGetIamInstanceProfileInfo(GOOD_INSTANCE_PROFILE_ARN);

        provider.getCredentials();
    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_throws_client_exception_when_not_running_on_ec2_instance() {
        when(EC2MetadataUtils.getIAMSecurityCredentials()).thenThrow(new AmazonClientException("BAD"));

        provider.getCredentials();
    }

    @Test(expected = CerberusClientException.class)
    public void getCredentials_thorws_client_exception_when_no_instance_profile_assigned() {
        when(EC2MetadataUtils.getIAMInstanceProfileInfo()).thenReturn(null);

        provider.getCredentials();
    }

    private void mockGetCurrentRegion() {
        when(EC2MetadataUtils.getEC2InstanceRegion()).thenReturn(Regions.US_WEST_2.getName());
    }

    private void mockGetIamSecurityCredentials(final String role) {
        Map<String, EC2MetadataUtils.IAMSecurityCredential> map = new HashMap<>();
        map.put(role, null);
        when(EC2MetadataUtils.getIAMSecurityCredentials()).thenReturn(map);
    }

    private void mockGetIamInstanceProfileInfo(final String instanceProfileArn) {
        EC2MetadataUtils.IAMInfo iamInfo = new EC2MetadataUtils.IAMInfo();
        iamInfo.instanceProfileArn = instanceProfileArn;
        when(EC2MetadataUtils.getIAMInstanceProfileInfo()).thenReturn(iamInfo);
    }

    @Test
    public void test_buildIamRoleArns_with_path() {
        String instanceProfileArn = "arn:aws:iam::1234567890123:instance-profile/brewmaster/foo/brewmaster-foo-cerberus";
        Set<String> roles = Sets.newSet("brewmaster-foo-cerberus");

        Set<String> results = buildIamRoleArns(instanceProfileArn, roles);
        assertEquals(2, results.size());
        Iterator<String> iterator = results.iterator();
        assertEquals("arn:aws:iam::1234567890123:role/brewmaster/foo/brewmaster-foo-cerberus", iterator.next());
        assertEquals("arn:aws:iam::1234567890123:role/brewmaster-foo-cerberus", iterator.next());
    }

    @Test
    public void test_buildIamRoleArns_with_CloudFormation_style_names() {
        String instanceProfileArn = "arn:aws:iam::1234567890123:instance-profile/foo-cerberus-LKSJDFIWERWER";
        Set<String> roles = Sets.newSet("foo-cerberus-SDFLKJRWE234");

        Set<String> results = buildIamRoleArns(instanceProfileArn, roles);
        assertEquals(1, results.size());
        String result = results.iterator().next();

        assertEquals("arn:aws:iam::1234567890123:role/foo-cerberus-SDFLKJRWE234", result);
    }

    @Test
    public void test_buildIamRoleArns_CloudFormation_style_names_with_paths() {
        String instanceProfileArn = "arn:aws:iam::1234567890123:instance-profile/brewmaster/foo/foo-cerberus-LKSJDFIWERWER";
        Set<String> roles = Sets.newSet("foo-cerberus-SDFLKJRWE234");

        Set<String> results = buildIamRoleArns(instanceProfileArn, roles);
        assertEquals(2, results.size());
    }

    @Test
    public void test_parseInstanceProfileArn_with_path() {
        String instanceProfileArn = "arn:aws:iam::1234567890123:instance-profile/brewmaster/foo/brewmaster-foo-cerberus";
        InstanceRoleCerberusCredentialsProvider.InstanceProfileInfo info = InstanceRoleCerberusCredentialsProvider.parseInstanceProfileArn(instanceProfileArn);
        assertEquals("1234567890123", info.accountId);
        assertEquals("brewmaster/foo/brewmaster-foo-cerberus", info.profileName);
    }

    @Test
    public void test_parseInstanceProfileArn_without_path() {
        String instanceProfileArn = "arn:aws:iam::1234567890123:instance-profile/foo-cerberus";
        InstanceRoleCerberusCredentialsProvider.InstanceProfileInfo info = InstanceRoleCerberusCredentialsProvider.parseInstanceProfileArn(instanceProfileArn);
        assertEquals("1234567890123", info.accountId);
        assertEquals("foo-cerberus", info.profileName);
    }

    @Test
    public void test_parsePathFromInstanceProfileName() {
        String instanceProfileName = "brewmaster/foo/brewmaster-foo-cerberus";
        String path = InstanceRoleCerberusCredentialsProvider.parsePathFromInstanceProfileName(instanceProfileName);
        assertEquals("brewmaster/foo", path);
    }

    @Test
    public void test_buildRoleArn_without_path() {
        String accountId = "1234567890123";
        assertEquals("arn:aws:iam::1234567890123:role/foo", buildRoleArn(accountId, "", "foo"));
    }

    @Test
    public void test_buildRoleArn_with_null_path() {
        String accountId = "1234567890123";
        assertEquals("arn:aws:iam::1234567890123:role/foo", buildRoleArn(accountId, null, "foo"));
    }

    @Test
    public void test_buildRoleArn_with_path() {
        String accountId = "1234567890123";
        assertEquals("arn:aws:iam::1234567890123:role/bar/foo", buildRoleArn(accountId, "bar", "foo"));
    }

    @Test
    public void test_buildRoleArn_with_longer_path() {
        String accountId = "1234567890123";
        assertEquals("arn:aws:iam::1234567890123:role/bar/more/foo", buildRoleArn(accountId, "bar/more", "foo"));
    }
}