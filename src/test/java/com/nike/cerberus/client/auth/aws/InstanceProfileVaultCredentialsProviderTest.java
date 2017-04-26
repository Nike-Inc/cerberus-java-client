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
