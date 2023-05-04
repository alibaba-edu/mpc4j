package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.SspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.AbstractMspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiverOutput;

/**
 * BCG19-REG-MSP-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/25
 */
public class Bcg19RegMspCotReceiver extends AbstractMspCotReceiver {
    /**
     * BSP-COT协议接收方
     */
    private final BspCotReceiver bspCotReceiver;
    /**
     * 预计算接收方输出
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * BSP-COT协议接收方输出
     */
    private BspCotReceiverOutput bspCotReceiverOutput;

    public Bcg19RegMspCotReceiver(Rpc senderRpc, Party receiverParty, Bcg19RegMspCotConfig config) {
        super(Bcg19RegMspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bspCotReceiver = BspCotFactory.createReceiver(senderRpc, receiverParty, config.getBspCotConfig());
        addSubPtos(bspCotReceiver);
    }

    @Override
    public void init(int maxT, int maxNum) throws MpcAbortException {
        setInitInput(maxT, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 原本应该设置为maxN / maxT，但此值不与T成正比，最后考虑直接设置为maxN
        bspCotReceiver.init(maxT, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MspCotReceiverOutput receive(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return receive();
    }

    @Override
    public MspCotReceiverOutput receive(int t, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(t, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private MspCotReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 生成稀疏数组
        int[] innerTargetArray = IntStream.range(0, t)
            .map(i -> {
                // 需要转换为long计算，否则n和t比较大时会溢出
                int lowerBound = (int) (i * (long) num / t);
                int upperBound = (int) ((i + 1) * (long) num / t);
                return secureRandom.nextInt(upperBound - lowerBound);
            })
            .toArray();
        // 执行2选1-单点批处理COT
        if (cotReceiverOutput == null) {
            bspCotReceiverOutput = bspCotReceiver.receive(innerTargetArray, (int) Math.ceil((double) num / t));
        } else {
            bspCotReceiverOutput = bspCotReceiver.receive(
                innerTargetArray, (int) Math.ceil((double) num / t), cotReceiverOutput
            );
            cotReceiverOutput = null;
        }
        stopWatch.stop();
        long bspcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, bspcotTime);

        stopWatch.start();
        // 计算输出结果
        MspCotReceiverOutput receiverOutput = generateReceiverOutput(innerTargetArray);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private MspCotReceiverOutput generateReceiverOutput(int[] innerTargetArray) {
        int[] targetArray = new int[t];
        byte[][] rbArray = IntStream.range(0, t)
            .mapToObj(i -> {
                // 需要转换为long计算，否则n和t比较大时会溢出
                int lowerBound = (int) (i * (long) num / t);
                int upperBound = (int) ((i + 1) * (long) num / t);
                targetArray[i] = innerTargetArray[i] + lowerBound;
                SspCotReceiverOutput sspcotReceiverOutput = bspCotReceiverOutput.get(i);
                return IntStream.range(0, upperBound - lowerBound)
                    .mapToObj(sspcotReceiverOutput::getRb)
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        return MspCotReceiverOutput.create(targetArray, rbArray);
    }
}
