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

package com.nike.cerberus.client.http;

/**
 * Constants for HTTP status codes interpreted by the Cerberus client.
 */
public final class HttpStatus {

    public static final int OK = 200;

    public static final int CREATED = 201;

    public static final int ACCEPTED = 202;

    public static final int NO_CONTENT = 204;

    public static final int BAD_REQUEST = 400;

    public static final int UNAUTHORIZED = 401;

    public static final int FORBIDDEN = 403;

    public static final int NOT_FOUND = 404;

    public static final int CONFLICT = 409;

    public static final int TOO_MANY_REQUESTS = 429;

    public static final int INTERNAL_SERVER_ERROR = 500;

    public static final int BAD_GATEWAY = 502;

    public static final int SERVICE_UNAVAILABLE = 503;

    public static final int GATEWAY_TIMEOUT = 504;
}
