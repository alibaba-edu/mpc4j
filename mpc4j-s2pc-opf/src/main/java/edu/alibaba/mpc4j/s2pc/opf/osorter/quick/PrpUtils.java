package edu.alibaba.mpc4j.s2pc.opf.osorter.quick;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.stream.IntStream;

/**
 * Utilities for prp operation
 *
 * @author Feng Han
 * @date 2024/01/24
 */
public class PrpUtils {
    public static final int PRP_PARALLEL_THRESHOLD = 256;

    /**
     * generating randomness
     *
     * @param prp     prp instance for generating randomness
     * @param index   current index
     * @param byteLen required byte size
     */
    public static byte[] generateRandBytes(Prp[] prp, long index, int byteLen) {
        int trueGroupNum = byteLen >> 4;
        byte[] res = new byte[byteLen];
        if (trueGroupNum < prp.length * PRP_PARALLEL_THRESHOLD) {
            // not parallel
            IntStream wIntStream = IntStream.range(0, trueGroupNum);
            byte[] data = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            wIntStream.forEach(i -> {
                long currentIndex = index + i;
                System.arraycopy(LongUtils.longToByteArray(currentIndex), 0, data, 0, 8);
                byte[] originByte = prp[0].prp(data);
                System.arraycopy(originByte, 0, res, i << 4, CommonConstants.BLOCK_BYTE_LENGTH);
            });
        } else {
            // parallel if there are many Prp instance
            int len = prp.length;
            int perMission = trueGroupNum / len;
            IntStream range = IntStream.range(0, len).parallel();
            range.forEach(i -> {
                Prp tmp = prp[i];
                byte[] data = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                IntStream.range(perMission * i, (i == len - 1) ? trueGroupNum : perMission * (i + 1)).forEach(j -> {
                    long currentIndex = index + j;
                    System.arraycopy(LongUtils.longToByteArray(currentIndex), 0, data, 0, 8);
                    byte[] originByte = tmp.prp(data);
                    System.arraycopy(originByte, 0, res, j << 4, CommonConstants.BLOCK_BYTE_LENGTH);
                });
            });
        }
        if((byteLen & 15) > 0){
            byte[] data = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            System.arraycopy(LongUtils.longToByteArray(index + trueGroupNum), 0, data, 0, 8);
            byte[] originByte = prp[0].prp(data);
            System.arraycopy(originByte, 0, res, trueGroupNum << 4, byteLen & 15);
        }
        return res;
    }
}
