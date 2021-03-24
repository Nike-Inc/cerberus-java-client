
package com.nike.cerberus.client.auth.aws;

import com.nike.cerberus.client.CerberusServerException;
import com.nike.cerberus.client.auth.CerberusCredentials;
import com.nike.cerberus.client.auth.TokenCerberusCredentials;
import okhttp3.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
public class BaseAwsCredentialsProviderTest {

    private TokenCerberusCredentials credentials;
    private  OkHttpClient httpClient;
    private Call call;


    @Before
    public void setup(){
        credentials = Mockito.mock(TokenCerberusCredentials.class);
        httpClient = Mockito.mock(OkHttpClient.class);
        call = Mockito.mock(Call.class);
    }

    @Test
    public  void test_getCredentials(){
        BaseAwsCredentialsProvider baseAwsCredentialsProvider = new BaseAwsCredentialsProvider("http://testurl") {
            @Override
            protected void authenticate() {
                this.credentials = new TokenCerberusCredentials("token-value");
            }
        };
        CerberusCredentials credentials = baseAwsCredentialsProvider.getCredentials();
        Assert.assertNotNull(credentials);
        assertThat(credentials.getToken()).isEqualTo("token-value");
    }
    @Test
    public  void test_getCredentials_not_empty(){
        Mockito.when(credentials.getToken()).thenReturn("test-token");
        BaseAwsCredentialsProvider baseAwsCredentialsProvider = new BaseAwsCredentialsProvider("http://testurl","test-value") {
            @Override
            protected void authenticate() {

            }
        };
        baseAwsCredentialsProvider.credentials = credentials;

        CerberusCredentials credentials = baseAwsCredentialsProvider.getCredentials();
        Assert.assertNotNull(credentials);
        assertThat(credentials.getToken()).isEqualTo("test-token");
    }

    @Test
    public  void test_executeRequestWithRetry() throws  Exception{
        Mockito.when(httpClient.newCall(Mockito.any())).thenReturn(call);
        Request request = new Request.Builder().url("http://testurl").build();

        Response response = new Response.Builder().request(request).protocol(Protocol.HTTP_1_0).code(200).message("response").build();
        Mockito.when(call.execute()).thenReturn(response);
        BaseAwsCredentialsProvider baseAwsCredentialsProvider = new BaseAwsCredentialsProvider("http://testurl",httpClient) {
            @Override
            protected void authenticate() {

            }
        };
        response =  baseAwsCredentialsProvider.executeRequestWithRetry(null, 1,1);
        Assert.assertNotNull(response);
    }

    @Test(expected = IOException.class)
    public  void test_executeRequestWithRetry_exception() throws  Exception{
        Mockito.when(httpClient.newCall(Mockito.any())).thenReturn(call);
        Request request = new Request.Builder().url("http://testurl").build();

        Response response = new Response.Builder().request(request).protocol(Protocol.HTTP_1_0).code(200).message("response").build();
        Mockito.doThrow(new IOException()).when(call).execute();
        BaseAwsCredentialsProvider baseAwsCredentialsProvider = new BaseAwsCredentialsProvider("http://testurl",httpClient) {
            @Override
            protected void authenticate() {

            }
        };
        baseAwsCredentialsProvider.executeRequestWithRetry(request, 1,1);

    }
    @Test(expected = CerberusServerException.class)
    public  void test_parseAndThrowErrorResponse() throws  Exception{
        Mockito.when(httpClient.newCall(Mockito.any())).thenReturn(call);
        Request request = new Request.Builder().url("http://testurl").build();

        Response response = new Response.Builder().request(request).protocol(Protocol.HTTP_1_0).code(200).message("response").build();
        //Mockito.doThrow(new IOException()).when(call).execute();
        BaseAwsCredentialsProvider baseAwsCredentialsProvider = new BaseAwsCredentialsProvider("http://testurl",httpClient) {
            @Override
            protected void authenticate() {

            }
        };
        baseAwsCredentialsProvider.parseAndThrowErrorResponse(200,"response");
    }
}

