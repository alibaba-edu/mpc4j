package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20.Prty20PsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PRTY20 semi-honest PSI client.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public class Prty20PsiClient<T> extends AbstractPsiClient<T> {
    /**
     * security model
     */
    private final SecurityModel securityModel;
    /**
     * LOT receiver
     */
    private final LcotReceiver lcotReceiver;
    /**
     * PaXoS type
     */
    private final Gf2eDokvsType paxosType;
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

    public Prty20PsiClient(Rpc clientRpc, Party serverParty, Prty20PsiConfig config) {
        super(Prty20PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        lcotReceiver = LcotFactory.createReceiver(clientRpc, serverParty, config.getLcotConfig());
        addSubPto(lcotReceiver);
        securityModel = config.getSecurityModel();
        paxosType = config.getPaxosType();
        paxosKeyNum = Gf2eDokvsFactory.getHashKeyNum(paxosType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        maxL = Prty20PsiPtoDesc.getMaxL(envType, securityModel, paxosType, maxServerElementSize, maxClientElementSize);
        int maxByteL = CommonUtils.getByteLength(maxL);
        h1 = HashFactory.createInstance(envType, maxByteL);
        int maxM = Gf2eDokvsFactory.getM(envType, paxosType, maxClientElementSize);
        lcotReceiver.init(maxL, maxM);
        stopWatch.stop();
        long initLcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initLcotTime, "Client inits LCOT");

        stopWatch.start();
        paxosKeys = CommonUtils.generateRandomKeys(paxosKeyNum, secureRandom);
        List<byte[]> paxosKeyPayload = Arrays.stream(paxosKeys).collect(Collectors.toList());
        DataPacketHeader paxosKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PAXOS_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(paxosKeyHeader, paxosKeyPayload));
        stopWatch.stop();
        long paxosKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, paxosKeyTime, "Client generates PaXoS key");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        Hash peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        Gf2eDokvs<T> paxosD = Gf2eDokvsFactory.createBinaryInstance(envType, paxosType, clientElementSize, maxL, paxosKeys);
        paxosD.setParallelEncode(parallel);
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, setupTime, "Client inits tools");

        stopWatch.start();
        // The receiver generates a PaXoS D = Encode({(y, H_1(y)) | y ∈ Y}).
        Stream<T> clientElementStream = clientElementSet.stream();
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        Map<T, byte[]> dKeyValueMap = clientElementStream
            .collect(Collectors.toMap(
                y -> y,
                y -> {
                    byte[] yBytes = ObjectUtils.objectToByteArray(y);
                    byte[] h1y = h1.digestToBytes(yBytes);
                    BytesUtils.reduceByteArray(h1y, maxL);
                    return h1y;
                }
            ));
        byte[][] d = paxosD.encode(dKeyValueMap, false);
        LcotReceiverOutput lcotReceiverOutput = lcotReceiver.receive(d);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, lcotTime, "Client encodes Y and runs LCOT");

        stopWatch.start();
        // The receiver outputs {y ∈ Y | H_2(y, Decode(R, y)) ∈ M}.
        int t = lcotReceiverOutput.getOutputBitLength();
        Gf2eDokvs<T> paxosR = Gf2eDokvsFactory.createBinaryInstance(envType, paxosType, clientElementSize, t, paxosKeys);
        paxosR.setParallelEncode(parallel);
        byte[][] r = lcotReceiverOutput.getRbArray();
        clientElementStream = clientElementSet.stream();
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        Map<ByteBuffer, T> clientH2ElementMap = clientElementStream
            .collect(Collectors.toMap(
                y -> {
                    // Decode(R, y)
                    byte[] decode = paxosR.decode(r, y);
                    // H_2(y, Decode(R, y))
                    byte[] yBytes = ObjectUtils.objectToByteArray(y);
                    byte[] yDecode = ByteBuffer.allocate(yBytes.length + decode.length)
                        .put(yBytes)
                        .put(decode)
                        .array();
                    return ByteBuffer.wrap(peqtHash.digestToBytes(yDecode));
                },
                y -> y
            ));
        stopWatch.stop();
        long decodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, decodeTime, "Client decodes R");

        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_FILTER.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrfFilterPayload = rpc.receive(serverPrfFilterHeader).getPayload();

        stopWatch.start();
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, serverPrfFilterPayload);
        Set<T> intersection = clientH2ElementMap.entrySet().stream()
            .map(entry -> {
                if (serverPrfFilter.mightContain(entry.getKey().array())) {
                    return entry.getValue();
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, serverPrfTime, "Client computes intersection");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }
}
