package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.AbstractNcLotReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 直接NC-2^l选1-OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/8/18
 */
public class DirectNcLotReceiver extends AbstractNcLotReceiver {
    /**
     * 核2^l选1-OT协议接收方
     */
    private final CoreLotReceiver coreLotReceiver;

    public DirectNcLotReceiver(Rpc receiverRpc, Party senderParty, DirectNcLotConfig config) {
        super(DirectNcLotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreLotReceiver = CoreLotFactory.createReceiver(receiverRpc, senderParty, config.getCoreLotConfig());
        coreLotReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        coreLotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreLotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreLotReceiver.addLogLevel();
    }

    @Override
    public void init(int inputBitLength, int num) throws MpcAbortException {
        setInitInput(inputBitLength, num);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        coreLotReceiver.init(inputBitLength, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());

    }

    @Override
    public LotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        byte[][] choices = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] choice = new byte[inputByteLength];
                secureRandom.nextBytes(choice);
                BytesUtils.reduceByteArray(choice, inputBitLength);
                return choice;
            })
            .toArray(byte[][]::new);
        LotReceiverOutput receiverOutput = coreLotReceiver.receive(choices);
        stopWatch.stop();
        long coreLotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        receiverOutput.reduce(num);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), coreLotTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }
}
