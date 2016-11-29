package com.nike.cerberus.client.auth.aws;

import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;

import java.nio.ByteBuffer;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

public class BaseCredentialsProviderTest {
    protected static final String AUTH_RESPONSE = "{\"auth_data\":\"eyJjbGllbnRfdG9rZW4iOiI2NjMyY2I1Zi1mMTBjLTQ1NzItOTU0NS1lNTJmNDdmNmEzZmQiLCAibGVhc2VfZHVyYXRpb24iOiIzNjAwIn0=\"}";
    protected static final String BAD_AUTH_RESPONSE_JSON = "{,\"auth_data\":\"eyJjbGllbnRfdG9rZW4iOiI2NjMyY2I1Zi1mMTBjLTQ1NzItOTU0NS1lNTJmNDdmNmEzZmQiLCAibGVhc2VfZHVyYXRpb24iOiIzNjAwIn0=\"}";
    protected static final String DECODED_AUTH_DATA = "{\"client_token\":\"6632cb5f-f10c-4572-9545-e52f47f6a3fd\", \"lease_duration\":\"3600\"}";
    protected static final String AUTH_TOKEN = "6632cb5f-f10c-4572-9545-e52f47f6a3fd";

    protected void mockDecrypt(AWSKMSClient kmsClient, final String toDecrypt) {
        DecryptResult decryptResult = new DecryptResult();
        decryptResult.setPlaintext(ByteBuffer.wrap(toDecrypt.getBytes()));
        when(kmsClient.decrypt(any(DecryptRequest.class))).thenReturn(decryptResult);
    }
}
