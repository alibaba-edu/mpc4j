package edu.alibaba.mpc4j.work.dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdp;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpFactory;
import edu.alibaba.mpc4j.work.dpsi.AbstractDpsiServer;
import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * DPSI based on mqRPMT server.
 *
 * @author Yufei Wang, Weiran Liu
 * @date 2023/9/19
 */
public class MqRpmtDpsiServer<T> extends AbstractDpsiServer<T> {
    /**
     * mqRPMT
     */
    private final MqRpmtClient mqRpmtClient;
    /**
     * binary LDP
     */
    private final BinaryLdp binaryLdp;
    /**
     * max PSI-CA dummy size
     */
    private final int maxPsicaDummySize;
    /**
     * max PSD-CA dummy size
     */
    private final int maxPsdcaDummySize;
    /**
     * element hash
     */
    private final Hash elementHash;

    public MqRpmtDpsiServer(Rpc serverRpc, Party clientParty, MqRpmtDpsiConfig config) {
        super(MqRpmtDpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        mqRpmtClient = MqRpmtFactory.createClient(serverRpc, clientParty, config.getMqRpmtConfig());
        addSubPto(mqRpmtClient);
        binaryLdp = BinaryLdpFactory.createInstance(config.getBinaryLdpConfig());
        maxPsicaDummySize = config.getMaxPsicaDummySize();
        maxPsdcaDummySize = config.getMaxPsdcaDummySize();
        elementHash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
    }


    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mqRpmtClient.init(
            maxServerElementSize + maxPsicaDummySize,
            maxClientElementSize + maxPsicaDummySize + maxPsdcaDummySize
        );
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
        // server adds dummy elements
        Map<ByteBuffer, T> dummyServerElementMap = createDummyServerElementSet();
        stopWatch.stop();
        long serverDummyElementTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, serverDummyElementTime);

        DataPacketHeader clientDummyElementSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientDummyElementSizePayload = rpc.receive(clientDummyElementSizeHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(clientDummyElementSizePayload.size() == 1);
        int dummyClientElementSize = IntUtils.byteArrayToInt(clientDummyElementSizePayload.get(0));
        Set<ByteBuffer> dummyServerElementSet = dummyServerElementMap.keySet();
        boolean[] containVector = mqRpmtClient.mqRpmt(dummyServerElementSet, dummyClientElementSize);
        stopWatch.stop();
        long mqRpmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, mqRpmtTime);

        stopWatch.start();
        // Random Response
        int bitNum = containVector.length;
        byte[] containByteVector = BinaryUtils.binaryToRoundByteArray(containVector);
        BitVector actualServerVector = BitVectorFactory.create(bitNum, containByteVector);
        BitVector randomServerVector = BitVectorFactory.createZeros(bitNum);
        IntStream.range(0, bitNum).forEach(index ->
            randomServerVector.set(index, binaryLdp.randomize(actualServerVector.get(index)))
        );
        List<byte[]> randomizedServerVectorPayload = Collections.singletonList(randomServerVector.getBytes());
        DataPacketHeader randomizedServerVectorHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RANDOMIZED_VECTOR.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomizedServerVectorHeader, randomizedServerVectorPayload));
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, ldpTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    public Map<ByteBuffer, T> createDummyServerElementSet() {
        Map<ByteBuffer, T> dummyServerElementMap = new HashMap<>(serverElementSize + maxPsicaDummySize);
        // add real element into the map
        serverElementArrayList.forEach(element -> {
            byte[] hashElement = elementHash.digestToBytes(ObjectUtils.objectToByteArray(element));
            hashElement[0] = (byte) (hashElement[0] & (byte) 0x7F);
            dummyServerElementMap.put(ByteBuffer.wrap(hashElement), element);
        });
        IntStream.range(0, maxPsicaDummySize).forEach(index -> {
            byte[] dummyElement = IntUtils.nonNegIntToFixedByteArray(index, CommonConstants.BLOCK_BYTE_LENGTH);
            dummyElement[0] = (byte) (dummyElement[0] | (byte) 0x80);
            dummyServerElementMap.put(ByteBuffer.wrap(dummyElement), null);
        });
        return dummyServerElementMap;
    }
}

