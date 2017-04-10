/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.jprotoc;

/**
 * This exception represents a structural problem with output generation. Error messages will be printed to the
 * console output.
 */
public class GeneratorException extends RuntimeException {
    public GeneratorException(String message) {
        super(message);
    }
}
