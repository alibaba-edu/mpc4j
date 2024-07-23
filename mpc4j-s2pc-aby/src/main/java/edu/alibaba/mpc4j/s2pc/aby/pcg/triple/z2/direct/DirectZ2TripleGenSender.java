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
 * Direct Z2 triple generation sender.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
public class DirectZ2TripleGenSender extends AbstractZ2TripleGenParty {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * round num
     */
    private int roundNum;
    /**
     * each num
     */
    private int eachNum;

    public DirectZ2TripleGenSender(Rpc senderRpc, Party receiverParty, DirectZ2TripleGenConfig config) {
        super(DirectZ2TripleGenPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int expectTotalNum) throws MpcAbortException {
        setInitInput(expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        roundNum = Math.min(config.defaultRoundNum(), expectTotalNum);
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        coreCotSender.init(delta);
        coreCotReceiver.init();
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
        // S and R perform a silent R-OT. S obtains bits x0, x1.
        CotSenderOutput cotSenderOutput = coreCotSender.send(eachNum);
        stopWatch.stop();
        long firstRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 1, 3, firstRoundTime);

        stopWatch.start();
        // S and R perform a silent R-OT. R obtains bits a and xa as output.
        boolean[] choices = BinaryUtils.randomBinary(eachNum, secureRandom);
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choices);
        stopWatch.stop();
        long secondRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 2, 3, secondRoundTime);

        stopWatch.start();
        Z2Triple eachTriple = Z2Triple.create(envType, cotSenderOutput, cotReceiverOutput);
        long generateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 3, 3, generateTime);

        logPhaseInfo(PtoState.PTO_END);
        return eachTriple;
    }
}
