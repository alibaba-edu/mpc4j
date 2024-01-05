package edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulElligatorEcc;
import edu.alibaba.mpc4j.common.tool.crypto.engine.Rijndael256Engine;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21.Rt21PsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType.X25519_ELLIGATOR_BC;

/**
 * RT21-PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/8/10
 */
public class Rt21PsiClient<T> extends AbstractPsiClient<T> {
    /**
     * Elligator ECC instance
     */
    private final ByteMulElligatorEcc byteMulEcc;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * OKVS key num
     */
    private final int okvsKeyNum;
    /**
     * OKVS key
     */
    private byte[][] okvsKey;
    /**
     * m
     */
    private byte[] m;
    /**
     * H1: {0, 1}^* → F
     */
    private final Hash h1;
    /**
     * H2: {0, 1}^* × F → {0, 1}^{2κ}
     */
    private final Prf h2;
    /**
     * 256-bit decryption engine
     */
    private final Rijndael256Engine decEngine;
    /**
     * b_i
     */
    private byte[][] bArray;

    public Rt21PsiClient(Rpc clientRpc, Party serverParty, Rt21PsiConfig config) {
        super(Rt21PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        byteMulEcc = ByteEccFactory.createMulElligatorInstance(X25519_ELLIGATOR_BC);
        MathPreconditions.checkEqual(
            "point_byte_length", "field_byte_length",
            byteMulEcc.pointByteLength(), Rt21PsiPtoDesc.FIELD_BYTE_LENGTH
        );
        okvsType = config.getOkvsType();
        okvsKeyNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
        h1 = HashFactory.createInstance(envType, Rt21PsiPtoDesc.FIELD_BYTE_LENGTH);
        h2 = PrfFactory.createInstance(envType, Rt21PsiPtoDesc.FIELD_BYTE_LENGTH);
        h2.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        decEngine = new Rijndael256Engine();
        MathPreconditions.checkEqual(
            "block_byte_length", "field_byte_length",
            decEngine.getBlockByteLength(), Rt21PsiPtoDesc.FIELD_BYTE_LENGTH
        );
        decEngine.init(false, new byte[decEngine.getKeyByteLength()]);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // generate OKVS key
        okvsKey = CommonUtils.generateRandomKeys(okvsKeyNum, secureRandom);
        List<byte[]> okvsKeyPayload = Arrays.stream(okvsKey).collect(Collectors.toList());
        DataPacketHeader okvsKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsKeyHeader, okvsKeyPayload));
        stopWatch.stop();
        long okvsKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, okvsKeyTime);

        DataPacketHeader msgHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_INIT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> msgPayload = rpc.receive(msgHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(msgPayload.size() == 1);
        m = msgPayload.get(0);
        stopWatch.stop();
        long msgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, msgTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // generate OKVS
        List<byte[]> okvsPayload = generateOkvsPayload();
        DataPacketHeader polyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(polyHeader, okvsPayload));
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, okvsTime, "Client generates OKVS");

        stopWatch.start();
        IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
        clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
        byte[][] kArray = clientElementIntStream
            .mapToObj(index -> {
                byte[] bi = bArray[index];
                return byteMulEcc.uniformMul(m, bi);
            })
            .toArray(byte[][]::new);
        bArray = null;
        stopWatch.stop();
        long kTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, kTime, "Client generates key_2");

        DataPacketHeader peqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> peqtPayload = rpc.receive(peqtHeader).getPayload();

        stopWatch.start();
        Set<T> intersection = handlePeqtPayload(kArray, peqtPayload);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, peqtTime, "Client computes intersection");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private List<byte[]> generateOkvsPayload() {
        bArray = new byte[clientElementSize][];
        IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
        clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
        Map<ByteBuffer, byte[]> map = clientElementIntStream
            .boxed()
            .collect(Collectors.toMap(
                index -> {
                    byte[] elementByteArray = ObjectUtils.objectToByteArray(clientElementArrayList.get(index));
                    return ByteBuffer.wrap(h1.digestToBytes(elementByteArray));
                },
                index -> {
                    byte[] point = new byte[Rt21PsiPtoDesc.FIELD_BYTE_LENGTH];
                    byte[] mpi = new byte[Rt21PsiPtoDesc.FIELD_BYTE_LENGTH];
                    boolean success = false;
                    while (!success) {
                        // b_i ← KA.R, m'_i = KA.msg_2(b_i, m)
                        bArray[index] = byteMulEcc.randomScalar(secureRandom);
                        success =  byteMulEcc.baseMul(bArray[index], point, mpi);
                    }
                    // f_i = PRP^{-1}(m'_i)
                    return decEngine.doFinal(mpi);
                })
            );
        // P = encode(H1(y), f)
        Gf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createInstance(
            envType, okvsType, clientElementSize, Rt21PsiPtoDesc.FIELD_BIT_LENGTH, okvsKey
        );
        return Arrays.asList(dokvs.encode(map, true));
    }

    private Set<T> handlePeqtPayload(byte[][] kArray, List<byte[]> peqtPayload) {
        Filter<byte[]> peqtFilter = FilterFactory.load(envType, peqtPayload);
        IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
        clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
        return clientElementIntStream
            .mapToObj(index -> {
                T element = clientElementArrayList.get(index);
                byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                byte[] h2Input = ByteBuffer.allocate(kArray[index].length + elementByteArray.length)
                    .put(kArray[index])
                    .put(elementByteArray)
                    .array();
                return peqtFilter.mightContain(h2.getBytes(h2Input)) ? element : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
