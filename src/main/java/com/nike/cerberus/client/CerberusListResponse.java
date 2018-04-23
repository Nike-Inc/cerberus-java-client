package com.nike.cerberus.client;

import java.util.LinkedList;
import java.util.List;

public class CerberusListResponse {
    private List<String> keys = new LinkedList<String>();

    public List<String> getKeys() {
        return keys;
    }

    public CerberusListResponse setKeys(List<String> keys) {
        this.keys = keys;
        return this;
    }
}
