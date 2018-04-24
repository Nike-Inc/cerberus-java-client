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

package com.nike.cerberus.client;

import org.apache.commons.lang3.StringUtils;

/**
 * Wrapper for the URL resolver interface for a static URL.
 */
public class StaticCerberusUrlResolver implements UrlResolver {

    private final String cerberusUrl;

    /**
     * Explicit constructor for holding a static Cerberus URL.
     *
     * @param cerberusUrl Cerberus URL
     */
    public StaticCerberusUrlResolver(final String cerberusUrl) {
        if (StringUtils.isBlank(cerberusUrl)) {
            throw new IllegalArgumentException("Cerberus URL can not be blank.");
        }

        this.cerberusUrl = cerberusUrl;
    }

    /**
     * Returns a static Cerberus URL.
     *
     * @return Cerberus URL
     */
    @Override
    public String resolve() {
        return cerberusUrl;
    }
}
