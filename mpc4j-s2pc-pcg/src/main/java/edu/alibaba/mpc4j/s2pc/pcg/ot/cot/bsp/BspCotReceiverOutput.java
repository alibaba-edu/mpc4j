package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.Arrays;

/**
 * BSP-COT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class BspCotReceiverOutput implements PcgPartyOutput {
    /**
     * SSPCOT receiver output array
     */
    private SspCotReceiverOutput[] receiverOutputs;
    /**
     * num for each SSPCOT receiver output
     */
    private int eachNum;

    public static BspCotReceiverOutput create(SspCotReceiverOutput[] sspCotReceiverOutputs) {
        BspCotReceiverOutput receiverOutput = new BspCotReceiverOutput();
        assert sspCotReceiverOutputs.length > 0;
        // 取第一个输出的参数
        receiverOutput.eachNum = sspCotReceiverOutputs[0].getNum();
        // 设置其余输出
        receiverOutput.receiverOutputs = Arrays.stream(sspCotReceiverOutputs)
            // 验证数量均为num
            .peek(sspcotReceiverOutput -> {
                assert sspcotReceiverOutput.getNum() == receiverOutput.eachNum;
            })
            .toArray(SspCotReceiverOutput[]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private BspCotReceiverOutput() {
        // empty
    }

    /**
     * Gets a single single-point COT.
     *
     * @param index the index.
     * @return a single single-point COT.
     */
    public SspCotReceiverOutput get(int index) {
        return receiverOutputs[index];
    }

    /**
     * Gets num for each single single-point COT.
     *
     * @return num for each single single-point COT.
     */
    public int getEachNum() {
        return eachNum;
    }

    @Override
    public int getNum() {
        return receiverOutputs.length;
    }
}
