package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.AbstractMqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnSender;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21-mqRPMT client.
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
public class Gmr21MqRpmtClient extends AbstractMqRpmtClient {
    /**
     * OPRF used in cuckoo hash
     */
    private final OprfSender cuckooHashOprfSender;
    /**
     * OSN
     */
    private final DosnSender dosnSender;
    /**
     * OPRF used in PEQT
     */
    private final OprfReceiver peqtOprfReceiver;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int cuckooHashNum;
    /**
     * hash for finite field
     */
    private Hash finiteFieldHash;
    /**
     * DOKVS hash keys
     */
    private byte[][] okvsHashKeys;
    /**
     * bin hashes
     */
    private Prf[] binHashes;
    /**
     * bin num
     */
    private int binNum;
    /**
     * (s_1, ..., s_m)
     */
    private byte[][] sVector;

    public Gmr21MqRpmtClient(Rpc clientRpc, Party serverParty, Gmr21MqRpmtConfig config) {
        super(Gmr21MqRpmtPtoDesc.getInstance(), clientRpc, serverParty, config);
        cuckooHashOprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getCuckooHashOprfConfig());
        addSubPto(cuckooHashOprfSender);
        dosnSender = DosnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        addSubPto(dosnSender);
        peqtOprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getPeqtOprfConfig());
        addSubPto(peqtOprfReceiver);
        okvsType = config.getOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        int maxPrfNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType) * maxClientElementSize;
        // 初始化各个子协议
        cuckooHashOprfSender.init(maxBinNum, maxPrfNum);
        dosnSender.init();
        peqtOprfReceiver.init(maxBinNum);
        // 初始化多项式有限域哈希，根据论文实现，固定为64比特
        finiteFieldHash = HashFactory.createInstance(envType, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        int okvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
        MpcAbortPreconditions.checkArgument(keysPayload.size() == okvsHashKeyNum);
        // 初始化OKVS密钥
        okvsHashKeys = keysPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public boolean[] mqRpmt(Set<ByteBuffer> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        Hash peqtHash = HashFactory.createInstance(envType, Gmr21MqRpmtPtoDesc.getPeqtByteLength(binNum));
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, cuckooHashTime, "Client handles cuckoo hash keys");

        stopWatch.start();
        OprfSenderOutput cuckooHashOprfSenderOutput = cuckooHashOprfSender.oprf(binNum);
        stopWatch.stop();
        long cuckooHashOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, cuckooHashOprfTime, "Client runs OPRF for cuckoo hash bins");

        stopWatch.start();
        List<byte[]> okvsPayload = generateOkvsPayload(cuckooHashOprfSenderOutput);
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsHeader, okvsPayload));
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, okvsTime, "Client generates OKVS");

        stopWatch.start();
        DosnPartyOutput osnSenderOutput = dosnSender.dosn(sVector, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        IntStream bOprfIntStream = IntStream.range(0, binNum);
        bOprfIntStream = parallel ? bOprfIntStream.parallel() : bOprfIntStream;
        byte[][] bArray = bOprfIntStream.mapToObj(osnSenderOutput::getShare).toArray(byte[][]::new);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, osnTime, "Client runs OSN");

        stopWatch.start();
        OprfReceiverOutput peqtOprfReceiverOutput = peqtOprfReceiver.oprf(bArray);
        IntStream bPrimeOprfIntStream = IntStream.range(0, binNum);
        bPrimeOprfIntStream = parallel ? bPrimeOprfIntStream.parallel() : bPrimeOprfIntStream;
        ByteBuffer[] bPrimeOprfs = bPrimeOprfIntStream
            .mapToObj(peqtOprfReceiverOutput::getPrf)
            .map(peqtHash::digestToBytes)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // receiver a'
        DataPacketHeader aPrimeOprfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_A_PRIME_OPRFS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> aPrimeOprfPayload = rpc.receive(aPrimeOprfHeader).getPayload();
        MpcAbortPreconditions.checkArgument(aPrimeOprfPayload.size() == binNum);
        ByteBuffer[] aPrimeOprfs = aPrimeOprfPayload.stream()
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // compute the bit vector
        boolean[] containVector = new boolean[binNum];
        IntStream.range(0, binNum).forEach(binIndex ->
            containVector[binIndex] = bPrimeOprfs[binIndex].equals(aPrimeOprfs[binIndex])
        );
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, peqtTime, "Client runs OPRF for PEQT");

        logPhaseInfo(PtoState.PTO_END);
        return containVector;
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) {
        binHashes = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                byte[] key = cuckooHashKeyPayload.remove(0);
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
    }

    private List<byte[]> generateOkvsPayload(OprfSenderOutput cuckooHashOprfSenderOutput) {
        // For each j ∈ [m], Bob choose a random s_j.
        sVector = IntStream.range(0, binNum)
            .mapToObj(index -> {
                byte[] si = new byte[Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH];
                secureRandom.nextBytes(si);
                return si;
            })
            .toArray(byte[][]::new);
        // compute key-value pairs
        Vector<byte[][]> keyArrayVector = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> clientElementArrayList.stream()
                .map(clientElement -> {
                    byte[] entryBytes = clientElement.array();
                    ByteBuffer extendEntryByteBuffer = ByteBuffer.allocate(entryBytes.length + Integer.BYTES);
                    // y || i
                    extendEntryByteBuffer.put(entryBytes);
                    extendEntryByteBuffer.putInt(hashIndex);
                    return extendEntryByteBuffer.array();
                })
                .toArray(byte[][]::new)
            ).collect(Collectors.toCollection(Vector::new));
        // Bob interpolates a polynomial P of degree < 3n such that for every y ∈ Y and i ∈ {1, 2, 3}, we have
        // P(y || i) = s_{h_i(y)} ⊕ PRF(k_{h_i(y)}, y || i)
        byte[][] valueArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
                return clientElementIntStream
                    .mapToObj(clientElementIndex -> {
                        byte[] clientElement = clientElementArrayList.get(clientElementIndex).array();
                        byte[] extendBytes = keyArrayVector.elementAt(hashIndex)[clientElementIndex];
                        int binIndex = binHashes[hashIndex].getInteger(clientElement, binNum);
                        byte[] oprf = cuckooHashOprfSenderOutput.getPrf(binIndex, extendBytes);
                        byte[] value = finiteFieldHash.digestToBytes(oprf);
                        BytesUtils.xori(value, sVector[binIndex]);
                        return value;
                    })
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        ByteBuffer[] keyArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                // 计算OPRF有密码学运算，并发处理
                IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
                return clientElementIntStream
                    .mapToObj(clientElementIndex -> keyArrayVector.elementAt(hashIndex)[clientElementIndex])
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        Map<ByteBuffer, byte[]> keyValueMap = IntStream.range(0, cuckooHashNum * clientElementSize)
            .boxed()
            .collect(Collectors.toMap(
                index -> keyArray[index],
                index -> valueArray[index]
            ));
        Gf2eDokvs<ByteBuffer> okvs = Gf2eDokvsFactory.createInstance(
            envType, okvsType, cuckooHashNum * clientElementSize,
            Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH * Byte.SIZE, okvsHashKeys
        );
        okvs.setParallelEncode(parallel);
        return Arrays.stream(okvs.encode(keyValueMap, false)).collect(Collectors.toList());
    }
}
