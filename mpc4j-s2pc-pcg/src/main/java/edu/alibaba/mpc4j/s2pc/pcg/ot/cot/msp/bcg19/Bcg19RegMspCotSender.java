package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.SspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.AbstractMspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSenderOutput;

/**
 * BCG19-REG-MSP-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/25
 */
public class Bcg19RegMspCotSender extends AbstractMspCotSender {
    /**
     * BSP-COT协议发送方
     */
    private final BspCotSender bspCotSender;
    /**
     * 预计算发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * BSP-COT协议发送方输出
     */
    private BspCotSenderOutput bspCotSenderOutput;

    public Bcg19RegMspCotSender(Rpc senderRpc, Party receiverParty, Bcg19RegMspCotConfig config) {
        super(Bcg19RegMspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bspCotSender = BspCotFactory.createSender(senderRpc, receiverParty, config.getBspCotConfig());
        bspCotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        bspCotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        bspCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        bspCotSender.addLogLevel();
    }

    @Override
    public void init(byte[] delta, int maxT, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxT, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 原本应该设置为maxN / maxT，但此值不与T成正比，最后考虑直接设置为maxN
        bspCotSender.init(delta, maxT, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public MspCotSenderOutput send(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return send();
    }

    @Override
    public MspCotSenderOutput send(int t, int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(t, num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private MspCotSenderOutput send() throws MpcAbortException {
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 执行t次2选1-单点批处理COT，每一个的选择范围是n/t
        if (cotSenderOutput == null) {
            bspCotSenderOutput = bspCotSender.send(t, (int)Math.ceil((double) num / t));
        } else {
            bspCotSenderOutput = bspCotSender.send(t, (int)Math.ceil((double) num / t), cotSenderOutput);
            cotSenderOutput = null;
        }
        stopWatch.stop();
        long bspcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), bspcotTime);

        stopWatch.start();
        // 计算输出结果
        MspCotSenderOutput senderOutput = generateSenderOutput();
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), outputTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private MspCotSenderOutput generateSenderOutput() {
        byte[][] r0Array = IntStream.range(0, t)
            .mapToObj(i -> {
                // 需要转换为long计算，否则n和t比较大时会溢出
                int lowerBound = (int)(i * (long) num / t);
                int upperBound = (int)((i + 1) * (long) num / t);
                SspCotSenderOutput spcotSenderOutput = bspCotSenderOutput.get(i);
                return IntStream.range(0, upperBound - lowerBound)
                    .mapToObj(spcotSenderOutput::getR0)
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        bspCotSenderOutput = null;
        return MspCotSenderOutput.create(delta, r0Array);
    }
}
