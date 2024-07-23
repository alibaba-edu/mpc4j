package edu.alibaba.mpc4j.work.dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.work.dpsi.AbstractDpsiClient;
import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtServer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DPSI based on mqRPMT client.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/9/19
 */
public class MqRpmtDpsiClient<T> extends AbstractDpsiClient<T> {
    /**
     * mqRPMT
     */
    private final MqRpmtServer mqRpmtServer;
    /**
     * ε_I
     */
    private final double psicaEpsilon;
    /**
     * ε_D
     */
    private final double psdcaEpsilon;
    /**
     * delta
     */
    private final double delta;
    /**
     * max PSI-CA dummy size
     */
    private final int maxPsicaDummySize;
    /**
     * max PSI-DA dummy size
     */
    private final int maxPsdcaDummySize;
    /**
     * element hash
     */
    private final Hash elementHash;

    public MqRpmtDpsiClient(Rpc clientRpc, Party serverParty, MqRpmtDpsiConfig config) {
        super(MqRpmtDpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        mqRpmtServer = MqRpmtFactory.createServer(clientRpc, serverParty, config.getMqRpmtConfig());
        addSubPto(mqRpmtServer);
        psicaEpsilon = config.getPsicaEpsilon();
        psdcaEpsilon = config.getPsdcaEpsilon();
        delta = config.getDelta();
        maxPsicaDummySize = config.getMaxPsicaDummySize();
        maxPsdcaDummySize = config.getMaxPsdcaDummySize();
        elementHash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mqRpmtServer.init(
            maxClientElementSize + maxPsicaDummySize + maxPsdcaDummySize,
            maxServerElementSize + maxPsicaDummySize
        );
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // client adds dummy elements
        Map<ByteBuffer, T> dummyClientElementMap = createDummyClientElementSet();
        int dummyClientElementSize = dummyClientElementMap.size();
        List<byte[]> clientDummyElementSizePayload = Collections.singletonList(
            IntUtils.intToByteArray(dummyClientElementSize)
        );
        DataPacketHeader clientDummyElementSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientDummyElementSizeHeader, clientDummyElementSizePayload));
        stopWatch.stop();
        long clientDummyElementTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, clientDummyElementTime);

        stopWatch.start();
        Set<ByteBuffer> dummyClientElementSet = dummyClientElementMap.keySet();
        ByteBuffer[] clientVector = mqRpmtServer.mqRpmt(
            dummyClientElementSet, serverElementSize + maxPsicaDummySize
        );
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, mqRpmtTime);

        DataPacketHeader randomizedServerVectorHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RANDOMIZED_VECTOR.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomizedServerVectorPayload = rpc.receive(randomizedServerVectorHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(randomizedServerVectorPayload.size() == 1);
        int bitNum = clientVector.length;
        BitVector serverContainVector = BitVectorFactory.create(bitNum, randomizedServerVectorPayload.get(0));
        Set<T> intersection = IntStream.range(0, bitNum)
            .mapToObj(i -> {
                if (serverContainVector.get(i)) {
                    return dummyClientElementMap.get(clientVector[i]);
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long intersectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, intersectionTime);

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    public Map<ByteBuffer, T> createDummyClientElementSet() {
        int psicaDummySize = MqRpmtDpUtils.randomize(1, psicaEpsilon, delta, 0);
        int psdcaDummySize = MqRpmtDpUtils.randomize(1, psdcaEpsilon, delta, 0);
        Map<ByteBuffer, T> dummyClientElementMap = new HashMap<>(clientElementSize + psicaDummySize + psdcaDummySize);
        clientElementArrayList.forEach(element -> {
            byte[] hashElement = elementHash.digestToBytes(ObjectUtils.objectToByteArray(element));
            hashElement[0] = (byte) (hashElement[0] & (byte) 0x7F);
            dummyClientElementMap.put(ByteBuffer.wrap(hashElement), element);
        });
        // add dummy points for PSI-CA
        IntStream.range(0, psicaDummySize).forEach(index -> {
            byte[] dummyElement = IntUtils.nonNegIntToFixedByteArray(index, CommonConstants.BLOCK_BYTE_LENGTH);
            dummyElement[0] = (byte) (dummyElement[0] | (byte) 0x80);
            dummyClientElementMap.put(ByteBuffer.wrap(dummyElement), null);
        });
        // add dummy points for PSD-CA
        IntStream.range(0, psdcaDummySize).forEach(index -> {
            byte[] dummyElement = IntUtils.nonNegIntToFixedByteArray(index, CommonConstants.BLOCK_BYTE_LENGTH);
            dummyElement[0] = (byte) (dummyElement[0] | (byte) 0x84);
            dummyClientElementMap.put(ByteBuffer.wrap(dummyElement), null);
        });

        return dummyClientElementMap;
    }
}