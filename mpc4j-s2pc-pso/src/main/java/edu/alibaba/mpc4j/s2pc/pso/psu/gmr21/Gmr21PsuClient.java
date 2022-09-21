package edu.alibaba.mpc4j.s2pc.pso.psu.gmr21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.Okvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.oprf.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21-PSU协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public class Gmr21PsuClient extends AbstractPsuClient {
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
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * OKVS类型
     */
    private final OkvsType okvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * 抗关联哈希函数
     */
    private final Crhf crhf;
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

    public Gmr21PsuClient(Rpc clientRpc, Party serverParty, Gmr21PsuConfig config) {
        super(Gmr21PsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        cuckooHashOprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getCuckooHashOprfConfig());
        cuckooHashOprfSender.addLogLevel();
        osnSender = OsnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        osnSender.addLogLevel();
        peqtOprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getPeqtOprfConfig());
        peqtOprfReceiver.addLogLevel();
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        okvsType = config.getOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        crhf = CrhfFactory.createInstance(getEnvType(), CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        cuckooHashOprfSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        osnSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        peqtOprfReceiver.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
        coreCotReceiver.setTaskId(taskIdPrf.getLong(3, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        cuckooHashOprfSender.setParallel(parallel);
        osnSender.setParallel(parallel);
        peqtOprfReceiver.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        cuckooHashOprfSender.addLogLevel();
        osnSender.addLogLevel();
        peqtOprfReceiver.addLogLevel();
        coreCotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        // 初始化各个子协议
        cuckooHashOprfSender.init(maxBinNum);
        osnSender.init(maxBinNum);
        peqtOprfReceiver.init(maxBinNum);
        coreCotReceiver.init(maxBinNum);
        // 初始化多项式有限域哈希，根据论文实现，固定为64比特
        finiteFieldHash = HashFactory.createInstance(envType, Gmr21PsuPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        DataPacketHeader keysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        int okvsHashKeyNum = OkvsFactory.getHashNum(okvsType);
        MpcAbortPreconditions.checkArgument(keysPayload.size() == okvsHashKeyNum);
        // 初始化OKVS密钥
        okvsHashKeys = keysPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Set<ByteBuffer> psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 设置最大桶数量
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        // 初始化PEQT哈希
        Hash peqtHash = HashFactory.createInstance(envType, Gmr21PsuPtoDesc.getPeqtByteLength(binNum));
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashTime);

        stopWatch.start();
        OprfSenderOutput cuckooHashOprfSenderOutput = cuckooHashOprfSender.oprf(binNum);
        stopWatch.stop();
        long cuckooHashOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashOprfTime);

        stopWatch.start();
        List<byte[]> okvsPayload = generateOkvsPayload(cuckooHashOprfSenderOutput);
        DataPacketHeader okvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsHeader, okvsPayload));
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 3/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), okvsTime);

        stopWatch.start();
        OsnPartyOutput osnSenderOutput = osnSender.osn(sVector, Gmr21PsuPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        IntStream bOprfIntStream = IntStream.range(0, binNum);
        bOprfIntStream = parallel ? bOprfIntStream.parallel() : bOprfIntStream;
        byte[][] bArray = bOprfIntStream.mapToObj(osnSenderOutput::getShare).toArray(byte[][]::new);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 4/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), osnTime);

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
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_A_PRIME_OPRFS.ordinal(), extraInfo,
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
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 5/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), peqtTime);

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choiceArray);
        DataPacketHeader encHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> encPayload = rpc.receive(encHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encPayload.size() == binNum);
        ArrayList<byte[]> encArrayList = new ArrayList<>(encPayload);
        // Y \cup Z
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream decIntStream = IntStream.range(0, binNum);
        decIntStream = parallel ? decIntStream.parallel() : decIntStream;
        Set<ByteBuffer> union = decIntStream
            .mapToObj(index -> {
                if (choiceArray[index]) {
                    return botElementByteBuffer;
                } else {
                    byte[] key = cotReceiverOutput.getRb(index);
                    key = crhf.hash(key);
                    byte[] message = encPrg.extendToBytes(key);
                    BytesUtils.xori(message, encArrayList.get(index));
                    return ByteBuffer.wrap(message);
                }
            })
            .collect(Collectors.toSet());
        union.addAll(clientElementSet);
        union.remove(botElementByteBuffer);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 6/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return union;
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
                byte[] si = new byte[Gmr21PsuPtoDesc.FINITE_FIELD_BYTE_LENGTH];
                secureRandom.nextBytes(si);
                return si;
            })
            .collect(Collectors.toCollection(Vector::new));
        // 计算OKVS键值
        Vector<byte[][]> keyArrayVector = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> clientElementArrayList.stream()
                .map(receiverElement -> {
                    byte[] entryBytes = receiverElement.array();
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
                // 计算OPRF有密码学运算，并发处理
                IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
                return clientElementIntStream
                    .mapToObj(clientElementIndex -> {
                        byte[] clientElement = clientElementArrayList.get(clientElementIndex).array();
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
        ByteBuffer[] hashKeyArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                // 计算OPRF有密码学运算，并发处理
                IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
                return clientElementIntStream
                    .mapToObj(clientElementIndex -> {
                        byte[] extendBytes = keyArrayVector.elementAt(hashIndex)[clientElementIndex];
                        return finiteFieldHash.digestToBytes(extendBytes);
                    })
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        Map<ByteBuffer, byte[]> keyValueMap = IntStream.range(0, cuckooHashNum * clientElementSize)
            .boxed()
            .collect(Collectors.toMap(
                index -> hashKeyArray[index],
                index -> valueArray[index]
            ));
        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(
            envType, okvsType, cuckooHashNum * clientElementSize,
            Gmr21PsuPtoDesc.FINITE_FIELD_BYTE_LENGTH * Byte.SIZE, okvsHashKeys
        );
        // OKVS编码可以并行处理
        okvs.setParallelEncode(parallel);
        return Arrays.stream(okvs.encode(keyValueMap)).collect(Collectors.toList());
    }
}
