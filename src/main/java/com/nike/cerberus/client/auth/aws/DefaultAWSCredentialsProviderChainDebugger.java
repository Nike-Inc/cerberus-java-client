package com.nike.cerberus.client.auth.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
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
    private final AWSCredentialsProvider[] credentialProviderChain = new AWSCredentialsProvider[]{
            new EnvironmentVariableCredentialsProvider(),
            new SystemPropertiesCredentialsProvider(),
            new ProfileCredentialsProvider(),
            new EC2ContainerCredentialsProviderWrapper()
    };

    /**
     * Log extra debugging information if appropriate
     * @param cerberusErrorMessage error message from Cerberus
     */
    public void logExtraDebuggingIfAppropriate(String cerberusErrorMessage) {
        if (StringUtils.contains(cerberusErrorMessage, TOKEN_IS_EXPIRED) || StringUtils.contains(cerberusErrorMessage, TOKEN_IS_INVALID)) {
            LOGGER.warn("Bad credentials may have been picked up from the DefaultAWSCredentialsProviderChain");
            boolean firstCredentialsFound = false;
            for (AWSCredentialsProvider provider : credentialProviderChain) {
                try {
                    AWSCredentials credentials = provider.getCredentials();
                    if (credentials.getAWSAccessKeyId() != null &&
                            credentials.getAWSSecretKey() != null) {
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
