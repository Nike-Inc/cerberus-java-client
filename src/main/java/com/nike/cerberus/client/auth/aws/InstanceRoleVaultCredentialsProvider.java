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
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.gson.JsonSyntaxException;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.auth.TokenVaultCredentials;
import com.nike.vault.client.auth.VaultCredentialsProvider;
import com.nike.vault.client.model.VaultAuthResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


/**
 * {@link VaultCredentialsProvider} implementation that uses the assigned role
 * to an EC2 instance to authenticate with Cerberus and decrypt the auth
 * response using KMS. If the assigned role has been granted the appropriate
 * provisioned for usage of Vault, it will succeed and have a token that can be
 * used to interact with Vault.
 */
public class InstanceRoleVaultCredentialsProvider extends BaseAwsCredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceRoleVaultCredentialsProvider.class);

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

            final AWSKMSClient kmsClient = new AWSKMSClient();
            kmsClient.setRegion(Regions.getCurrentRegion());

            for (final String iamRole : iamRoleSet) {
                try {
                    final String encryptedAuthData = getEncryptedAuthData(accountId, iamRole);
                    final VaultAuthResponse decryptedToken = decryptToken(kmsClient, encryptedAuthData);
                    final DateTime expires = DateTime.now(DateTimeZone.UTC);
                    expires.plusSeconds(decryptedToken.getLeaseDuration() - paddingTimeInSeconds);

                    credentials = new TokenVaultCredentials(decryptedToken.getClientToken());
                    expireDateTime = expires;

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
}
