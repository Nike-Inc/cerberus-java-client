package com.nike.cerberus.client.model.error;

import java.util.List;

import com.nike.cerberus.client.exception.CerberusApiError;

/**
 * POJO for representing error response body from Cerberus.
 */
public class ApiErrorResponse {

    private String errorId;
    private List<CerberusApiError> errors;

    public List<CerberusApiError> getErrors() {
        return errors;
    }

    public String getErrorId() {
        return errorId;
    }
	
}
