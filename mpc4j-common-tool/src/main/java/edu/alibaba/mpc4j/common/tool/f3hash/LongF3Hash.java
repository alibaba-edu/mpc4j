package edu.alibaba.mpc4j.common.tool.f3hash;

import edu.alibaba.mpc4j.common.tool.f3hash.F3HashFactory.F3HashType;
import edu.alibaba.mpc4j.common.tool.hash.LongHash;
import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory;
import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory.LongHashType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * long hash based f3 hash
 *
 * @author Feng Han
 * @date 2024/10/21
 */
public class LongF3Hash implements F3Hash {
    /**
     * 哈希函数类型
     */
    private final LongHash hash;

    LongF3Hash(LongHashType hashType) {
        hash = LongHashFactory.createInstance(hashType);
    }

    @Override
    public byte[] digestToBytes(byte[] message) {
        assert message.length > 0 : "Message length must be greater than 0: " + message.length;
        byte[] result = new byte[OUTPUT_F3_NUM];
        int count = 0;
        long hashRes = 0;
        while (count < OUTPUT_F3_NUM) {
            if (count % 39 == 0) {
                byte[] input = BytesUtils.clone(message);
                byte[] byteHash = LongUtils.longToByteArray(hashRes);
                for (int i = 0; i < Math.min(input.length, 8); i++) {
                    input[i] ^= byteHash[i];
                }
                hashRes = Math.abs(hash.hash(input));
            }
            // get each data
            result[count] = (byte) (hashRes % 3);
            hashRes = hashRes / 3;
            count++;
        }
        return result;
    }

    @Override
    public F3HashType getHashType() {
        return F3HashType.LONG_F3_HASH;
    }

}
