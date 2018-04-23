package com.nike.cerberus.client;

import java.util.Map;

public class CerberusResponse {
    private Map<String, String> data;

    /**
     * Returns the key/value pairs stored at a path
     *
     * @return Map of data
     */
    public Map<String, String> getData() {
        return data;
    }

    public CerberusResponse setData(Map<String, String> data) {
        this.data = data;
        return this;
    }
}
