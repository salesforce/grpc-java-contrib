/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
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
