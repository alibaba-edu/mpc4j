package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.AbstractNcLnotReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * COT no-choice 1-out-of-n (with n = 2^l) OT receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class CotNcLnotReceiver extends AbstractNcLnotReceiver {
    /**
     * no-choice COT receiver
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * the maximal COT num
     */
    private final int maxCotBaseNum;
    /**
     * update round
     */
    private int updateRound;

    public CotNcLnotReceiver(Rpc receiverRpc, Party senderParty, CotNcLnotConfig config) {
        super(CotNcLnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        NcCotConfig ncCotConfig = config.getNcCotConfig();
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, ncCotConfig);
        addSubPto(ncCotReceiver);
        maxCotBaseNum = ncCotConfig.maxNum();
    }

    @Override
    public void init(int l, int num) throws MpcAbortException {
        setInitInput(l, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int cotNum = l * num;
        int perRoundNum;
        if (cotNum <= maxCotBaseNum) {
            // we need to run single round
            perRoundNum = cotNum;
            updateRound = 1;
        } else {
            // we need to run multiple round
            perRoundNum = maxCotBaseNum;
            updateRound = (int) Math.ceil((double) cotNum / maxCotBaseNum);
        }
        ncCotReceiver.init(perRoundNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        CotReceiverOutput cotReceiverOutput = CotReceiverOutput.createEmpty();
        for (int round = 1; round <= updateRound; round++) {
            stopWatch.start();
            CotReceiverOutput roundCotReceiverOutput = ncCotReceiver.receive();
            cotReceiverOutput.merge(roundCotReceiverOutput);
            stopWatch.stop();
            long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logSubStepInfo(PtoState.PTO_STEP, 1, round, updateRound, roundTime);
        }

        stopWatch.start();
        cotReceiverOutput.reduce(l * num);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfFactory.CrhfType.MMO, cotReceiverOutput);
        // convert COT receiver output to be LNOT receiver output
        int[] choiceArray = new int[num];
        byte[][] rbArray = new byte[num][];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream
            .forEach(index -> {
                int cotIndex = index * l;
                choiceArray[index] = 0;
                rbArray[index] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for (int bitPosition = 0; bitPosition < l; bitPosition++) {
                    boolean choiceBit = rotReceiverOutput.getChoice(cotIndex + bitPosition);
                    choiceArray[index] = choiceBit ? (choiceArray[index] << 1) + 1 : (choiceArray[index] << 1);
                    BytesUtils.xori(rbArray[index], rotReceiverOutput.getRb(cotIndex + bitPosition));
                }
            });
        LnotReceiverOutput receiverOutput = LnotReceiverOutput.create(l, choiceArray, rbArray);
        stopWatch.stop();
        long convertTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, convertTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
