package edu.alibaba.mpc4j.s2pc.pso.psica.gmr21;

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
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.pso.psica.AbstractPsiCaClient;

import edu.alibaba.mpc4j.s2pc.pso.psica.gmr21.Gmr21PsiCaPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21-PSICA协议客户端。
 *
 * @author Weiran Liu, Liqiang Peng
 * @date 2022/02/15
 */
public class Gmr21PsiCaClient<T> extends AbstractPsiCaClient<T> {
    /**
     * 布谷鸟哈希所用OPRF发送方
     */
    private final OprfSender cuckooHashOprfSender;
    /**
     * OSN发送方
     */
    private final OsnSender osnSender;
    /**
     * PEQT协议所用OPRF接收方
     */
    private final OprfReceiver peqtOprfReceiver;
    /**
     * OKVS类型
     */
    private final Gf2eDokvsType okvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * 多项式有限域哈希
     */
    private Hash finiteFieldHash;
    /**
     * OKVS密钥
     */
    private byte[][] okvsHashKeys;
    /**
     * 布谷鸟哈希
     */
    private Prf[] binHashes;
    /**
     * 桶数量
     */
    private int binNum;
    /**
     * 向量(s_1, ..., s_m)
     */
    private Vector<byte[]> sVector;

    public Gmr21PsiCaClient(Rpc clientRpc, Party serverParty, Gmr21PsiCaConfig config) {
        super(Gmr21PsiCaPtoDesc.getInstance(), clientRpc, serverParty, config);
        cuckooHashOprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getCuckooHashOprfConfig());
        addSubPto(cuckooHashOprfSender);
        osnSender = OsnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        addSubPto(osnSender);
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
        // note that in PSU, we must use no-stash cucko hash
        int maxPrfNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType) * maxClientElementSize;
        // 初始化各个子协议
        cuckooHashOprfSender.init(maxBinNum, maxPrfNum);
        osnSender.init(maxBinNum);
        peqtOprfReceiver.init(maxBinNum);
        // 初始化多项式有限域哈希，根据论文实现，固定为64比特
        finiteFieldHash = HashFactory.createInstance(envType, Gmr21PsiCaPtoDesc.FINITE_FIELD_BYTE_LENGTH);
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
    public int psiCardinality(Set<T> clientElementSet, int serverElementSize)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 设置最大桶数量
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        // 初始化PEQT哈希
        Hash peqtHash = HashFactory.createInstance(envType, Gmr21PsiCaPtoDesc.getPeqtByteLength(binNum));
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, cuckooHashTime);

        stopWatch.start();
        OprfSenderOutput cuckooHashOprfSenderOutput = cuckooHashOprfSender.oprf(binNum);
        stopWatch.stop();
        long cuckooHashOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, cuckooHashOprfTime);

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
        logStepInfo(PtoState.PTO_STEP, 3, 5, okvsTime);

        stopWatch.start();
        OsnPartyOutput osnSenderOutput = osnSender.osn(sVector, Gmr21PsiCaPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        IntStream bOprfIntStream = IntStream.range(0, binNum);
        bOprfIntStream = parallel ? bOprfIntStream.parallel() : bOprfIntStream;
        byte[][] bArray = bOprfIntStream.mapToObj(osnSenderOutput::getShare).toArray(byte[][]::new);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, osnTime);

        stopWatch.start();
        // 以s为输入调用OPRF
        OprfReceiverOutput peqtOprfReceiverOutput = peqtOprfReceiver.oprf(bArray);
        IntStream bPrimeOprfIntStream = IntStream.range(0, binNum);
        bPrimeOprfIntStream = parallel ? bPrimeOprfIntStream.parallel() : bPrimeOprfIntStream;
        ByteBuffer[] bPrimeOprfs = bPrimeOprfIntStream
            .mapToObj(peqtOprfReceiverOutput::getPrf)
            .map(peqtHash::digestToBytes)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // 接收aPrimeOprf
        DataPacketHeader aPrimeOprfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_A_PRIME_OPRFS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> aPrimeOprfPayload = rpc.receive(aPrimeOprfHeader).getPayload();
        MpcAbortPreconditions.checkArgument(aPrimeOprfPayload.size() == binNum);
        ByteBuffer[] aPrimeOprfs = aPrimeOprfPayload.stream()
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // 对比并得到结果
        boolean[] choiceArray = new boolean[binNum];
        IntStream.range(0, binNum).forEach(binIndex ->
            choiceArray[binIndex] = bPrimeOprfs[binIndex].equals(aPrimeOprfs[binIndex])
        );
        int card = (int) IntStream.range(0, choiceArray.length).filter(i -> choiceArray[i]).count();
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
        return card;
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) {
        // 读取哈希函数种子
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
                byte[] si = new byte[Gmr21PsiCaPtoDesc.FINITE_FIELD_BYTE_LENGTH];
                secureRandom.nextBytes(si);
                return si;
            })
            .collect(Collectors.toCollection(Vector::new));
        // 计算OKVS键值
        Vector<byte[][]> keyArrayVector = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> clientElementArrayList.stream()
                .map(ObjectUtils::objectToByteArray)
                .map(receiverElement -> {
                    ByteBuffer extendEntryByteBuffer = ByteBuffer.allocate(receiverElement.length + Integer.BYTES);
                    // y || i
                    extendEntryByteBuffer.put(receiverElement);
                    extendEntryByteBuffer.putInt(hashIndex);
                    return extendEntryByteBuffer.array();
                })
                .toArray(byte[][]::new)
            ).collect(Collectors.toCollection(Vector::new));
        // Bob interpolates a polynomial P of degree < 3n such that for every y ∈ Y and i ∈ {1, 2, 3}, we have
        // P(y || i) = s_{h_i(y)} ⊕ PRF(k_{h_i(y)}, y || i)
        byte[][] valueArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                // 计算OPRF有密码学运算，并发处理
                IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
                return clientElementIntStream
                    .mapToObj(clientElementIndex -> {
                        byte[] clientElement = ObjectUtils.objectToByteArray(clientElementArrayList.get(clientElementIndex));
                        byte[] extendBytes = keyArrayVector.elementAt(hashIndex)[clientElementIndex];
                        int binIndex = binHashes[hashIndex].getInteger(clientElement, binNum);
                        byte[] oprf = cuckooHashOprfSenderOutput.getPrf(binIndex, extendBytes);
                        byte[] value = finiteFieldHash.digestToBytes(oprf);
                        BytesUtils.xori(value, sVector.elementAt(binIndex));
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
            Gmr21PsiCaPtoDesc.FINITE_FIELD_BYTE_LENGTH * Byte.SIZE, okvsHashKeys
        );
        // OKVS编码可以并行处理
        okvs.setParallelEncode(parallel);
        return Arrays.stream(okvs.encode(keyValueMap, false)).collect(Collectors.toList());
    }
}
