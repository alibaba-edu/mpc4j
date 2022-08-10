package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * Z2-SSP-VOLE协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public class Z2SspVoleReceiverOutput {
    /**
     * 数量
     */
    private int num;
    /**
     * 字节数量
     */
    private int byteNum;
    /**
     * 关联值Δ
     */
    private boolean delta;
    /**
     * q_i
     */
    private byte[] q;

    /**
     * 创建接收方输出。
     *
     * @param num   数量。
     * @param delta 关联值Δ。
     * @param q     q_i。
     * @return 接收方输出。
     */
    public static Z2SspVoleReceiverOutput create(int num, boolean delta, byte[] q) {
        Z2SspVoleReceiverOutput receiverOutput = new Z2SspVoleReceiverOutput();
        assert num > 0 : "# must be greater than 0";
        receiverOutput.num = num;
        receiverOutput.byteNum = CommonUtils.getByteLength(num);
        receiverOutput.delta = delta;
        assert q.length == receiverOutput.byteNum && BytesUtils.isReduceByteArray(q, num);
        receiverOutput.q = BytesUtils.clone(q);

        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private Z2SspVoleReceiverOutput() {
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
     * 返回关联值Δ字节数组。
     *
     * @return 关联值Δ字节数组。
     */
    public byte[] getDeltaBytes() {
        byte[] deltaBytes = new byte[byteNum];
        if (delta) {
            Arrays.fill(deltaBytes, (byte) 0xFF);
            BytesUtils.reduceByteArray(deltaBytes, num);
        }
        return deltaBytes;
    }

    /**
     * 返回q。
     *
     * @return q。
     */
    public byte[] getQ() {
        return q;
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
     * 返回字节数量。
     *
     * @return 字节数量。
     */
    public int getByteNum() {
        return byteNum;
    }
}
