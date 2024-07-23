package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.AbstractZ2TripleGenParty;

import java.util.concurrent.TimeUnit;

/**
 * Direct triple generation receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
public class DirectZ2TripleGenReceiver extends AbstractZ2TripleGenParty {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * round num
     */
    private int roundNum;
    /**
     * each num
     */
    private int eachNum;

    public DirectZ2TripleGenReceiver(Rpc receiverRpc, Party senderParty, DirectZ2TripleGenConfig config) {
        super(DirectZ2TripleGenPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public void init(int expectTotalNum) throws MpcAbortException {
        setInitInput(expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        roundNum = Math.min(config.defaultRoundNum(), expectTotalNum);
        coreCotReceiver.init();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        coreCotSender.init(delta);
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

        Z2Triple triple = Z2Triple.createEmpty();
        int roundCount = 1;
        while (triple.getNum() < num) {
            int gapNum = num - triple.getNum();
            eachNum = Math.min(gapNum, roundNum);
            Z2Triple eachTriple = roundGenerate(roundCount);
            triple.merge(eachTriple);
            roundCount++;
        }

        logPhaseInfo(PtoState.PTO_END);
        return triple;
    }

    private Z2Triple roundGenerate(int roundCount) throws MpcAbortException {
        stopWatch.start();
        // S and R perform a silent R-OT. R obtains bits a and xa as output.
        boolean[] choices = BinaryUtils.randomBinary(eachNum, secureRandom);
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choices);
        stopWatch.stop();
        long firstRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 1, 3, firstRoundTime);

        stopWatch.start();
        // S and R perform a silent R-OT. S obtains bits x0, x1.
        CotSenderOutput cotSenderOutput = coreCotSender.send(eachNum);
        stopWatch.stop();
        long secondRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 2, 3, secondRoundTime);

        stopWatch.start();
        Z2Triple eachTriple = Z2Triple.create(envType, cotSenderOutput, cotReceiverOutput);
        stopWatch.stop();
        long generateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 3, 3, generateTime);

        logPhaseInfo(PtoState.PTO_END);
        return eachTriple;
    }
}
