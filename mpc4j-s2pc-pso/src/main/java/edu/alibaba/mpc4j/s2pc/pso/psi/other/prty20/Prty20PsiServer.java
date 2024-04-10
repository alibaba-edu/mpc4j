package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20.Prty20PsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PRTY20 semi-honest PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public class Prty20PsiServer<T> extends AbstractPsiServer<T> {
    /**
     * security model
     */
    private final SecurityModel securityModel;
    /**
     * LOT sender
     */
    private final LcotSender lcotSender;
    /**
     * PaXoS type
     */
    private final Gf2eDokvsType paxosType;
    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * PaXoS key num
     */
    private final int paxosKeyNum;
    /**
     * max L
     */
    private int maxL;
    /**
     * H_1: {0,1}^* → {0,1}^{l_1}
     */
    private Hash h1;
    /**
     * PaXoS keys
     */
    private byte[][] paxosKeys;

    public Prty20PsiServer(Rpc serverRpc, Party clientParty, Prty20PsiConfig config) {
        super(Prty20PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        lcotSender = LcotFactory.createSender(serverRpc, clientParty, config.getLcotConfig());
        addSubPto(lcotSender);
        securityModel = config.getSecurityModel();
        paxosType = config.getPaxosType();
        paxosKeyNum = Gf2eDokvsFactory.getHashKeyNum(paxosType);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        maxL = Prty20PsiPtoDesc.getMaxL(envType, securityModel, paxosType, maxServerElementSize, maxClientElementSize);
        int maxByteL = CommonUtils.getByteLength(maxL);
        h1 = HashFactory.createInstance(envType, maxByteL);
        int maxM = Gf2eDokvsFactory.getM(envType, paxosType, maxClientElementSize);
        lcotSender.init(maxL, maxM);
        stopWatch.stop();
        long initLcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initLcotTime, "Server inits LCOT");

        DataPacketHeader paxosKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PAXOS_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> paxosKeyPayload = rpc.receive(paxosKeyHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(paxosKeyPayload.size() == paxosKeyNum);
        paxosKeys = paxosKeyPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long paxosKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, paxosKeyTime, "Server handles PaXoS key");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        Hash peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        int m = Gf2eDokvsFactory.getM(envType, paxosType, clientElementSize);
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, setupTime, "Server inits tools");

        stopWatch.start();
        // The parties run the OOS functionality, the server uses a random string s as input.
        // The sender obtains output strings Q = (q_1, ..., q_m).
        LcotSenderOutput lcotSenderOutput = lcotSender.send(m);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Server runs LCOT");

        stopWatch.start();
        // The sender computes and sends the set M = {H_2(x, Decode(Q, x) ⊕ C(H_1(x)) ☉ s) | x ∈ X}
        byte[] s = lcotSenderOutput.getDelta();
        int t = lcotSenderOutput.getOutputBitLength();
        LinearCoder linearCoder = lcotSenderOutput.getLinearCoder();
        byte[][] q = lcotSenderOutput.getQsArray();
        Gf2eDokvs<T> paxosQ = Gf2eDokvsFactory.createBinaryInstance(envType, paxosType, clientElementSize, t, paxosKeys);
        paxosQ.setParallelEncode(parallel);
        IntStream serverElementIntStream = IntStream.range(0, serverElementSize);
        serverElementIntStream = parallel ? serverElementIntStream.parallel() : serverElementIntStream;
        List<byte[]> serverPrfs = serverElementIntStream
            .mapToObj(index -> {
                T x = serverElementArrayList.get(index);
                // H_1(x)
                byte[] xBytes = ObjectUtils.objectToByteArray(x);
                byte[] hx = h1.digestToBytes(xBytes);
                BytesUtils.reduceByteArray(hx, maxL);
                // C(H_1(x))
                hx = linearCoder.encode(hx);
                // C(H_1(x)) ☉ s
                BytesUtils.andi(hx, s);
                // Decode(Q, x)
                byte[] decode = paxosQ.decode(q, x);
                // Decode(Q, x) ⊕ C(H_1(x)) ☉ s)
                BytesUtils.xori(hx, decode);
                // H_2(x, Decode(Q, x) ⊕ C(H_1(x)) ☉ s)
                byte[] xDecode = ByteBuffer.allocate(xBytes.length + decode.length)
                    .put(xBytes)
                    .put(hx)
                    .array();
                return peqtHash.digestToBytes(xDecode);
            })
            .collect(Collectors.toList());
        // randomly permuted
        Collections.shuffle(serverPrfs, secureRandom);
        // create filter
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        serverPrfs.forEach(serverPrfFilter::put);
        List<byte[]> serverPrfFilterPayload = serverPrfFilter.save();
        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_FILTER.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfFilterHeader, serverPrfFilterPayload));
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Server computes PRFs");

        logPhaseInfo(PtoState.PTO_END);
    }
}
