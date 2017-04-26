package com.nike.cerberus.client.auth.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.UrlResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for allowing users to explicitly set the account id, rolename and region that they want to authenticate as.
 */
public class StaticIamRoleVaultCredentialsProvider extends BaseAwsCredentialsProvider {

    public static final String IAM_ROLE_ARN_FORMAT = "arn:aws:iam::%s:role/%s";
    protected String iamPrincipalArn;
    protected Region region;

    public StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver, String accountId, String roleName, String region) {
        this(urlResolver);
        this.iamPrincipalArn = generateIamRoleArn(accountId, roleName);
        this.region = Region.getRegion(Regions.fromName(region));
    }

    public StaticIamRoleVaultCredentialsProvider(String vaultUrl, String accountId, String roleName, String region) {
        this(new StaticVaultUrlResolver(vaultUrl));
        this.iamPrincipalArn = generateIamRoleArn(accountId, roleName);
        this.region = Region.getRegion(Regions.fromName(region));
    }

    public StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver, String accountId, String roleName, Region region) {
        this(urlResolver);
        this.iamPrincipalArn = generateIamRoleArn(accountId, roleName);
        this.region = region;
    }

    public StaticIamRoleVaultCredentialsProvider(String vaultUrl, String accountId, String roleName, Region region) {
        this(new StaticVaultUrlResolver(vaultUrl));
        this.iamPrincipalArn = generateIamRoleArn(accountId, roleName);
        this.region = region;
    }

    public StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver, String iamRoleArn, String region) {
        this(urlResolver);
        this.iamPrincipalArn = iamRoleArn;
        this.region = Region.getRegion(Regions.fromName(region));
    }


    public StaticIamRoleVaultCredentialsProvider(String vaultUrl, String iamRoleArn, String region) {
        this(new StaticVaultUrlResolver(vaultUrl));
        this.iamPrincipalArn = iamRoleArn;
        this.region = Region.getRegion(Regions.fromName(region));
    }


    public StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver, String iamRoleArn, Region region) {
        this(urlResolver);
        this.iamPrincipalArn = iamRoleArn;
        this.region = region;
    }

    public StaticIamRoleVaultCredentialsProvider(String vaultUrl, String iamRoleArn, Region region) {
        this(new StaticVaultUrlResolver(vaultUrl));
        this.iamPrincipalArn = iamRoleArn;
        this.region = region;
    }

    private StaticIamRoleVaultCredentialsProvider(UrlResolver urlResolver) {
        super(urlResolver);
    }

    private String generateIamRoleArn(String accountId, String roleName) {

        return String.format(IAM_ROLE_ARN_FORMAT, accountId, roleName);
    }

    @Override
    protected void authenticate() {
        getAndSetToken(iamPrincipalArn, region);
    }
}
