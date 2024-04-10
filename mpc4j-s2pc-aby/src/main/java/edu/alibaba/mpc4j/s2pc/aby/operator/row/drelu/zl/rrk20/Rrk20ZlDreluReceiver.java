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
 * RRK+20 Zl DReLU Receiver.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlDreluReceiver extends AbstractZlDreluParty {
    /**
     * Millionaire receiver
     */
    private final MillionaireParty millionaireReceiver;
    /**
     * z2 circuit receiver.
     */
    private final Z2cParty z2cReceiver;
    /**
     * most significant bit.
     */
    private SquareZ2Vector msb;
    /**
     * remaining x
     */
    private byte[][] remainingX;

    public Rrk20ZlDreluReceiver(Rpc receiverRpc, Party senderParty, Rrk20ZlDreluConfig config) {
        super(Rrk20ZlDreluPtoDesc.getInstance(), receiverRpc, senderParty, config);
        millionaireReceiver = MillionaireFactory.createReceiver(receiverRpc, senderParty, config.getMillionaireConfig());
        addSubPto(millionaireReceiver);
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        addSubPto(z2cReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        millionaireReceiver.init(maxL, maxNum);
        z2cReceiver.init(maxL * maxNum);
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
            drelu = z2cReceiver.xor(msb, one);
        } else {
            SquareZ2Vector carry = millionaireReceiver.lt(l - 1, remainingX);
            drelu = z2cReceiver.xor(msb, z2cReceiver.xor(carry, one));
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
