package com.nike.cerberus.client.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Simple Util for determining environment metadata
 */
public class EnvironmentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUtils.class);

    /**
     * This endpoint is available on EC2 instances
     */
    private static final String INSTANCE_IDENTITY_DOCUMENT = "http://169.254.169.254/latest/dynamic/instance-identity/document";

    /**
     * This endpoint is available on ECS Containers
     */
    private static final String ECS_METADATA_ENDPOINT = "http://localhost:51678/v1/metadata";

    private EnvironmentUtils() {
    }

    /**
     * True if the current system is running in a pure EC2 environment, and not ECS.
     */
    public static boolean isRunningInEc2() {
        return hasInstanceIdentity() && ! isRunningInEcs();
    }

    /**
     * True if the http://169.254.169.254/latest/dynamic/instance-identity/document endpoint
     * is available and returns a 2xx status code.  True means we are currently running on
     * an Ec2 instance.  This check should work both on Linux and Windows.
     */
    public static boolean hasInstanceIdentity() {
        return canGetSuccessfully(INSTANCE_IDENTITY_DOCUMENT);
    }

    /**
     *
     * @return true if this is a container in ECS
     */
    public static boolean isRunningInEcs() {
        return canGetSuccessfully(ECS_METADATA_ENDPOINT);
    }

    protected static boolean canGetSuccessfully(String url) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(500, MILLISECONDS)
                .readTimeout(500, MILLISECONDS)
                .writeTimeout(500, MILLISECONDS)
                .build();

        Request request = new Request.Builder().get().url(url).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.debug("Error when trying to GET {}", url, e);
        }
        return false;
    }

}
