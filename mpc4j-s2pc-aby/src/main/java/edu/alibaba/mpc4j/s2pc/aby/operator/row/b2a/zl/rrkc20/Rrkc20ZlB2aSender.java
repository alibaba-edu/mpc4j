package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.AbstractZlB2aParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20.Rrkc20ZlB2aPtoDesc.*;

/**
 * RRKC20 Zl boolean to arithmetic protocol sender.
 *
 * @author Liqiang Peng
 * @date 2024/6/4
 */
public class Rrkc20ZlB2aSender extends AbstractZlB2aParty {
    /**
     * cot sender
     */
    private final CotSender cotSender;

    public Rrkc20ZlB2aSender(Rpc senderRpc, Party receiverParty, Rrkc20ZlB2aConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        cotSender.init(delta, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector b2a(MpcZ2Vector xi, Zl zl) throws MpcAbortException {
        setPtoInput(xi, zl);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfFactory.CrhfType.MMO, cotSenderOutput);
        int messageByteLength = IntUtils.boundedNonNegIntByteLength(num);
        int offset = CommonUtils.getByteLength(num) * Byte.SIZE - num;
        // P_1 generates n random values r_1, ... r_n \in Z_{n + 1} and computes r = Σ_{i = 1}^n t_i
        int[] rs = new int[num];
        IntStream.range(0, num).forEach(index -> rs[index] = secureRandom.nextInt(2));
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> senderMessagePayload = intStream
            .mapToObj(index -> {
                byte[] key0 = Arrays.copyOf(rotSenderOutput.getR0(index), messageByteLength);
                byte[] key1 = Arrays.copyOf(rotSenderOutput.getR1(index), messageByteLength);
                int rxi = BinaryUtils.getBoolean(xi.getBitVector().getBytes(), index + offset) ? 1 : 0;
                int negRxi = (rxi + rs[index]);
                byte[][] ciphertexts = new byte[2][];
                ciphertexts[0] = IntUtils.nonNegIntToFixedByteArray(rs[index], messageByteLength);
                ciphertexts[1] = IntUtils.nonNegIntToFixedByteArray(negRxi, messageByteLength);
                BytesUtils.xori(ciphertexts[0], key0);
                BytesUtils.xori(ciphertexts[1], key1);
                return ciphertexts;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        DataPacketHeader senderMessageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_ENC_ELEMENTS.ordinal(), extraInfo++,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderMessageHeader, senderMessagePayload));
        BigInteger[] as = (parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num))
            .mapToObj(i -> BigInteger.valueOf((xi.getBitVector().get(i) ? 1 : 0) + rs[i] * 2L).mod(zl.getRangeBound()))
            .toArray(BigInteger[]::new);
        stopWatch.stop();
        long b2aTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, b2aTime);

        logPhaseInfo(PtoState.PTO_END);
        return SquareZlVector.create(zl, as, false);
    }
}
