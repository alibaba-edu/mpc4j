package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * 缓存COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CacheCotSender extends AbstractCotSender {
    /**
     * NC-COT发送方
     */
    private final NcCotSender nccotSender;
    /**
     * 预计算COT协议发送方
     */
    private final PreCotSender preCotSender;
    /**
     * 更新时的执行轮数
     */
    private int updateRound;
    /**
     * 缓存区
     */
    private CotSenderOutput buffer;

    public CacheCotSender(Rpc senderRpc, Party receiverParty, CacheCotConfig config) {
        super(CacheCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        nccotSender = NcCotFactory.createSender(senderRpc, receiverParty, config.getNcCotConfig());
        nccotSender.addLogLevel();
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        preCotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // NCCOT协议和PCOT协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        nccotSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        preCotSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        nccotSender.setParallel(parallel);
        preCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        nccotSender.addLogLevel();
        preCotSender.addLogLevel();
    }

    @Override
    public void init(byte[] delta, int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(delta, maxRoundNum, updateNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

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
        nccotSender.init(delta, updateRoundNum);
        preCotSender.init();
        buffer = CotSenderOutput.createEmpty(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        while (num > buffer.getNum()) {
            // 如果所需的数量大于缓存区数量，则继续生成
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                CotSenderOutput cotSenderOutput = nccotSender.send();
                buffer.merge(cotSenderOutput);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                info("{}{} Send. Step 0.{}/0.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        CotSenderOutput senderOutput = buffer.split(num);
        stopWatch.stop();
        long splitTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), splitTripleTime);

        stopWatch.start();
        // 应用预计算COT协议纠正选择比特
        senderOutput = preCotSender.send(senderOutput);
        stopWatch.stop();
        long preCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), preCotTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }
}
