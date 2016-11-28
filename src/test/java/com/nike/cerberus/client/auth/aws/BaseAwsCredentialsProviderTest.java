package com.nike.cerberus.client.auth.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kms.AWSKMSClient;
import com.nike.cerberus.client.DefaultCerberusUrlResolver;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.VaultServerException;
import com.nike.vault.client.auth.VaultCredentials;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class BaseAwsCredentialsProviderTest extends BaseCredentialsProviderTest{
    public static final Region REGION = RegionUtils.getRegion("us-west-2");
    public static final String ACCOUNT_ID = "123456789012";
    public static final String CERBERUS_TEST_ROLE = "cerberus-test-role";
    public static final String ERROR_RESPONSE = "Error calling vault";

    protected static final String MISSING_AUTH_DATA = "{}";


    private BaseAwsCredentialsProvider provider;
    private UrlResolver urlResolver;
    private String vaultUrl;
    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        urlResolver = mock(UrlResolver.class);

        provider = new TestAwsCredentialsProvider(urlResolver);

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        vaultUrl = "http://localhost:" + mockWebServer.getPort();
    }

    @After
    public void tearDown() throws Exception {
        reset(urlResolver);
    }

    @Test(expected = VaultClientException.class)
    public void getEncryptedAuthData_blank_url_throws_exception() throws Exception {
        when(urlResolver.resolve()).thenReturn("");

        provider.getEncryptedAuthData(ACCOUNT_ID, CERBERUS_TEST_ROLE, REGION);
    }

    @Test(expected = VaultClientException.class)
    public void decryptToken_throws_exception_when_non_encrypted_data_provided() {
        provider.decryptToken(mock(AWSKMSClient.class), "non-encrypted-token");
    }

    @Test(expected = VaultServerException.class)
    public void getEncryptedAuthData_throws_exception_on_bad_response_code() throws IOException {
        when(urlResolver.resolve()).thenReturn(vaultUrl);

        System.setProperty(DefaultCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY, vaultUrl);
        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody(ERROR_RESPONSE));

        provider.getEncryptedAuthData(ACCOUNT_ID, CERBERUS_TEST_ROLE, REGION);
    }

    @Test(expected = VaultClientException.class)
    public void getEncryptedAuthData_throws_exception_on_missing_auth_data() throws IOException {
        when(urlResolver.resolve()).thenReturn(vaultUrl);

        System.setProperty(DefaultCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY, vaultUrl);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(MISSING_AUTH_DATA));

        provider.getEncryptedAuthData(ACCOUNT_ID, CERBERUS_TEST_ROLE, REGION);
    }

    class TestAwsCredentialsProvider extends BaseAwsCredentialsProvider {
        /**
         * Constructor to setup credentials provider using the specified
         * implementation of {@link UrlResolver}
         *
         * @param urlResolver Resolver for resolving the Cerberus URL
         */
        public TestAwsCredentialsProvider(UrlResolver urlResolver) {
            super(urlResolver);
        }

        @Override
        protected void authenticate() {

        }
    }

}