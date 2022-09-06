package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;

/**
 * DPPRF协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class DpprfSenderOutput {
    /**
     * α上界
     */
    private final int alphaBound;
    /**
     * α比特长度
     */
    private final int h;
    /**
     * 批处理数量组PRF输出，每组输出都有α上界个元素
     */
    private final byte[][][] prfOutputArrays;
    /**
     * 批处理数量
     */
    private final int batchNum;

    public DpprfSenderOutput(int alphaBound, byte[][][] prfOutputArrays) {
        // 批处理数量设置
        batchNum = prfOutputArrays.length;
        assert batchNum > 0 : "Batch Num must be greater than 0: " + batchNum;
        // α设置
        assert alphaBound > 0 : "AlphaBound must be greater than 0: " + alphaBound;
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound);
        this.prfOutputArrays = Arrays.stream(prfOutputArrays)
            .peek(prfKey -> {
                // 每一个PRF密钥都有α上界个
                assert prfKey.length == alphaBound : "PrfKey length should be " + alphaBound + ": " + prfKey.length;
            })
            .toArray(byte[][][]::new);
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
     * 返回PRF输出。
     *
     * @param batchIndex 批处理索引。
     * @return PRF输出。
     */
    public byte[][] getPrfOutputArray(int batchIndex) {
        return prfOutputArrays[batchIndex];
    }
}
