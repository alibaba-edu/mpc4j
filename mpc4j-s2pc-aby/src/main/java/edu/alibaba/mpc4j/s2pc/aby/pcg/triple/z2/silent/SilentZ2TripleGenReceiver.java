package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.silent;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.AbstractZ2TripleGenParty;

import java.util.concurrent.TimeUnit;

/**
 * Silent Z2 triple generation receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
public class SilentZ2TripleGenReceiver extends AbstractZ2TripleGenParty {
    /**
     * NC-COT receiver
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * NC-COT sender
     */
    private final NcCotSender ncCotSender;
    /**
     * triple buffer
     */
    private Z2Triple tripleBuffer;

    public SilentZ2TripleGenReceiver(Rpc receiverRpc, Party senderParty, SilentZ2TripleGenConfig config) {
        super(SilentZ2TripleGenPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, config.getNcCotConfig());
        addSubPto(ncCotReceiver);
        ncCotSender = NcCotFactory.createSender(receiverRpc, senderParty, config.getNcCotConfig());
        addSubPto(ncCotSender);
    }

    @Override
    public void init(int expectTotalNum) throws MpcAbortException {
        setInitInput(expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int roundNum = Math.min(config.defaultRoundNum(), expectTotalNum);
        ncCotReceiver.init(roundNum);
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        ncCotSender.init(delta, roundNum);
        tripleBuffer = Z2Triple.createEmpty();
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

        int roundCount = 1;
        while (tripleBuffer.getNum() < num) {
            Z2Triple eachTriple = roundGenerate(roundCount);
            tripleBuffer.merge(eachTriple);
            roundCount++;
        }
        Z2Triple triple = tripleBuffer.split(num);

        logPhaseInfo(PtoState.PTO_END);
        return triple;
    }

    private Z2Triple roundGenerate(int roundCount) throws MpcAbortException {
        stopWatch.start();
        // S and R perform a silent R-OT. R obtains bits a and xa as output.
        CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
        stopWatch.stop();
        long firstRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 1, 3, firstRoundTime);

        stopWatch.start();
        // S and R perform a silent R-OT. S obtains bits x0, x1.
        CotSenderOutput cotSenderOutput = ncCotSender.send();
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
