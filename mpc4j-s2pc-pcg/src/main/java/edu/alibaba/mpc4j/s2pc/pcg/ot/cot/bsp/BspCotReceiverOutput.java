package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.util.Arrays;

/**
 * BSP-COT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class BspCotReceiverOutput {
    /**
     * SSPCOT协议接收方输出数组
     */
    private SspCotReceiverOutput[] receiverOutputs;
    /**
     * 每个SSPCOT协议输出的数量
     */
    private int num;

    public static BspCotReceiverOutput create(SspCotReceiverOutput[] sspCotReceiverOutputs) {
        BspCotReceiverOutput receiverOutput = new BspCotReceiverOutput();
        assert sspCotReceiverOutputs.length > 0;
        // 取第一个输出的参数
        receiverOutput.num = sspCotReceiverOutputs[0].getNum();
        // 设置其余输出
        receiverOutput.receiverOutputs = Arrays.stream(sspCotReceiverOutputs)
            // 验证数量均为num
            .peek(sspcotReceiverOutput -> {
                assert sspcotReceiverOutput.getNum() == receiverOutput.num;
            })
            .toArray(SspCotReceiverOutput[]::new);
        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private BspCotReceiverOutput() {
        // empty
    }

    /**
     * 返回SSPCOT协议接收方输出。
     *
     * @param index 索引值。
     * @return 接收方输出。
     */
    public SspCotReceiverOutput get(int index) {
        return receiverOutputs[index];
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return num;
    }

    /**
     * 返回批处理数量。
     *
     * @return 批处理数量。
     */
    public int getBatch() {
        return receiverOutputs.length;
    }
}
