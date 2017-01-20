package com.nike.cerberus.client.auth.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.EC2MetadataUtils;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Credentials provider will look up the assigned InstanceProfileArn for this machine and attempt
 * To automatically retrieve a Vault token from CMS's iam-auth endpoint that takes region, acct id, role name.
 */
public class InstanceProfileVaultCredentialsProvider extends BaseAwsCredentialsProvider {

    /**
     * Constructor to setup credentials provider using the specified
     * implementation of {@link UrlResolver}
     *
     * @param urlResolver Resolver for resolving the Cerberus URL
     */
    public InstanceProfileVaultCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);
    }

    @Override
    protected void authenticate() {
        EC2MetadataUtils.IAMInfo iamInfo = getIamInfo();
        IamAuthInfo iamAuthInfo = getIamAuthInfo(iamInfo.instanceProfileArn);

        try {
            getAndSetToken(iamAuthInfo.accountId, iamAuthInfo.roleName, iamAuthInfo.region);
        } catch (Exception e) {
            throw new VaultClientException(String.format("Failed to authenticate with Cerberus's iam auth endpoint " +
                            "using the following auth info, acct id: %s, roleName: %s, region: %s",
                    iamAuthInfo.accountId, iamAuthInfo.roleName, iamAuthInfo.region), e);
        }
    }

    protected IamAuthInfo getIamAuthInfo(String instanceProfileArn) {
        if (instanceProfileArn == null) {
            throw new VaultClientException("instanceProfileArn provided was null rather than valid arn");
        }

        IamAuthInfo info = new IamAuthInfo();
        String pattern = "arn:aws:iam::(.*?):instance-profile/(.*)";
        Matcher matcher = Pattern.compile(pattern).matcher(instanceProfileArn);
        boolean found = matcher.find();
        if (! found) {
            throw new VaultClientException(String.format(
                    "Failed to find account id and role / instance profile name from ARN: %s using pattern %s",
                    instanceProfileArn, pattern));
        }

        info.accountId = matcher.group(1);
        info.roleName = matcher.group(2);

        return info;
    }

    protected static class IamAuthInfo {
        String accountId;
        String roleName;
        Region region = Regions.getCurrentRegion();
    }

    protected EC2MetadataUtils.IAMInfo getIamInfo() {
        return EC2MetadataUtils.getIAMInstanceProfileInfo();
    }
}
