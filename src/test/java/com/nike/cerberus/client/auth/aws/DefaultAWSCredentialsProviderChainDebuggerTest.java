package com.nike.cerberus.client.auth.aws;

import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.nike.cerberus.client.auth.EnvironmentCerberusCredentialsProvider;
import com.nike.cerberus.client.auth.SystemPropertyCerberusCredentialsProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EnvironmentVariableCredentialsProvider.class, SystemPropertiesCredentialsProvider.class, ProfileCredentialsProvider.class, EC2ContainerCredentialsProviderWrapper.class})
public class DefaultAWSCredentialsProviderChainDebuggerTest {

    @Test
    public void test_logExtraDebuggingIfAppropriate(){
        mockStatic(System.class);
        when(System.getenv("AWS_ACCESS_KEY_ID")).thenReturn("TOKEN");
        when(System.getenv("AWS_SECRET_KEY")).thenReturn("secretKey");
        when(System.getenv("aws.accessKeyId")).thenReturn("accesskey");
        when(System.getenv("aws.secretKey")).thenReturn("aws_secretKey");
        DefaultAWSCredentialsProviderChainDebugger defaultAWSCredentialsProviderChainDebugger = new DefaultAWSCredentialsProviderChainDebugger();
        defaultAWSCredentialsProviderChainDebugger.logExtraDebuggingIfAppropriate("The security token included in the request is expired.");
    }
}
