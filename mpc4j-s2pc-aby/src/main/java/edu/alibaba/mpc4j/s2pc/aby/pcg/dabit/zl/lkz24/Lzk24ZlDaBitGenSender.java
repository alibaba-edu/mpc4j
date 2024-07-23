package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.lkz24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.AbstractZlDaBitGenParty;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.ZlDaBitTuple;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.lkz24.Lkz24ZlDaBitGenPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LKZ24 Zl daBit generation sender.
 *
 * @author Weiran Liu
 * @date 2024/7/2
 */
public class Lzk24ZlDaBitGenSender extends AbstractZlDaBitGenParty {
    /**
     * default round num
     */
    private final int defaultRounNum;
    /**
     * COT sender
     */
    private final CotSender cotSender;

    public Lzk24ZlDaBitGenSender(Rpc senderRpc, Party receiverParty, Lkz24ZlDaBitGenConfig config) {
        super(Lkz24ZlDaBitGenPtoDesc.getInstance(), senderRpc, receiverParty, config);
        CotConfig cotConfig = config.getCotConfig();
        cotSender = CotFactory.createSender(senderRpc, receiverParty, cotConfig);
        addSubPto(cotSender);
        defaultRounNum = cotConfig.defaultRoundNum();
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        cotSender.init(delta, expectTotalNum);
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
        // P0 samples [r]_0^2, [s]_0^p
        BitVector r0Vector = BitVectorFactory.createRandom(num, secureRandom);
        ZlVector s0Vector = ZlVector.createRandom(zl, num, secureRandom);
        // P0 sets m_0 = [s]_0^p - [r]_0^2 and m_1 = [s]_0^p - (1 - [r]_0^2)
        ZlVector m0Vector = ZlVector.createZeros(zl, num);
        ZlVector m1Vector = ZlVector.createZeros(zl, num);
        IntStream.range(0, num).forEach(i -> {
            if (r0Vector.get(i)) {
                m0Vector.setElement(i, zl.sub(s0Vector.getElement(i), zl.createOne()));
                m1Vector.setElement(i, s0Vector.getElement(i));
            } else {
                m0Vector.setElement(i, s0Vector.getElement(i));
                m1Vector.setElement(i, zl.sub(s0Vector.getElement(i), zl.createOne()));
            }
        });
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, paramTime);

        stopWatch.start();
        CotSenderOutput cotSenderOutput = cotSender.sendRandom(num);
        // P0 and P1 invokes OT, where P0 as the sender inputs m_0 = [s]_0^p - [r]_0^2 and m_1 = [s]_0^p - (1 - [r]_0^2)
        List<byte[]> m0EncPayload, m1EncPayload;
        IntStream indexIntStream;
        if (l <= CommonConstants.BLOCK_BIT_LENGTH) {
            RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
            indexIntStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
            m0EncPayload = indexIntStream
                .mapToObj(i -> {
                    byte[] ciphertext0 = Arrays.copyOf(rotSenderOutput.getR0(i), byteL);
                    BytesUtils.xori(ciphertext0, BigIntegerUtils.nonNegBigIntegerToByteArray(m0Vector.getElement(i), byteL));
                    return ciphertext0;
                })
                .toList();
            indexIntStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
            m1EncPayload = indexIntStream
                .mapToObj(i -> {
                    byte[] ciphertext1 = Arrays.copyOf(rotSenderOutput.getR1(i), byteL);
                    BytesUtils.xori(ciphertext1, BigIntegerUtils.nonNegBigIntegerToByteArray(m1Vector.getElement(i), byteL));
                    return ciphertext1;
                })
                .toList();
        } else {
            Prg prg = PrgFactory.createInstance(envType, byteL);
            indexIntStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
            m0EncPayload = indexIntStream
                .mapToObj(i -> {
                    byte[] ciphertext0 = prg.extendToBytes(cotSenderOutput.getR0(i));
                    BytesUtils.xori(ciphertext0, BigIntegerUtils.nonNegBigIntegerToByteArray(m0Vector.getElement(i), byteL));
                    return ciphertext0;
                })
                .toList();
            indexIntStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
            m1EncPayload = indexIntStream
                .mapToObj(i -> {
                    byte[] ciphertext1 = prg.extendToBytes(cotSenderOutput.getR1(i));
                    BytesUtils.xori(ciphertext1, BigIntegerUtils.nonNegBigIntegerToByteArray(m1Vector.getElement(i), byteL));
                    return ciphertext1;
                })
                .toList();
        }
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_CIPHERTEXTS.ordinal(), m0EncPayload);
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_CIPHERTEXTS.ordinal(), m1EncPayload);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        // P0 sets [t]_0^p = [s]_0^p
        return ZlDaBitTuple.create(SquareZlVector.create(s0Vector, false), SquareZ2Vector.create(r0Vector, false));
    }
}
