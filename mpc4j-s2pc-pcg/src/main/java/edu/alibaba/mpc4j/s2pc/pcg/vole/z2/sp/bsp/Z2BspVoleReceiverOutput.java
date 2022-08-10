package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.Z2SspVoleReceiverOutput;

import java.util.Arrays;

/**
 * Z2-BSP-VOLE协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/6/21
 */
public class Z2BspVoleReceiverOutput {
    /**
     * Z2-SSP-VOLE协议接收方输出数组
     */
    private Z2SspVoleReceiverOutput[] receiverOutputs;
    /**
     * 关联值Δ
     */
    private boolean delta;
    /**
     * 每个Z2-SSP-VOLE协议接收方输出的数量
     */
    private int num;

    /**
     * 创建接收方输出。
     *
     * @param z2SspVoleReceiverOutputs Z2-SSP-VOLE协议接收方输出数组。
     * @return 接收方输出。
     */
    public static Z2BspVoleReceiverOutput create(Z2SspVoleReceiverOutput[] z2SspVoleReceiverOutputs) {
        Z2BspVoleReceiverOutput receiverOutput = new Z2BspVoleReceiverOutput();
        assert z2SspVoleReceiverOutputs.length > 0;
        // 取第一个输出的参数
        receiverOutput.delta = z2SspVoleReceiverOutputs[0].getDelta();
        receiverOutput.num = z2SspVoleReceiverOutputs[0].getNum();
        // 设置其余输出
        receiverOutput.receiverOutputs = Arrays.stream(z2SspVoleReceiverOutputs)
            // 验证所有Δ相等，且数量均为num
            .peek(z2SspVoleReceiverOutput -> {
                assert z2SspVoleReceiverOutput.getDelta() == receiverOutput.delta;
                assert z2SspVoleReceiverOutput.getNum() == receiverOutput.num;
            })
            .toArray(Z2SspVoleReceiverOutput[]::new);
        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private Z2BspVoleReceiverOutput() {
        // empty
    }

    /**
     * 返回关联值Δ。
     *
     * @return 关联值Δ。
     */
    public boolean getDelta() {
        return delta;
    }

    /**
     * 返回SSPCOT协议发送方输出。
     *
     * @param index 索引值。
     * @return 发送方输出。
     */
    public Z2SspVoleReceiverOutput get(int index) {
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
