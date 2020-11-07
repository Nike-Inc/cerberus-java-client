package com.nike.cerberus.client.model;

public enum CerberusRolePermission {
    OWNER,
    WRITE,
    READ;

    public static CerberusRolePermission fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        return valueOf(value.toUpperCase());
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
