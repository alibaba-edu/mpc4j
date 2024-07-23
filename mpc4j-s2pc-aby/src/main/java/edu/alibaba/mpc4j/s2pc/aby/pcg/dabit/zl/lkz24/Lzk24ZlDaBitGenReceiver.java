package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.lkz24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.AbstractZlDaBitGenParty;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.ZlDaBitTuple;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.lkz24.Lkz24ZlDaBitGenPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LZK24 Zl daBit generation receiver.
 *
 * @author Weiran Liu
 * @date 2024/7/2
 */
public class Lzk24ZlDaBitGenReceiver extends AbstractZlDaBitGenParty {
    /**
     * default round num
     */
    private final int defaultRounNum;
    /**
     * COT receiver
     */
    private final CotReceiver cotReceiver;

    public Lzk24ZlDaBitGenReceiver(Rpc receiverRpc, Party senderParty, Lkz24ZlDaBitGenConfig config) {
        super(Lkz24ZlDaBitGenPtoDesc.getInstance(), receiverRpc, senderParty, config);
        CotConfig cotConfig = config.getCotConfig();
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, cotConfig);
        addSubPto(cotReceiver);
        defaultRounNum = cotConfig.defaultRoundNum();
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init(expectTotalNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxL) throws MpcAbortException {
        init(maxL, defaultRounNum);
    }

    @Override
    public ZlDaBitTuple generate(Zl zl, int num) throws MpcAbortException {
        setPtoInput(zl, num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P0 and P1 invokes OT, where P1 as the receiver inputs [r_1]_2, and then receives [s]_1^p = m_[r_1^2]
        // here we reuse random choice bits
        CotReceiverOutput cotReceiverOutput = cotReceiver.receiveRandom(num);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        List<byte[]> m0EncPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_CIPHERTEXTS.ordinal());
        List<byte[]> m1EncPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_CIPHERTEXTS.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(m0EncPayload.size() == num);
        MpcAbortPreconditions.checkArgument(m1EncPayload.size() == num);
        byte[][] m0EncBytes = m0EncPayload.toArray(new byte[0][]);
        byte[][] m1EncBytes = m1EncPayload.toArray(new byte[0][]);
        ZlVector s1Vector = ZlVector.createZeros(zl, num);
        if (l <= CommonConstants.BLOCK_BIT_LENGTH) {
            RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
            IntStream indexIntStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
            indexIntStream.forEach(i -> {
                byte[] plaintext = Arrays.copyOf(rotReceiverOutput.getRb(i), byteL);
                if (rotReceiverOutput.getChoice(i)) {
                    BytesUtils.xori(plaintext, m1EncBytes[i]);
                } else {
                    BytesUtils.xori(plaintext, m0EncBytes[i]);
                }
                BigInteger s1 = BigIntegerUtils.byteArrayToNonNegBigInteger(plaintext);
                s1Vector.setElement(i, zl.neg(s1));
            });
        } else {
            Prg prg = PrgFactory.createInstance(envType, byteL);
            IntStream indexIntStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
            indexIntStream.forEach(i -> {
                byte[] plaintext = prg.extendToBytes(cotReceiverOutput.getRb(i));
                if (cotReceiverOutput.getChoice(i)) {
                    BytesUtils.xori(plaintext, m1EncBytes[i]);
                } else {
                    BytesUtils.xori(plaintext, m0EncBytes[i]);
                }
                BigInteger s1 = BigIntegerUtils.byteArrayToNonNegBigInteger(plaintext);
                s1Vector.setElement(i, zl.neg(s1));
            });
        }
        BitVector r1Vector = BitVectorFactory.create(num, BinaryUtils.binaryToRoundByteArray(cotReceiverOutput.getChoices()));
        stopWatch.stop();
        long z2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, z2Time);

        logPhaseInfo(PtoState.PTO_END);
        return ZlDaBitTuple.create(SquareZlVector.create(s1Vector, false), SquareZ2Vector.create(r1Vector, false));
    }
}
