package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * DPPRF协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class DpprfReceiverOutput {
    /**
     * α上界
     */
    private final int alphaBound;
    /**
     * α比特长度
     */
    private final int h;
    /**
     * 批处理数量组PRF输出，每组输出都有α上界个元素，第α个元素为null
     */
    private final byte[][][] pprfOutputArrays;
    /**
     * α数组
     */
    private final int[] alphaArray;
    /**
     * 批处理数量
     */
    private final int batchNum;

    public DpprfReceiverOutput(int alphaBound, int[] alphaArray, byte[][][] pprfOutputArrays) {
        // 批处理数量设置
        batchNum = alphaArray.length;
        assert batchNum > 0 : "Batch Num must be greater than 0: " + batchNum;
        assert pprfOutputArrays.length == batchNum : "# of pprfKeys must be equal to " + batchNum + ": " + pprfOutputArrays.length;
        // α设置
        assert alphaBound > 0 : "AlphaBound must be greater than 0: " + alphaBound;
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> {
                assert alpha >= 0 && alpha < alphaBound : "α must be in range [0," + alphaBound + "): " + alpha;
            })
            .toArray();
        this.pprfOutputArrays = IntStream.range(0, batchNum)
                .mapToObj(batchIndex -> {
                    byte[][] pprfKey = pprfOutputArrays[batchIndex];
                    // 批处理数量个PRF密钥，每个密钥都有α上界个元素
                    assert pprfKey.length == alphaBound : "PrfKey length should be " + alphaBound + ": " + pprfKey.length;
                    // 第α个密钥为空
                    assert pprfKey[alphaArray[batchIndex]] == null;
                    return pprfKey;
                })
            .toArray(byte[][][]::new);
    }

    /**
     * 返回α。
     *
     * @param batchIndex 批处理索引。
     * @return α。
     */
    public int getAlpha(int batchIndex) {
        return alphaArray[batchIndex];
    }

    /**
     * 返回α比特长度。
     *
     * @return α比特长度。
     */
    public int getH() {
        return h;
    }

    /**
     * 返回α上界。
     *
     * @return α上界。
     */
    public int getAlphaBound() {
        return alphaBound;
    }

    /**
     * 返回批处理数量。
     *
     * @return 批处理数量。
     */
    public int getBatchNum() {
        return batchNum;
    }

    /**
     * 返回PPRF输出。
     *
     * @param batchIndex 批处理索引。
     * @return PPRF输出。
     */
    public byte[][] getPprfOutputArray(int batchIndex) {
        return pprfOutputArrays[batchIndex];
    }
}
