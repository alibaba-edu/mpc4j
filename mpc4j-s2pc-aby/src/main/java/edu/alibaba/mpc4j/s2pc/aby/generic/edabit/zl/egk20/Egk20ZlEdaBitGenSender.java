package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.egk20;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.AbstractZlEdaBitGenParty;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.PlainZlEdaBitVector;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.SquareZlEdaBitVector;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * EGK20+ Zl edaBit generation sender.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class Egk20ZlEdaBitGenSender extends AbstractZlEdaBitGenParty {
    /**
     * Zl sender
     */
    private final ZlcParty zlcSender;
    /**
     * Z2 sender
     */
    private final Z2cParty z2cSender;
    /**
     * Z2 integer circuit
     */
    private final Z2IntegerCircuit z2IntegerCircuit;

    public Egk20ZlEdaBitGenSender(Rpc senderPpc, Party receiverParty, Egk20ZlEdaBitGenConfig config) {
        super(Egk20ZlEdaBitGenPtoDesc.getInstance(), senderPpc, receiverParty, config);
        zlcSender = ZlcFactory.createSender(senderPpc, receiverParty, config.getZlcConfig());
        addSubPto(zlcSender);
        z2cSender = Z2cFactory.createSender(senderPpc, receiverParty, config.getZ2cConfig());
        addSubPto(z2cSender);
        z2IntegerCircuit = new Z2IntegerCircuit(z2cSender);
    }

    public Egk20ZlEdaBitGenSender(Rpc senderPpc, Party receiverParty, Party aiderParty, Egk20ZlEdaBitGenConfig config) {
        super(Egk20ZlEdaBitGenPtoDesc.getInstance(), senderPpc, receiverParty, config);
        zlcSender = ZlcFactory.createSender(senderPpc, receiverParty, aiderParty, config.getZlcConfig());
        addSubPto(zlcSender);
        z2cSender = Z2cFactory.createSender(senderPpc, receiverParty, aiderParty, config.getZ2cConfig());
        addSubPto(z2cSender);
        z2IntegerCircuit = new Z2IntegerCircuit(z2cSender);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zlcSender.init(maxNum);
        z2cSender.init(maxNum * l);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlEdaBitVector generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // The parties generate private edaBit vector.
        PlainZlEdaBitVector plainZlEdaBitVector = PlainZlEdaBitVector.createRandom(zl, num, secureRandom);
        // share own vector
        SquareZlVector squareZlVector0 = zlcSender.shareOwn(plainZlEdaBitVector.getZlVector());
        BitVector[] bitVectors = plainZlEdaBitVector.getBitVectors();
        SquareZ2Vector[] squareZ2Vectors0 = new SquareZ2Vector[l];
        for (int i = 0; i < l; i++) {
            squareZ2Vectors0[i] = z2cSender.shareOwn(bitVectors[i]);
        }
        // share other vector
        SquareZlVector squareZlVector1 = zlcSender.shareOther(num);
        SquareZ2Vector[] squareZ2Vectors1 = new SquareZ2Vector[l];
        for (int i = 0; i < l; i++) {
            squareZ2Vectors1[i] = z2cSender.shareOther(num);
        }
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, shareTime);

        stopWatch.start();
        // The parties invoke F_{ABB} to compute [r']_M = [r_0]_M = [r_1]_M.
        SquareZlVector primeSquareZlVector = zlcSender.add(squareZlVector0, squareZlVector1);
        // The parties invoke F_{ABB} to compute nBitADD([r_0]_2, [r_1]_2).
        MpcZ2Vector[] primeMpcZ2Vector = z2IntegerCircuit.add(squareZ2Vectors0, squareZ2Vectors1);
        SquareZ2Vector[] primeSquareZ2Vectors = Arrays.stream(primeMpcZ2Vector)
            .map(mpcZ2Vector -> (SquareZ2Vector) mpcZ2Vector)
            .toArray(SquareZ2Vector[]::new);
        SquareZlEdaBitVector senderOutput = SquareZlEdaBitVector.create(primeSquareZlVector, primeSquareZ2Vectors);
        stopWatch.stop();
        long computeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, computeTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
