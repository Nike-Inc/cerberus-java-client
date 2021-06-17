package com.nike.cerberus.client.auth.aws;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;

import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EnvironmentVariableCredentialsProvider.class, SystemPropertyCredentialsProvider.class, ProfileCredentialsProvider.class, ContainerCredentialsProvider.class})
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
