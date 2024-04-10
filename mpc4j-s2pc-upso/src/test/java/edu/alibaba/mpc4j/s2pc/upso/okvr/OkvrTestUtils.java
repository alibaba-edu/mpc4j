package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.math3.util.Pair;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * unbalanced batched OPPRF test utilities.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public class OkvrTestUtils {
    /**
     * input byte length
     */
    private static final int INPUT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * private constructor.
     */
    private OkvrTestUtils() {
        // empty
    }

    static Pair<Map<ByteBuffer, byte[]>, Set<ByteBuffer>> generateInputs(int num, int l, int retrievalSize, boolean equalValue,
                                                                         byte[][] simpleHashKeys, SecureRandom secureRandom) {
        assert simpleHashKeys.length == 1;
        int byteL = CommonUtils.getByteLength(l);
        // use simple hash to place int into batched queries.
        SimpleIntHashBin simpleIntHashBin = new SimpleIntHashBin(EnvType.STANDARD, retrievalSize, num, simpleHashKeys);
        simpleIntHashBin.insertItems(IntStream.range(0, num).toArray());
        Map<ByteBuffer, byte[]> keyValueMap = new HashMap<>(num);
        Set<ByteBuffer> keys = new HashSet<>(retrievalSize);
        for (int index = 0; index < retrievalSize; index++) {
            byte[] equalTarget = BytesUtils.randomByteArray(byteL, l, secureRandom);
            int batchNum = simpleIntHashBin.binSize(index);
            // generate sender inputs
            ByteBuffer[] batchKeys = new ByteBuffer[batchNum];
            byte[][] batchValues = new byte[batchNum][];
            for (int pointIndex = 0; pointIndex < batchNum; pointIndex++) {
                byte[] key = new byte[INPUT_BYTE_LENGTH];
                secureRandom.nextBytes(key);
                batchKeys[pointIndex] = ByteBuffer.wrap(key);
                batchValues[pointIndex] = equalValue ? BytesUtils.clone(equalTarget)
                    : BytesUtils.randomByteArray(byteL, l, secureRandom);
                keyValueMap.put(batchKeys[pointIndex], batchValues[pointIndex]);
            }
            // generate receiver inputs
            if (batchNum > 0) {
                // batch num is not zero
                if (index % 2 == 0) {
                    // randomly select a point to be the input
                    int pointIndex = secureRandom.nextInt(batchNum);
                    keys.add(ByteBuffer.wrap(BytesUtils.clone(batchKeys[pointIndex].array())));
                } else {
                    // randomly generate a input
                    keys.add(ByteBuffer.wrap(BytesUtils.randomByteArray(byteL, l, secureRandom)));
                }
            } else {
                // batch point num is zero, create a random input
                keys.add(ByteBuffer.wrap(BytesUtils.randomByteArray(byteL, l, secureRandom)));
            }
        }
        return new Pair<>(keyValueMap, keys);
    }
}
