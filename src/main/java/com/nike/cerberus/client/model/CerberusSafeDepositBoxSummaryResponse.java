package com.nike.cerberus.client.model;

/** Represents a summary for a specific safe deposit box. */
public class CerberusSafeDepositBoxSummaryResponse {

    private String id;

    private String name;

    private String path;

    private String categoryId;

    public String getId() {
        return id;
    }

    public CerberusSafeDepositBoxSummaryResponse setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public CerberusSafeDepositBoxSummaryResponse setName(String name) {
        this.name = name;
        return this;
    }

    public String getPath() {
        return path;
    }

    public CerberusSafeDepositBoxSummaryResponse setPath(String path) {
        this.path = path;
        return this;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public CerberusSafeDepositBoxSummaryResponse setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        return this;
    }
}
