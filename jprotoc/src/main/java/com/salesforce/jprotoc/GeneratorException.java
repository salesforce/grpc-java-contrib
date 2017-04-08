/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.jprotoc;

/**
 * This exception represents a structural problem with output generation.
 */
public class GeneratorException extends RuntimeException {
    public GeneratorException(String message) {
        super(message);
    }
}
