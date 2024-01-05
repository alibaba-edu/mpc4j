package edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulElligatorEcc;
import edu.alibaba.mpc4j.common.tool.crypto.engine.Rijndael256Engine;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21.Rt21PsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RT21-PSI server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/8/10
 */
public class Rt21PsiServer<T> extends AbstractPsiServer<T> {
    /**
     * Elligator ECC instance
     */
    private final ByteMulElligatorEcc byteMulEcc;
    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * OKVS key num
     */
    private final int okvsKeyNum;
    /**
     * H1: {0, 1}^* → F
     */
    private final Hash h1;
    /**
     * H2: {0, 1}^* × F → {0, 1}^{2κ}
     */
    private final Prf h2;
    /**
     * 256-bit encryption engine
     */
    private final Rijndael256Engine encEngine;
    /**
     * OKVS key
     */
    private byte[][] okvsKey;
    /**
     * a ← KA.R
     */
    private byte[] a;

    public Rt21PsiServer(Rpc serverRpc, Party clientParty, Rt21PsiConfig config) {
        super(Rt21PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        byteMulEcc = ByteEccFactory.createMulElligatorInstance(ByteEccType.X25519_ELLIGATOR_BC);
        MathPreconditions.checkEqual(
            "point_byte_length", "field_byte_length",
            byteMulEcc.pointByteLength(), Rt21PsiPtoDesc.FIELD_BYTE_LENGTH
        );
        filterType = config.getFilterType();
        okvsType = config.getOkvsType();
        okvsKeyNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
        h1 = HashFactory.createInstance(envType, Rt21PsiPtoDesc.FIELD_BYTE_LENGTH);
        h2 = PrfFactory.createInstance(envType, Rt21PsiPtoDesc.FIELD_BYTE_LENGTH);
        h2.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        encEngine = new Rijndael256Engine();
        MathPreconditions.checkEqual(
            "block_byte_length", "field_byte_length",
            encEngine.getBlockByteLength(), Rt21PsiPtoDesc.FIELD_BYTE_LENGTH
        );
        encEngine.init(true, new byte[encEngine.getKeyByteLength()]);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] point = new byte[Rt21PsiPtoDesc.FIELD_BYTE_LENGTH];
        byte[] m = new byte[Rt21PsiPtoDesc.FIELD_BYTE_LENGTH];
        boolean success = false;
        while (!success) {
            // a ← KA.R, m = KA.msg_1(a)
            a = byteMulEcc.randomScalar(secureRandom);
            success = byteMulEcc.baseMul(a, point, m);
        }
        // Server sends m
        List<byte[]> msgPayload = Collections.singletonList(m);
        DataPacketHeader msgHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_INIT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(msgHeader, msgPayload));
        // Server receives OKVS key
        DataPacketHeader okvsKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsKeyPayload = rpc.receive(okvsKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(okvsKeyPayload.size() == okvsKeyNum);
        okvsKey = okvsKeyPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }


    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // server init OKVS
        Gf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createInstance(
            envType, okvsType, clientElementSize, Rt21PsiPtoDesc.FIELD_BIT_LENGTH, okvsKey
        );
        stopWatch.stop();
        long okvsInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, okvsInitTime, "Server inits OKVS");

        // server receives OKVS
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsPayload = rpc.receive(okvsHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(okvsPayload.size() == Gf2eDokvsFactory.getM(envType, okvsType, clientElementSize));
        // Server computes K
        List<byte[]> peqtPayload = generatePeqtPayload(dokvs, okvsPayload.toArray(new byte[0][]));
        DataPacketHeader peqtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(peqtHeader, peqtPayload));
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generatePeqtPayload(Gf2eDokvs<ByteBuffer> dokvs, byte[][] storage) {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> serverPeqtList = serverElementStream
            .map(element -> {
                byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                //  P(H1(x))
                byte[] ph1x = dokvs.decode(storage, ByteBuffer.wrap(h1.digestToBytes(elementByteArray)));
                // prp(P(H1(x)))
                byte[] prpPh1x = encEngine.doFinal(ph1x);
                // KA.key(a, prp(P(H1(x)))
                byte[] ki = byteMulEcc.uniformMul(prpPh1x, a);
                // K = H2(x, KA.key(a, prp(P(H1(x))))
                byte[] h2Input = ByteBuffer.allocate(ki.length + elementByteArray.length)
                    .put(ki)
                    .put(elementByteArray)
                    .array();
                return h2.getBytes(h2Input);
            })
            .collect(Collectors.toList());
        Collections.shuffle(serverPeqtList, secureRandom);
        // create filter
        Filter<byte[]> peqtFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        serverPeqtList.forEach(peqtFilter::put);
        return peqtFilter.save();
    }
}
