package com.nike.cerberus.client.auth.aws;


import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sometimes Bad AWS Credentials get picked up from the provider chain
 * and people aren't sure where they came from.
 */
public class DefaultAWSCredentialsProviderChainDebugger {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAWSCredentialsProviderChainDebugger.class);
    private static final String TOKEN_IS_EXPIRED = "The security token included in the request is expired.";
    private static final String TOKEN_IS_INVALID = "Invalid credentials";

    /**
     * This chain should match that found in DefaultAWSCredentialsProviderChain
     */
    private final AwsCredentialsProvider[] credentialProviderChain = new AwsCredentialsProvider[]{
             EnvironmentVariableCredentialsProvider.create(),
            SystemPropertyCredentialsProvider.create(),
            ProfileCredentialsProvider.create(),
            ContainerCredentialsProvider.builder().build()
    };

    /**
     * Log extra debugging information if appropriate
     * @param cerberusErrorMessage error message from Cerberus
     */
    public void logExtraDebuggingIfAppropriate(String cerberusErrorMessage) {
        if (StringUtils.contains(cerberusErrorMessage, TOKEN_IS_EXPIRED) || StringUtils.contains(cerberusErrorMessage, TOKEN_IS_INVALID)) {
            LOGGER.warn("Bad credentials may have been picked up from the DefaultAWSCredentialsProviderChain");
            boolean firstCredentialsFound = false;
            for (AwsCredentialsProvider provider : credentialProviderChain) {
                try {
                    AwsCredentials credentials = provider.resolveCredentials();
                    if (credentials.accessKeyId() != null &&
                            credentials.secretAccessKey() != null) {
                        if (!firstCredentialsFound) {
                            firstCredentialsFound = true;
                            LOGGER.info("AWS Credentials were loaded from " + provider.toString());
                        } else {
                            LOGGER.info("AWS Credentials were also available from " + provider.toString() + " but those were not used");
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.info("Unable to load credentials from " + provider.toString());
                }
            }
        }
    }
}
