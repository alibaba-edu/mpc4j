/*
 * Original Work Copyright 2013 Square Inc.
 * Modified by Weiran Liu. Adjust the code based on Alibaba Java Code Guidelines.
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
package edu.alibaba.mpc4j.common.jnagmp;

import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link GmpBigInteger}.
 *
 * @author Square Inc.
 */
public class GmpBigIntegerTest implements AutoCloseable {

    @Test
    public void testNegatives() {
        assertEquals(BigInteger.valueOf(-10), new GmpBigInteger(BigInteger.valueOf(-10)));
    }

    @Test
    public void testConstructors() {
        assertEquals(BigInteger.TEN, new GmpBigInteger(BigInteger.TEN));
        assertEquals(BigInteger.TEN, new GmpBigInteger(new byte[]{10}));
        assertEquals(BigInteger.TEN, new GmpBigInteger(1, new byte[]{10}));
        assertEquals(BigInteger.TEN, new GmpBigInteger("A", 16));
        assertEquals(BigInteger.TEN, new GmpBigInteger("10"));

        assertEquals(BigInteger.TEN, new GmpBigInteger(8, new Random() {
            @Override
            public void nextBytes(byte[] bytes) {
                assertEquals(1, bytes.length);
                bytes[0] = 10;
            }
        }));

        final AtomicBoolean firstTime = new AtomicBoolean(true);
        assertEquals(BigInteger.valueOf(13), new GmpBigInteger(4, 12, new Random() {
            @Override
            public int nextInt() {
                if (firstTime.compareAndSet(true, false)) {
                    return 13;
                } else {
                    return super.nextInt();
                }
            }
        }));
    }

    @Override
    public void close() throws Exception {
        // Force GC to verify {@code GmpInteger.mpzMemory} cleans up properly without crashing.
        final AtomicBoolean gcHappened = new AtomicBoolean(false);
        gcHappened.set(true);
        while (!gcHappened.get()) {
            System.gc();
            //noinspection BusyWait
            Thread.sleep(100);
        }
    }
}
