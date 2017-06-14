/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import java.util.Iterator;

public class Sequence implements Iterable<Integer> {
    private final int max;

    public Sequence(int max) {
        this.max = max;
    }

    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            int i = 0;

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return i < max;
            }

            public Integer next() {
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                return i++;
            }
        };
    }
}
