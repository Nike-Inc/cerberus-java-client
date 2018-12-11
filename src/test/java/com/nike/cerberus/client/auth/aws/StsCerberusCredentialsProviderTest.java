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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.nike.cerberus.client.UrlResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class StsCerberusCredentialsProviderTest {

    private static final String ACCOUNT_ID = "1234";
    private static final String ROLE_NAME = "foo/base/bar";
    private static final String ROLE_ARN = String.format("arn:aws:iam::%s:role/%s", ACCOUNT_ID, ROLE_NAME);
    private static final String REGION_STRING = "us-east-1";
//    private static final Region REGION = Region.getRegion(Regions.US_EAST_1);

    private static final String cerberusUrl = "https://dev.cerberus.nikecloud.com";

    private static final String amazonUrl = "https://sts.us-east-1.amazonaws.com";

    public static final Region REGION = RegionUtils.getRegion("us-east-1");
    public static final String CERBERUS_TEST_ARN = "arn:aws:iam::123456789012:role/cerberus-test-role";


    private StsCerberusCredentialsProvider credentialsProvider;
    private UrlResolver urlResolver;

    @Before
    public void setUp() throws Exception {
        urlResolver = mock(UrlResolver.class);

//        urlResolver = () -> "https://dev.cerberus.nikecloud.com";

        credentialsProvider = new StsCerberusCredentialsProvider(urlResolver);
    }


    @Test
    public void test_get_caller_identity(){
        GetCallerIdentityResult callerIdentityResult = credentialsProvider.getCallerIdentity();
        assertThat(callerIdentityResult).isNotNull();
    }

    @Test
    public void get_aws_credentials(){
        AWSCredentials credentials = credentialsProvider.getAWSCredentials();
        assertThat(credentials).isNotNull();
    }


    @Test
    public void get_signed_headers(){
        when(urlResolver.resolve()).thenReturn(cerberusUrl);
        Map<String, String> headers = credentialsProvider.getSignedHeaders();
        assertThat(headers).isNotNull();
    }


    @Test
    public void get_token(){
        when(urlResolver.resolve()).thenReturn(cerberusUrl);
        String token = credentialsProvider.buildRequest(CERBERUS_TEST_ARN, REGION);
        assertThat(token).isNotNull();
    }

}

