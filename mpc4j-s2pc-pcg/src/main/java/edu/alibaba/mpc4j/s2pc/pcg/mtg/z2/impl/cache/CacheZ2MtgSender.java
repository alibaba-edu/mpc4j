package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.AbstractZ2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgParty;

import java.util.concurrent.TimeUnit;

/**
 * 缓存布尔三元组生成协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class CacheZ2MtgSender extends AbstractZ2MtgParty {
    /**
     * 核布尔三元组生成协议发送方
     */
    private final Z2CoreMtgParty z2CoreMtgSender;
    /**
     * max base num
     */
    private final int maxBaseNum;
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

    public CacheZ2MtgSender(Rpc senderRpc, Party receiverParty, CacheZ2MtgConfig config) {
        super(CacheZ2MtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        Z2CoreMtgConfig z2CoreMtgConfig = config.getZ2CoreMtgConfig();
        z2CoreMtgSender = Z2CoreMtgFactory.createSender(senderRpc, receiverParty, z2CoreMtgConfig);
        addSubPtos(z2CoreMtgSender);
        maxBaseNum = z2CoreMtgConfig.maxNum();
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        if (updateNum <= maxBaseNum) {
            // 如果最大数量小于支持的单轮最大数量，则执行1轮最大数量即可
            updateRoundNum = updateNum;
            updateRound = 1;
        } else {
            // 如果最大数量大于支持的单轮最大数量，则分批执行
            updateRoundNum = maxBaseNum;
            updateRound = (int)Math.ceil((double) updateNum / maxBaseNum);
        }
        z2CoreMtgSender.init(updateRoundNum);
        z2TripleBuffer = Z2Triple.createEmpty();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        while (num > z2TripleBuffer.getNum()) {
            // 如果所需的数量大于缓存区数量，则继续生成
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                Z2Triple booleanTriple = z2CoreMtgSender.generate(updateRoundNum);
                z2TripleBuffer.merge(booleanTriple);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, 0, round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        Z2Triple senderOutput = z2TripleBuffer.split(num);
        stopWatch.stop();
        long splitTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, splitTripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
