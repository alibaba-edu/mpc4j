package edu.alibaba.mpc4j.s3pc.abb3.basic.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.concurrent.ForkJoinPool;
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

    /**
     * generate the random permutation with the given key,
     * note that this function return the same permutation for various party as long as the envType are the same
     *
     * @param key      prp keys
     * @param len      required rank of permutation
     * @param parallel generate in parallel
     * @param envType  environment type
     */
    public static int[] genCorRandomPerm(byte[] key, int len, boolean parallel, EnvType envType) {
        int parallelNum = parallel ? ForkJoinPool.getCommonPoolParallelism() : 1;
        Prp[] prp = IntStream.range(0, parallelNum).mapToObj(j -> {
            Prp tmp = PrpFactory.createInstance(envType);
            tmp.setKey(key);
            return tmp;
        }).toArray(Prp[]::new);
        int[] randInt = IntUtils.byteArrayToIntArray(generateRandBytes(prp, 0, len << 2));
        return ShuffleUtils.permutationGeneration(randInt);
    }

}
