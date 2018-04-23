package com.nike.cerberus.client.auth;

public class TokenCerberusCredentials implements CerberusCredentials {
    private final String token;

    /**
     * Explicit constructor that sets the token.
     *
     * @param token Token to represent
     */
    public TokenCerberusCredentials(final String token) {
        this.token = token;
    }

    /**
     * Returns the token set during construction.
     *
     * @return Token
     */
    @Override
    public String getToken() {
        return token;
    }
}
