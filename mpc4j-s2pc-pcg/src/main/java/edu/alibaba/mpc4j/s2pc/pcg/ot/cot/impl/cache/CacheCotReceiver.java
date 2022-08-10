package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * 缓存COT接收方。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CacheCotReceiver extends AbstractCotReceiver {
    /**
     * NC-COT接收方
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * 预计算COT协议接收方
     */
    private final PreCotReceiver preCotReceiver;
    /**
     * 更新时的执行轮数
     */
    private int updateRound;
    /**
     * 缓存区
     */
    private CotReceiverOutput buffer;

    public CacheCotReceiver(Rpc receiverRpc, Party senderParty, CacheCotConfig config) {
        super(CacheCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, config.getNcCotConfig());
        ncCotReceiver.addLogLevel();
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        preCotReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // NC-COT协议和预计算COT协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        ncCotReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        preCotReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        ncCotReceiver.setParallel(parallel);
        preCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        ncCotReceiver.addLogLevel();
        preCotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int updateRoundNum;
        if (updateNum <= config.maxBaseNum()) {
            // 如果最大数量小于基础最大数量，则执行1轮最大数量即可
            updateRoundNum = updateNum;
            updateRound = 1;
        } else {
            // 如果最大数量大于基础最大数量，则分批执行
            updateRoundNum = config.maxBaseNum();
            updateRound = (int) Math.ceil((double) updateNum / config.maxBaseNum());
        }
        ncCotReceiver.init(updateRoundNum);
        buffer = CotReceiverOutput.createEmpty();
        preCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        while (num > buffer.getNum()) {
            // 如果所需的数量大于缓存区数量，则继续生成
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
                buffer.merge(cotReceiverOutput);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                info("{}{} Recv. Step 0.{}/0.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        CotReceiverOutput receiverOutput = buffer.split(num);
        stopWatch.stop();
        long splitTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), splitTripleTime);

        stopWatch.start();
        // 应用预计算COT协议纠正选择比特
        receiverOutput = preCotReceiver.receive(receiverOutput, choices);
        stopWatch.stop();
        long preCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), preCotTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }
}
