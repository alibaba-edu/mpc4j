package edu.alibaba.mpc4j.common.structure.fusefilter;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * test parameters for ByteFuseFilter.
 *
 * @author Weiran Liu
 * @date 2024/9/1
 */
@Ignore
public class ByteFuseFilterParamsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteFuseFilterParamsTest.class);
    /**
     * value length in byte
     */
    private static final int VALUE_BYTE_LENGTH = Long.BYTES;
    @Test
    public void testParams() {
        for (int logNum : new int[] {18, 20, 22}) {
            testParams(logNum);
        }
    }

    private void testParams(int logNum) {
        int num = 1 << logNum;
        Arity3ByteFuseInstance byteFuseInstance = new Arity3ByteFuseInstance(num, VALUE_BYTE_LENGTH);
        LOGGER.info("(n, m) = ({}, {})", num, byteFuseInstance.filterLength());
    }
}
