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

import com.amazonaws.regions.Region;
import com.amazonaws.util.EC2MetadataUtils;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.VaultClientException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class InstanceProfileVaultCredentialsProviderTest {

    InstanceProfileVaultCredentialsProvider provider;

    @Before
    public void before() {
        provider = new InstanceProfileVaultCredentialsProvider(new StaticVaultUrlResolver("foo"));
    }

    @Test(expected = VaultClientException.class)
    public void test_that_authenticate_catches_exceptions_and_throws_vault_exception() {
        InstanceProfileVaultCredentialsProvider providerSpy = spy(provider);

        doThrow(new RuntimeException("Foo")).when(providerSpy).getAndSetToken(anyString(), any(Region.class));

        EC2MetadataUtils.IAMInfo iamInfo = new EC2MetadataUtils.IAMInfo();
        iamInfo.instanceProfileArn = "foo";
        doReturn(iamInfo).when(providerSpy).getIamInfo();

        providerSpy.authenticate();
    }
}
