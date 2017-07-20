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

import com.amazonaws.AmazonClientException;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.gson.JsonSyntaxException;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.auth.VaultCredentialsProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * {@link VaultCredentialsProvider} implementation that uses the assigned role
 * to an EC2 instance to authenticate with Cerberus and decrypt the auth
 * response using KMS. If the assigned role has been granted the appropriate
 * provisioned for usage of Vault, it will succeed and have a token that can be
 * used to interact with Vault.
 */
public class InstanceRoleVaultCredentialsProvider extends BaseAwsCredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceRoleVaultCredentialsProvider.class);

    public static final Pattern IAM_ARN_PATTERN = Pattern.compile("(arn\\:aws\\:iam\\:\\:)(?<accountId>[0-9].*)(\\:.*)");

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     */
    public InstanceRoleVaultCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);
    }

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     * @param xCerberusClientOverride - Overrides the default header value for the 'X-Cerberus-Client' header
     */
    public InstanceRoleVaultCredentialsProvider(UrlResolver urlResolver, String xCerberusClientOverride) {
        super(urlResolver, xCerberusClientOverride);
    }

    /**
     * Looks up the IAM roles assigned to the instance via the EC2 metadata
     * service. For each role assigned, an attempt is made to authenticate and
     * decrypt the Vault auth response with KMS. If successful, the token
     * retrieved is cached locally for future calls to
     * {@link BaseAwsCredentialsProvider#getCredentials()}.
     */
    @Override
    protected void authenticate() {
        try {
            final Set<String> iamRoleSet = EC2MetadataUtils.getIAMSecurityCredentials().keySet();
            final String accountId = lookupAccountId();

            for (final String iamRole : iamRoleSet) {
                try {
                    getAndSetToken(accountId, iamRole);
                    return;
                } catch (VaultClientException sce) {
                    LOGGER.warn("Unable to acquire Vault token for IAM role: " + iamRole, sce);
                }
            }
        } catch (AmazonClientException ace) {
            LOGGER.warn("Unexpected error communicating with AWS services.", ace);
        } catch (JsonSyntaxException jse) {
            LOGGER.error("The decrypted auth response was not in the expected format!", jse);
        }

        throw new VaultClientException("Unable to acquire token with EC2 instance role.");
    }

    /**
     * Parses and returns the AWS account ID from the instance profile ARN.
     *
     * @return AWS account ID
     */
    protected String lookupAccountId() {
        final EC2MetadataUtils.IAMInfo iamInfo = EC2MetadataUtils.getIAMInstanceProfileInfo();

        if (iamInfo == null) {
            final String errorMessage = "No IAM Instance Profile assigned to running instance.";
            LOGGER.error(errorMessage);
            throw new VaultClientException(errorMessage);
        }

        final Matcher matcher = IAM_ARN_PATTERN.matcher(iamInfo.instanceProfileArn);

        if (matcher.matches()) {
            final String accountId = matcher.group("accountId");
            if (StringUtils.isNotBlank(accountId)) {
                return accountId;
            }
        }

        throw new VaultClientException("Unable to obtain AWS account ID from instance profile ARN.");
    }
}
