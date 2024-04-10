package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.AbstractZlDreluParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * RRK+20 Zl DReLU Sender.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlDreluSender extends AbstractZlDreluParty {
    /**
     * Millionaire sender
     */
    private final MillionaireParty millionaireSender;
    /**
     * z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * most significant bit.
     */
    private SquareZ2Vector msb;
    /**
     * remaining x
     */
    private byte[][] remainingX;

    public Rrk20ZlDreluSender(Rpc senderRpc, Party receiverParty, Rrk20ZlDreluConfig config) {
        super(Rrk20ZlDreluPtoDesc.getInstance(), senderRpc, receiverParty, config);
        millionaireSender = MillionaireFactory.createSender(senderRpc, receiverParty, config.getMillionaireConfig());
        addSubPto(millionaireSender);
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPto(z2cSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        millionaireSender.init(maxL, maxNum);
        z2cSender.init(maxL * maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector drelu(SquareZlVector xi) throws MpcAbortException {
        setPtoInput(xi);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // prepare
        stopWatch.start();
        partitionInputs(xi);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, prepareTime);

        // millionaire and xor
        stopWatch.start();
        SquareZ2Vector one = SquareZ2Vector.createOnes(num);
        SquareZ2Vector drelu;
        if (l == 1) {
            drelu = z2cSender.xor(msb, one);
        } else {
            SquareZ2Vector carry = millionaireSender.lt(l - 1, remainingX);
            drelu = z2cSender.xor(msb, z2cSender.xor(carry, one));
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        logPhaseInfo(PtoState.PTO_END);

        return drelu;
    }

    private void partitionInputs(SquareZlVector xi) {
        BitVector msbBitVector = BitVectorFactory.createZeros(num);
        BigInteger[] remaining = new BigInteger[num];
        IntStream.range(0, num).forEach(i -> {
            BigInteger x = xi.getZlVector().getElement(i);
            msbBitVector.set(i, x.testBit(l - 1));
            remaining[i] = x.setBit(l - 1).flipBit(l - 1);
        });
        remainingX = Arrays.stream(remaining)
                .map(v -> BigInteger.ONE.shiftLeft(l - 1).subtract(BigInteger.ONE).subtract(v))
                .map(v -> {
                    if (l == 1) {
                        return new byte[0];
                    } else {
                        return BigIntegerUtils.nonNegBigIntegerToByteArray(v, CommonUtils.getByteLength(l - 1));
                    }
                })
                .toArray(byte[][]::new);
        msb = SquareZ2Vector.create(msbBitVector, false);
    }
}
