package edu.alibaba.mpc4j.dp.service.fo.hadamard;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Hadamard Response (HR) Frequency Oracle LDP server. This is the optimized HR mechanism implementation. See paper:
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
public class HrHighEpsFoLdpServer extends AbstractFoLdpServer {
    /**
     * the number of blocks, we create 2^t blocks.
     */
    private final int blockNum;
    /**
     * the block size, each block has size 2^k.
     */
    private final int blockSize;
    /**
     * the output size
     */
    private final int outputSize;
    /**
     * output size byte length
     */
    private final int outputSizeByteLength;
    /**
     * e^ε
     */
    private final double expEpsilon;
    /**
     * the budgets
     */
    private final int[] budgets;

    public HrHighEpsFoLdpServer(FoLdpConfig config) {
        super(config);
        expEpsilon = Math.exp(epsilon);
        // e^ε > 1, d > 1
        int blockNumBitLength = (int)Math.floor(DoubleUtils.log2(Math.min(2 * d, Math.max(expEpsilon, 2))));
        blockNum = 1 << blockNumBitLength;
        int blockSizeBitLength = (int)Math.ceil(DoubleUtils.log2((double) d / blockNum + 1));
        blockSize = 1 << blockSizeBitLength;
        int outputBitLength = blockNumBitLength + blockSizeBitLength;
        outputSize = 1 << outputBitLength;
        outputSizeByteLength = IntUtils.boundedNonNegIntByteLength(outputSize);
        budgets = new int[outputSize];
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, outputSizeByteLength
        );
        int y = IntUtils.byteArrayToBoundedNonNegInt(itemBytes, outputSize);
        MathPreconditions.checkNonNegativeInRange("y", y, outputSize);
        budgets[y]++;
        num++;
    }

    @Override
    public Map<String, Double> estimate() {
        int[] counts = new int[outputSize];
        for (int blockIndex = 0; blockIndex < blockNum; blockIndex++) {
            int[] blockBudget = new int[blockSize];
            System.arraycopy(budgets, blockIndex * blockSize, blockBudget, 0, blockSize);
            int[] blockCounts = HadamardCoder.fastWalshHadamardTrans(blockBudget);
            System.arraycopy(blockCounts, 0, counts, blockIndex * blockSize, blockSize);
        }
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> {
                    // map to (blockIndex, y)
                    int blockIndex = itemIndex / (blockSize - 1);
                    int x = itemIndex % (blockSize - 1) + 1;
                    // map to C(x)
                    int cx = counts[blockIndex * blockSize + x];
                    // p(x)
                    return cx * (expEpsilon + 2 * blockNum - 2) / (expEpsilon - 1);
                }
            ));
    }
}
