package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.AbstractNcLnotSender;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * COT no-choice 1-out-of-n (with n = 2^l) OT sender.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class CotNcLnotSender extends AbstractNcLnotSender {
    /**
     * no-choice COT sender
     */
    private final NcCotSender ncCotSender;
    /**
     * the maximal COT num
     */
    private final int maxCotBaseNum;
    /**
     * update round
     */
    private int updateRound;
    /**
     * Δ
     */
    private byte[] delta;

    public CotNcLnotSender(Rpc senderRpc, Party receiverParty, CotNcLnotConfig config) {
        super(CotNcLnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        NcCotConfig ncCotConfig = config.getNcCotConfig();
        ncCotSender = NcCotFactory.createSender(senderRpc, receiverParty, ncCotConfig);
        addSubPto(ncCotSender);
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
        delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        // log(n) * num
        ncCotSender.init(delta, perRoundNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotSenderOutput send() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        CotSenderOutput cotSenderOutput = CotSenderOutput.createEmpty(delta);
        for (int round = 1; round <= updateRound; round++) {
            stopWatch.start();
            CotSenderOutput roundCotSenderOutput = ncCotSender.send();
            cotSenderOutput.merge(roundCotSenderOutput);
            stopWatch.stop();
            long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logSubStepInfo(PtoState.PTO_STEP, 1, round, updateRound, roundTime);
        }

        stopWatch.start();
        cotSenderOutput.reduce(l * num);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfFactory.CrhfType.MMO, cotSenderOutput);
        // convert COT sender output to be LNOT sender output
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][][] rsArray = indexIntStream
            .mapToObj(index -> {
                int cotIndex = index * l;
                byte[][] rs = new byte[n][];
                for (int choice = 0; choice < n; choice++) {
                    rs[choice] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    int tempChoice = choice;
                    for (int bitPosition = l - 1; bitPosition >= 0; bitPosition--) {
                        boolean bit = (tempChoice % 2) == 1;
                        if (!bit) {
                            BytesUtils.xori(rs[choice], rotSenderOutput.getR0(cotIndex + bitPosition));
                        } else {
                            BytesUtils.xori(rs[choice], rotSenderOutput.getR1(cotIndex + bitPosition));
                        }
                        tempChoice = (tempChoice >> 1);
                    }
                }
                return rs;
            })
            .toArray(byte[][][]::new);
        LnotSenderOutput senderOutput = LnotSenderOutput.create(l, rsArray);
        stopWatch.stop();
        long convertTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, convertTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
