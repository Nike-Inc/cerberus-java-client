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

package com.nike.cerberus.client.model;

import java.util.Map;
import java.util.Set;

/**
 * Represents an authentication response from Cerberus
 */
public class CerberusAuthResponse {

    private String clientToken;

    private Set<String> policies;

    private Map<String, String> metadata;

    private int leaseDuration;

    private boolean renewable;

    public String getClientToken() {
        return clientToken;
    }

    public CerberusAuthResponse setClientToken(String clientToken) {
        this.clientToken = clientToken;
        return this;
    }

    public Set<String> getPolicies() {
        return policies;
    }

    public CerberusAuthResponse setPolicies(Set<String> policies) {
        this.policies = policies;
        return this;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public CerberusAuthResponse setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public int getLeaseDuration() {
        return leaseDuration;
    }

    public CerberusAuthResponse setLeaseDuration(int leaseDuration) {
        this.leaseDuration = leaseDuration;
        return this;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public CerberusAuthResponse setRenewable(boolean renewable) {
        this.renewable = renewable;
        return this;
    }
}
