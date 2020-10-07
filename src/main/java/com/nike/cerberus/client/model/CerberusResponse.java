/*
 * Copyright (c) 2018 Nike, Inc.
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

@Deprecated
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
