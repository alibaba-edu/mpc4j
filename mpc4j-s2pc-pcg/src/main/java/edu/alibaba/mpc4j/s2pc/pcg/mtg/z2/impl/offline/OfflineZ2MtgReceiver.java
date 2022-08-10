package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.AbstractZ2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgParty;

import java.util.concurrent.TimeUnit;

/**
 * 离线布尔三元组生成协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
public class OfflineZ2MtgReceiver extends AbstractZ2MtgParty {
    /**
     * 核布尔三元组生成协议接收方
     */
    private final Z2CoreMtgParty z2CoreMtgReceiver;
    /**
     * 更新时的单次数量
     */
    private int updateRoundNum;
    /**
     * 更新时的执行轮数
     */
    private int updateRound;
    /**
     * 缓存区
     */
    private Z2Triple z2TripleBuffer;

    public OfflineZ2MtgReceiver(Rpc receiverRpc, Party senderParty, OfflineZ2MtgConfig config) {
        super(OfflineZ2MtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2CoreMtgReceiver = Z2CoreMtgFactory.createReceiver(receiverRpc, senderParty, config.getZ2CoreMtgConfig());
        z2CoreMtgReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        z2CoreMtgReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        z2CoreMtgReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        z2CoreMtgReceiver.addLogLevel();
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        if (updateNum <= config.maxBaseNum()) {
            // 如果最大数量小于支持的单轮最大数量，则执行1轮最大数量即可
            this.updateRoundNum = updateNum;
            updateRound = 1;
        } else {
            // 如果最大数量大于支持的单轮最大数量，则分批执行
            this.updateRoundNum = config.maxBaseNum();
            updateRound = (int) Math.ceil((double) updateNum / config.maxBaseNum());
        }
        z2CoreMtgReceiver.init(this.updateRoundNum);
        z2TripleBuffer = Z2Triple.createEmpty();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/2.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), updateRound, initTime);

        // 生成所需的布尔三元组
        for (int round = 1; round <= updateRound; round++) {
            stopWatch.start();
            Z2Triple booleanTriple = z2CoreMtgReceiver.generate(this.updateRoundNum);
            z2TripleBuffer.merge(booleanTriple);
            stopWatch.stop();
            long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. Init Step 2.{}/2.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), round, updateRound, roundTime);
        }

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        while (num > z2TripleBuffer.getNum()) {
            // 如果所需的数量大于缓存区数量，则继续生成
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                Z2Triple booleanTriple = z2CoreMtgReceiver.generate(updateRoundNum);
                z2TripleBuffer.merge(booleanTriple);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                info("{}{} Recv. Step 0.{}/0.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        Z2Triple receiverOutput = z2TripleBuffer.split(num);
        stopWatch.stop();
        long splitTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), splitTripleTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }
}
