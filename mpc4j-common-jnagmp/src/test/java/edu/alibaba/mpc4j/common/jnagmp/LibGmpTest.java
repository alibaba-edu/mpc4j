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

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link LibGmp}. Minor format modification.
 *
 * @author Square Inc.
 */
public class LibGmpTest {
    /**
     * Just adds "free()" for immediate de-allocation.
     */
    static class Memory extends com.sun.jna.Memory {
        public Memory(long size) {
            super(size);
        }

        public void free() {
            super.dispose();
        }
    }

    private static final Random RANDOM = new Random();

    final Memory scratch = new Memory(1024);
    final Memory count = new Memory(Native.SIZE_T_SIZE);
    final Memory mpzScratch = new Memory(LibGmp.mpz_t.SIZE * 3);

    @After
    public void tearDown() {
        scratch.free();
        mpzScratch.free();
        count.free();
    }

    @Test
    public void testVersion() {
        String libGmpVersion = LibGmp.GMP_VERSION;
        // All GMP version is 6.1.1 except for darwin-arrch64 (6.2.1)
        Assert.assertTrue(libGmpVersion.equals("6.1.1") || libGmpVersion.equals("6.2.1"));
    }

    @Test
    public void testInitClear() {
        LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch);
        LibGmp.__gmpz_init(mvalue);
        LibGmp.__gmpz_clear(mvalue);
    }

    @Test
    public void testInit2Clear() {
        for (int size = 0; size < 1000; ++size) {
            LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch);
            LibGmp.__gmpz_init2(mvalue, new NativeLong(8 * size));
            LibGmp.__gmpz_clear(mvalue);
        }
    }

    /**
     * Without any leading zero, import/export should match exactly.
     */
    @Test
    public void testImportExportNoLeadingZero() {
        for (int size = 0; size < 1000; ++size) {
            LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch);
            LibGmp.__gmpz_init2(mvalue, new NativeLong(8 * size));
            try {
                byte[] in = new byte[size];
                RANDOM.nextBytes(in);
                if (size > 0 && in[0] == 0) {
                    in[0] = 1;
                }

                scratch.write(0, in, 0, size);
                LibGmp.__gmpz_import(mvalue, in.length, 1, 1, 1, 0, scratch);

                scratch.clear();
                LibGmp.__gmpz_export(scratch, count, 1, 1, 1, 0, mvalue);

                Assert.assertEquals(size, LibGmp.readSizeT(count));
                byte[] out = new byte[size];
                scratch.read(0, out, 0, size);

                assertArrayEquals(in, out);
            } finally {
                LibGmp.__gmpz_clear(mvalue);
            }
        }
    }

    /**
     * GMP will strip off any leading zeroes with import/export.
     */
    @Test
    public void testImportExportLeadingZeroes() {
        for (int size = 0; size < 1000; ++size) {
            LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch);
            LibGmp.__gmpz_init2(mvalue, new NativeLong(8 * size));
            try {
                byte[] in = new byte[size];
                RANDOM.nextBytes(in);

                int leadingZeros = size / 4;
                for (int i = 0; i < leadingZeros; ++i) {
                    in[i] = 0;
                }
                if (leadingZeros < size && in[leadingZeros] == 0) {
                    in[leadingZeros] = 1;
                }

                scratch.write(0, in, 0, size);
                LibGmp.__gmpz_import(mvalue, in.length, 1, 1, 1, 0, scratch);

                scratch.clear();
                LibGmp.__gmpz_export(scratch, count, 1, 1, 1, 0, mvalue);

                Assert.assertEquals(size - leadingZeros, LibGmp.readSizeT(count));
                byte[] out = new byte[size];
                scratch.read(0, out, leadingZeros, size - leadingZeros);

                assertArrayEquals(in, out);
            } finally {
                LibGmp.__gmpz_clear(mvalue);
            }
        }
    }

    @Test
    public void testMod() {
        scratch.write(0, new byte[]{8, 3}, 0, 2);
        LibGmp.mpz_t[] mvalues = new LibGmp.mpz_t[2];
        try {
            for (int i = 0; i < 2; ++i) {
                LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch.share(i * LibGmp.mpz_t.SIZE, LibGmp.mpz_t.SIZE));
                LibGmp.__gmpz_init(mvalue);
                mvalues[i] = mvalue;
                LibGmp.__gmpz_import(mvalue, 1, 1, 1, 1, 0, scratch.share(i));
            }

            // We can reuse the modulus for the result, result must be < modulus.
            LibGmp.mpz_t result = mvalues[1];
            LibGmp.__gmpz_mod(result, mvalues[0], mvalues[1]);

            LibGmp.__gmpz_export(scratch, count, 1, 1, 1, 0, result);

            Assert.assertEquals(1, LibGmp.readSizeT(count));
            assertEquals(2, scratch.getByte(0));
        } finally {
            for (LibGmp.mpz_t mvalue : mvalues) {
                if (mvalue != null) {
                    LibGmp.__gmpz_clear(mvalue);
                }
            }
        }
    }

    @Test
    public void testModPow() {
        scratch.write(0, new byte[]{2, 3, 5}, 0, 3);
        LibGmp.mpz_t[] mvalues = new LibGmp.mpz_t[3];
        try {
            for (int i = 0; i < 3; ++i) {
                LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch.share(i * LibGmp.mpz_t.SIZE, LibGmp.mpz_t.SIZE));
                LibGmp.__gmpz_init(mvalue);
                mvalues[i] = mvalue;
                LibGmp.__gmpz_import(mvalue, 1, 1, 1, 1, 0, scratch.share(i));
            }

            // We can reuse the modulus for the result, result must be < modulus.
            LibGmp.mpz_t result = mvalues[2];
            LibGmp.__gmpz_powm(result, mvalues[0], mvalues[1], mvalues[2]);

            LibGmp.__gmpz_export(scratch, count, 1, 1, 1, 0, result);

            Assert.assertEquals(1, LibGmp.readSizeT(count));
            assertEquals(3, scratch.getByte(0));
        } finally {
            for (LibGmp.mpz_t mvalue : mvalues) {
                if (mvalue != null) {
                    LibGmp.__gmpz_clear(mvalue);
                }
            }
        }
    }

    @Test
    public void testModPowSec() {
        scratch.write(0, new byte[]{2, 3, 5}, 0, 3);
        LibGmp.mpz_t[] mvalues = new LibGmp.mpz_t[3];
        try {
            for (int i = 0; i < 3; ++i) {
                LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch.share(i * LibGmp.mpz_t.SIZE, LibGmp.mpz_t.SIZE));
                LibGmp.__gmpz_init(mvalue);
                mvalues[i] = mvalue;
                LibGmp.__gmpz_import(mvalue, 1, 1, 1, 1, 0, scratch.share(i));
            }

            // We can reuse the modulus for the result, result must be < modulus.
            LibGmp.mpz_t result = mvalues[2];
            LibGmp.__gmpz_powm_sec(result, mvalues[0], mvalues[1], mvalues[2]);

            LibGmp.__gmpz_export(scratch, count, 1, 1, 1, 0, result);

            Assert.assertEquals(1, LibGmp.readSizeT(count));
            assertEquals(3, scratch.getByte(0));
        } finally {
            for (LibGmp.mpz_t mvalue : mvalues) {
                if (mvalue != null) {
                    LibGmp.__gmpz_clear(mvalue);
                }
            }
        }
    }

    @Test
    public void testModInverse() {
        scratch.write(0, new byte[]{3, 5}, 0, 2);
        LibGmp.mpz_t[] mvalues = new LibGmp.mpz_t[2];
        try {
            for (int i = 0; i < 2; ++i) {
                LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch.share(i * LibGmp.mpz_t.SIZE, LibGmp.mpz_t.SIZE));
                LibGmp.__gmpz_init(mvalue);
                mvalues[i] = mvalue;
                LibGmp.__gmpz_import(mvalue, 1, 1, 1, 1, 0, scratch.share(i));
            }

            // We can reuse the modulus for the result, result must be < modulus.
            LibGmp.mpz_t result = mvalues[1];
            LibGmp.__gmpz_invert(result, mvalues[0], mvalues[1]);

            LibGmp.__gmpz_export(scratch, count, 1, 1, 1, 0, result);

            Assert.assertEquals(1, LibGmp.readSizeT(count));
            assertEquals(2, scratch.getByte(0));
        } finally {
            for (LibGmp.mpz_t mvalue : mvalues) {
                if (mvalue != null) {
                    LibGmp.__gmpz_clear(mvalue);
                }
            }
        }
    }

    @Test
    public void testMul() {
        scratch.write(0, new byte[]{2, 3}, 0, 2);
        LibGmp.mpz_t[] mvalues = new LibGmp.mpz_t[2];
        try {
            for (int i = 0; i < 2; ++i) {
                LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch.share(i * LibGmp.mpz_t.SIZE, LibGmp.mpz_t.SIZE));
                LibGmp.__gmpz_init(mvalue);
                mvalues[i] = mvalue;
                LibGmp.__gmpz_import(mvalue, 1, 1, 1, 1, 0, scratch.share(i));
            }

            // We can reuse the modulus for the result, result must be < modulus.
            LibGmp.mpz_t result = mvalues[1];
            LibGmp.__gmpz_mul(result, mvalues[0], mvalues[1]);

            LibGmp.__gmpz_export(scratch, count, 1, 1, 1, 0, result);

            Assert.assertEquals(1, LibGmp.readSizeT(count));
            assertEquals(6, scratch.getByte(0));
        } finally {
            for (LibGmp.mpz_t mvalue : mvalues) {
                if (mvalue != null) {
                    LibGmp.__gmpz_clear(mvalue);
                }
            }
        }
    }

    @Test
    public void testNegSign() {
        scratch.write(0, new byte[]{1, 0, 1}, 0, 3);
        LibGmp.mpz_t[] mvalues = new LibGmp.mpz_t[3];
        try {
            for (int i = 0; i < 3; ++i) {
                LibGmp.mpz_t mvalue = new LibGmp.mpz_t(mpzScratch.share(i * LibGmp.mpz_t.SIZE, LibGmp.mpz_t.SIZE));
                LibGmp.__gmpz_init(mvalue);
                mvalues[i] = mvalue;
                LibGmp.__gmpz_import(mvalue, 1, 1, 1, 1, 0, scratch.share(i));
            }

            // The first value should be -1
            LibGmp.__gmpz_neg(mvalues[0], mvalues[0]);

            // Check absolute values.
            for (int i = 0; i < 3; ++i) {
                LibGmp.mpz_t result = mvalues[i];
                LibGmp.__gmpz_export(scratch, count, 1, 1, 1, 0, result);

                if (i == 1) {
                    // The middle value is 0
                    Assert.assertEquals(0, LibGmp.readSizeT(count));
                } else {
                    // The other two are 1 (absolute value is exported)
                    Assert.assertEquals(1, LibGmp.readSizeT(count));
                    assertEquals(1, scratch.getByte(0));
                }
            }

            // Now comparisons
            Assert.assertEquals(1, LibGmp.__gmpz_cmp_si(mvalues[0], new NativeLong(-2))); // -1 > -2
            Assert.assertEquals(0, LibGmp.__gmpz_cmp_si(mvalues[0], new NativeLong(-1))); // -1 == -1
            Assert.assertEquals(-1, LibGmp.__gmpz_cmp_si(mvalues[0], new NativeLong(0))); // -1 < 0

            Assert.assertEquals(1, LibGmp.__gmpz_cmp_si(mvalues[1], new NativeLong(-1))); // 0 > -1
            Assert.assertEquals(0, LibGmp.__gmpz_cmp_si(mvalues[1], new NativeLong(0))); // 0 == 0
            Assert.assertEquals(-1, LibGmp.__gmpz_cmp_si(mvalues[1], new NativeLong(1))); // 0 < 1

            Assert.assertEquals(1, LibGmp.__gmpz_cmp_si(mvalues[2], new NativeLong(0))); // 1 > 0
            Assert.assertEquals(0, LibGmp.__gmpz_cmp_si(mvalues[2], new NativeLong(1))); // 1 == 1
            Assert.assertEquals(-1, LibGmp.__gmpz_cmp_si(mvalues[2], new NativeLong(2))); // 1 < 2
        } finally {
            for (LibGmp.mpz_t mvalue : mvalues) {
                if (mvalue != null) {
                    LibGmp.__gmpz_clear(mvalue);
                }
            }
        }
    }
}
