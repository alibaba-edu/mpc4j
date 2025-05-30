package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.structure.lpn.primal.LocalLinearCoder;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.Gf2kMspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.Gf2kMspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.Gf2kMspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp.Gf2kMspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.AbstractGf2kNcVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21.Wykw21Gf2kNcVolePtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * WYKW21-GF2K-NC-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
public class Wykw21Gf2kNcVoleReceiver extends AbstractGf2kNcVoleReceiver {
    /**
     * GF2K-MSP-VOLE config
     */
    private final Gf2kMspVoleConfig mspVoleConfig;
    /**
     * GF2K-MSP-VOLE receiver
     */
    private final Gf2kMspVoleReceiver mspVoleReceiver;
    /**
     * core GF2K-VOLE receiver
     */
    private final Gf2kCoreVoleReceiver coreVoleReceiver;
    /**
     * iteration LPN parameter k
     */
    private int iterationK;
    /**
     * iteration LPN parameter n
     */
    private int iterationN;
    /**
     * iteration LPN parameter t
     */
    private int iterationT;
    /**
     * GF2K-VOLE receiver output used in iteration
     */
    private Gf2kVoleReceiverOutput vVoleReceiverOutput;
    /**
     * GF2K-VOLE num used in GF2K-MSP-VOLE
     */
    private int mVolePreNum;
    /**
     * GF2K-VOLE receiver output used in GF2K-MSP-VOLE
     */
    private Gf2kVoleReceiverOutput mVoleReceiverOutput;
    /**
     * matrix A
     */
    private LocalLinearCoder matrixA;

    public Wykw21Gf2kNcVoleReceiver(Rpc receiverRpc, Party senderParty, Wykw21Gf2kNcVoleConfig config) {
        super(Wykw21Gf2kNcVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreVoleReceiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderParty, config.getCoreVoleConfig());
        addSubPto(coreVoleReceiver);
        mspVoleConfig = config.getMspVoleConfig();
        mspVoleReceiver = Gf2kMspVoleFactory.createReceiver(receiverRpc, senderParty, config.getMspVoleConfig());
        addSubPto(mspVoleReceiver);
    }

    @Override
    public void init(int subfieldL, byte[] delta, int num) throws MpcAbortException {
        setInitInput(subfieldL, delta, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        LpnParams setupLpnParams = Wykw21Gf2kNcVolePtoDesc.getSetupLpnParams(mspVoleConfig, num);
        int initK = setupLpnParams.getK();
        int initN = setupLpnParams.getN();
        int initT = setupLpnParams.getT();
        LpnParams iterationLpnParams = Wykw21Gf2kNcVolePtoDesc.getIterationLpnParams(mspVoleConfig, num);
        iterationK = iterationLpnParams.getK();
        iterationN = iterationLpnParams.getN();
        iterationT = iterationLpnParams.getT();
        // init core GF2K-VOLE and GF2K-MSP-VOLE
        coreVoleReceiver.init(subfieldL, delta);
        mspVoleReceiver.init(subfieldL, delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 5, initTime);

        stopWatch.start();
        // get k0 GF2K-VOLE used in setup
        Gf2kVoleReceiverOutput vInitVoleReceiverOutput = coreVoleReceiver.receive(initK);
        stopWatch.stop();
        long k0InitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 5, k0InitTime);

        stopWatch.start();
        // get seed for matrix A used in setup
        byte[][] matrixKeys = BlockUtils.randomBlocks(2, secureRandom);
        List<byte[]> matrixKeysPayload = Arrays.stream(matrixKeys).collect(Collectors.toList());
        DataPacketHeader matrixKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_KEYS.ordinal(),
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixKeysHeader, matrixKeysPayload));
        LocalLinearCoder matrixInitA = new LocalLinearCoder(envType, initK, initN, matrixKeys[0]);
        matrixInitA.setParallel(parallel);
        matrixA = new LocalLinearCoder(envType, iterationK, iterationN, matrixKeys[1]);
        matrixA.setParallel(parallel);
        stopWatch.stop();
        long keyInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 5, keyInitTime);

        stopWatch.start();
        // execute GF2K-MSP-VOLE
        Gf2kMspVoleReceiverOutput bInitMspVoleReceiverOutput = mspVoleReceiver.receive(initT, initN);
        stopWatch.stop();
        long bInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 5, bInitTime);

        stopWatch.start();
        // y = v * A + s
        byte[][] initY = matrixInitA.encode(vInitVoleReceiverOutput.getQ());
        IntStream.range(0, initN).forEach(index ->
            field.addi(initY[index], bInitMspVoleReceiverOutput.getQ(index))
        );
        mVoleReceiverOutput = Gf2kVoleReceiverOutput.create(field, delta, initY);
        vVoleReceiverOutput = mVoleReceiverOutput.split(iterationK);
        mVolePreNum = Gf2kMspVoleFactory.getPrecomputeNum(mspVoleConfig, subfieldL, iterationT, iterationN);
        mVoleReceiverOutput.reduce(mVolePreNum);
        stopWatch.stop();
        long extendInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 5, 5, extendInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(byte[] delta, int num) throws MpcAbortException {
        init(CommonConstants.BLOCK_BIT_LENGTH, delta, num);
    }

    @Override
    public Gf2kVoleReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // execute GF2K-MSP-VOLE
        Gf2kMspVoleReceiverOutput bMspVoleReceiverOutput = mspVoleReceiver.receive(iterationT, iterationN, mVoleReceiverOutput);
        stopWatch.stop();
        long bTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, bTime);

        stopWatch.start();
        // y = v * A + s
        byte[][] y = matrixA.encode(vVoleReceiverOutput.getQ());
        IntStream.range(0, iterationN).forEach(index ->
            field.addi(y[index], bMspVoleReceiverOutput.getQ(index))
        );
        // split GF2K-VOLE output into k0 + MSP-COT + output
        Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.create(field, delta, y);
        vVoleReceiverOutput = receiverOutput.split(iterationK);
        mVoleReceiverOutput = receiverOutput.split(mVolePreNum);
        receiverOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, extendTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
