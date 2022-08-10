package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.Z2SspVoleSenderOutput;

import java.util.Arrays;

/**
 * Z2-BSP-VOLE协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/6/21
 */
public class Z2BspVoleSenderOutput {
    /**
     * Z2-SSP-VOLE协议发送方输出数组
     */
    private Z2SspVoleSenderOutput[] senderOutputs;
    /**
     * 每个Z2-SSP-VOLE协议发送方输出的数量
     */
    private int num;

    /**
     * 创建发送方输出。
     *
     * @param z2SspVoleSenderOutputs Z2-SSP-VOLE协议发送方输出数组。
     * @return 发送方输出。
     */
    public static Z2BspVoleSenderOutput create(Z2SspVoleSenderOutput[] z2SspVoleSenderOutputs) {
        Z2BspVoleSenderOutput senderOutput = new Z2BspVoleSenderOutput();
        assert z2SspVoleSenderOutputs.length > 0;
        // 取第一个输出的参数
        senderOutput.num = z2SspVoleSenderOutputs[0].getNum();
        // 设置其余输出
        senderOutput.senderOutputs = Arrays.stream(z2SspVoleSenderOutputs)
            // 验证数量均为num
            .peek(z2SspVoleSenderOutput -> {
                assert z2SspVoleSenderOutput.getNum() == senderOutput.num;
            })
            .toArray(Z2SspVoleSenderOutput[]::new);
        return senderOutput;
    }

    /**
     * 私有构造函数。
     */
    private Z2BspVoleSenderOutput() {
        // empty
    }

    /**
     * 返回Z2-SSP-VOLE协议发送方输出。
     *
     * @param index 索引值。
     * @return 发送方输出。
     */
    public Z2SspVoleSenderOutput get(int index) {
        return senderOutputs[index];
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
        return senderOutputs.length;
    }
}
