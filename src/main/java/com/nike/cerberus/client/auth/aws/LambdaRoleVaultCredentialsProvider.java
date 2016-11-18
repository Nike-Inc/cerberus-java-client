/*
 * Copyright (c) 2016 Nike, Inc.
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
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationResult;
import com.google.gson.JsonSyntaxException;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.auth.VaultCredentialsProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link VaultCredentialsProvider} implementation that uses the assigned role
 * to lambda function to authenticate with Cerberus and decrypt the auth
 * response using KMS. If the assigned role has been granted the appropriate
 * provisioned for usage of Vault, it will succeed and have a token that can be
 * used to interact with Vault.
 */
public class LambdaRoleVaultCredentialsProvider extends BaseAwsCredentialsProvider {

    public static final Logger LOGGER = LoggerFactory.getLogger(LambdaRoleVaultCredentialsProvider.class);

    public static final Pattern LAMBDA_FUNCTION_ARN_PATTERN =
            Pattern.compile("arn:aws:lambda:(?<awsRegion>.*):(?<accountId>[0-9].*):(?<functionConstant>.*):(?<functionName>.*):(?<qualifier>.*)");

    public static final Pattern IAM_ROLE_ARN_PATTERN =
            Pattern.compile("arn:aws:iam::(?<accountId>\\d{12}):role/?(?<roleName>[a-zA-Z_0-9+=,.@\\-_/]+)");

    private final String functionName;

    private final String qualifier;

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     * @param invokedFunctionArn The invoked lambda function's ARN
     */
    public LambdaRoleVaultCredentialsProvider(final UrlResolver urlResolver, final String invokedFunctionArn) {
        super(urlResolver);
        final Matcher matcher = LAMBDA_FUNCTION_ARN_PATTERN.matcher(invokedFunctionArn);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("invokedFunctionArn not a properly formatted lambda function ARN.");
        }

        this.functionName = matcher.group("functionName");
        this.qualifier = matcher.group("qualifier");
    }

    /**
     * Looks up the assigned role for the running Lambda via the GetFunctionConfiguration API.  Requests a token from
     * Cerberus and attempts to decrypt it as that role.
     */
    @Override
    protected void authenticate() {
        final AWSLambda lambdaClient = new AWSLambdaClient();
        lambdaClient.setRegion(Regions.getCurrentRegion());

        final GetFunctionConfigurationResult functionConfiguration = lambdaClient.getFunctionConfiguration(
                new GetFunctionConfigurationRequest()
                        .withFunctionName(functionName)
                        .withQualifier(qualifier));

        final String roleArn = functionConfiguration.getRole();

        if (StringUtils.isBlank(roleArn)) {
            throw new IllegalStateException("Lambda function has no assigned role, aborting Cerberus authentication.");
        }

        final Matcher roleArnMatcher = IAM_ROLE_ARN_PATTERN.matcher(roleArn);

        if (!roleArnMatcher.matches()) {
            throw new IllegalStateException("Lambda function assigned role is not a valid IAM role ARN.");
        }

        final String accountId = roleArnMatcher.group("accountId");
        final String iamRoleArn = roleArnMatcher.group("roleName");

        try {
            getAndSetToken(accountId, iamRoleArn);
            return;
        } catch (AmazonClientException ace) {
            LOGGER.warn("Unexpected error communicating with AWS services.", ace);
        } catch (JsonSyntaxException jse) {
            LOGGER.error("The decrypted auth response was not in the expected format!", jse);
        } catch (VaultClientException sce) {
            LOGGER.warn("Unable to acquire Vault token for IAM role: " + iamRoleArn, sce);
        }

        throw new VaultClientException("Unable to acquire token with EC2 instance role.");
    }
}
