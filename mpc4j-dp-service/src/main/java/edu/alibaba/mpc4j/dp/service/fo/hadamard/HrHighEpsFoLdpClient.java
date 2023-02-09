package edu.alibaba.mpc4j.dp.service.fo.hadamard;

import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Random;

/**
 * Hadamard Response (HR) Frequency Oracle LDP client. This is the optimized HR mechanism implementation. See paper:
 * <p>
 * Cormode, Graham, Samuel Maddock, and Carsten Maple. "Frequency estimation under local differential privacy.
 * VLDB 2021, no. 11, pp. 2046-2058.
 * </p>
 * The description is as follows:
 * <p>
 * To improve the bound when e^ε is largewe can sample t Hadamard coefficients to produce a hash function with g = 2^t
 * possible outcomes. This preserves the result with probability p^* = e^ε / (e^ε + 2^t - 1), and otherwise perturbs it
 * uniformly.
 * </p>
 * The implementation is to partition the input index x into 2^t blocks, each block contains n = 2^k elements. Given an
 * input x, we respond uniformly random y ∈ with probability q^* = (2^t - 1) / (e^ε + 2^t - 1). Otherwise, we compute
 * the block index as x / 2^t, and the Hadamard matrix position x % 2^t, and respond a correct index.
 * <p>
 * Although the optimized HR mechanism is more suitable for high ε, we do not restrict ε to be very high.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/1/19
 */
public class HrHighEpsFoLdpClient extends AbstractFoLdpClient {
    /**
     * the number of blocks, we create 2^t blocks.
     */
    private final int blockNum;
    /**
     * the bit length of block size, each block has size 2^k.
     */
    private final int blockSizeBitLength;
    /**
     * the block size, each block has size 2^k.
     */
    private final int blockSize;
    /**
     * the output size
     */
    private final int outputSize;
    /**
     * e^ε
     */
    private final double expEpsilon;

    public HrHighEpsFoLdpClient(FoLdpConfig config) {
        super(config);
        expEpsilon = Math.exp(epsilon);
        // e^ε > 1, d > 1
        int blockNumBitLength = (int)Math.floor(DoubleUtils.log2(Math.min(2 * d, Math.max(expEpsilon, 2))));
        blockNum = 1 << blockNumBitLength;
        blockSizeBitLength = (int)Math.ceil(DoubleUtils.log2((double) d / blockNum + 1));
        blockSize = 1 << blockSizeBitLength;
        int outputBitLength = blockNumBitLength + blockSizeBitLength;
        outputSize = 1 << outputBitLength;
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        int itemIndex = domain.getItemIndex(item);
        int blockIndex = itemIndex / (blockSize - 1);
        // we do not use the position 0
        int x = itemIndex % (blockSize - 1) + 1;
        // get a random number y1 as a potential output
        int y1Encode = random.nextInt(outputSize);
        double u = random.nextDouble();
        // return uniformly random result, including choosing random block, and choosing random y.
        if (u < 2 * blockNum / (expEpsilon + 2 * blockNum - 1)) {
            return IntUtils.boundedNonNegIntToByteArray(y1Encode, outputSize);
        } else {
            // remove all information related to the block index
            int y1 = y1Encode & (blockSize - 1);
            // map y1 to the same block as inputEncode while maintain the location within the block
            y1Encode = (blockIndex << blockSizeBitLength) + y1;
            boolean y1Check = HadamardCoder.checkParity(x, y1);
            if (y1Check) {
                // if H[x][y1] = 1, output y1
                return IntUtils.boundedNonNegIntToByteArray(y1Encode, outputSize);
            } else {
                // construct y2 = blockIndex || y2 so that
                // if H[x][y1] = 0, then H[x][y2] = 1, and if H[x][y1] = 1, then H[x][y2] = 0
                int y2 = -1;
                for (int i = 0; i < blockSizeBitLength; i++) {
                    if (IntUtils.getLittleEndianBoolean(x, i)) {
                        y2 = y1 ^ (1 << i);
                        break;
                    }
                }
                assert y2 >= 0 && y2 < blockSize : "y2 must be in range [0, " + blockSize + ")";
                int y2Encode = (blockIndex << blockSizeBitLength) + y2;
                return IntUtils.boundedNonNegIntToByteArray(y2Encode, outputSize);
            }
        }
    }
}
