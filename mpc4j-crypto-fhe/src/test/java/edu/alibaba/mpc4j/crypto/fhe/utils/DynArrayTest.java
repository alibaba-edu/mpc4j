package edu.alibaba.mpc4j.crypto.fhe.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;

/**
 * Dynamic array unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/dynarray.cpp.
 * </p>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/4
 */
public class DynArrayTest {

    @Test
    public void testDynArrayBasic() {
        {
            DynArray arr = new DynArray();
            Assert.assertEquals(0, arr.size());
            Assert.assertEquals(0, arr.capacity());
            Assert.assertTrue(arr.empty());

            arr.resize(1);
            Assert.assertEquals(1, arr.size());
            Assert.assertEquals(1, arr.capacity());
            Assert.assertFalse(arr.empty());
            Assert.assertEquals(0, arr.at(0));
            arr.set(0, 1);
            Assert.assertEquals(1, arr.at(0));

            arr.reserve(6);
            Assert.assertEquals(1, arr.size());
            Assert.assertEquals(6, arr.capacity());
            Assert.assertFalse(arr.empty());
            Assert.assertEquals(1, arr.at(0));

            arr.resize(4);
            Assert.assertEquals(4, arr.size());
            Assert.assertEquals(6, arr.capacity());
            Assert.assertFalse(arr.empty());
            arr.set(0, 0);
            arr.set(1, 1);
            arr.set(2, 2);
            arr.set(3, 3);
            Assert.assertEquals(0, arr.at(0));
            Assert.assertEquals(1, arr.at(1));
            Assert.assertEquals(2, arr.at(2));
            Assert.assertEquals(3, arr.at(3));

            arr.shrinkToFit();
            Assert.assertEquals(4, arr.size());
            Assert.assertEquals(4, arr.capacity());
            Assert.assertFalse(arr.empty());
            Assert.assertEquals(0, arr.at(0));
            Assert.assertEquals(1, arr.at(1));
            Assert.assertEquals(2, arr.at(2));
            Assert.assertEquals(3, arr.at(3));
        }
    }

    @Test
    public void testSaveLoadDynArray() throws IOException {
        DynArray arr = new DynArray(6, 4);
        arr.set(0, 0);
        arr.set(1, 1);
        arr.set(2, 2);
        arr.set(3, 3);

        ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream();
        arr.saveMembers(outputStream1);
        outputStream1.close();
        ByteArrayInputStream inputStream1 = new ByteArrayInputStream(outputStream1.toByteArray());
        DynArray arr2 = new DynArray();
        arr2.loadMembers(null, inputStream1, null);
        // test equals()
        Assert.assertEquals(arr, arr2);
        Assert.assertEquals(arr.size(), arr2.size());
        Assert.assertEquals(arr.size(), arr2.capacity());
        Assert.assertEquals(arr.at(0), arr2.at(0));
        Assert.assertEquals(arr.at(1), arr2.at(1));
        Assert.assertEquals(arr.at(2), arr2.at(2));
        Assert.assertEquals(arr.at(3), arr2.at(3));

        arr.resize(2);
        arr.set(0, 5);
        arr.set(1, 6);

        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        arr.saveMembers(outputStream2);
        outputStream2.close();
        ByteArrayInputStream inputStream2 = new ByteArrayInputStream(outputStream2.toByteArray());
        // overwrite current arr2
        arr2.loadMembers(null, inputStream2, null);
        // test equals()
        Assert.assertEquals(arr, arr2);
        Assert.assertEquals(arr.size(), arr2.size());
        // overwrite does not change the capacity of arr2
        Assert.assertEquals(4, arr2.capacity());
        Assert.assertEquals(arr.at(0), arr2.at(0));
        Assert.assertEquals(arr.at(1), arr2.at(1));
    }
}
