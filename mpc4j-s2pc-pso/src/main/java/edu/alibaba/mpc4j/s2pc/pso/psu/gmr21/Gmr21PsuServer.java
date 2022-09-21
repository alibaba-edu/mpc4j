package edu.alibaba.mpc4j.s2pc.pso.psu.gmr21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.okve.okvs.Okvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.*;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuServer;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21-PSU协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public class Gmr21PsuServer extends AbstractPsuServer {
    /**
     * 布谷鸟哈希所用OPRF接收方
     */
    private final OprfReceiver cuckooHashOprfReceiver;
    /**
     * OSN接收方
     */
    private final OsnReceiver osnReceiver;
    /**
     * PEQT协议所用OPRF发送方
     */
    private final OprfSender peqtOprfSender;
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
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
     * 无贮存区布谷鸟哈希
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * 桶数量
     */
    private int binNum;
    /**
     * OKVS大小
     */
    private int okvsM;
    /**
     * 交换映射
     */
    private int[] permutationMap;
    /**
     * 扩展元素字节
     */
    private byte[][] extendEntryBytes;
    /**
     * f_1, ..., f_m
     */
    private byte[][] fArray;
    /**
     * 向量(t_1, ..., t_m)
     */
    private Vector<byte[]> tVector;
    /**
     * a'_1, ..., a'_m
     */
    private byte[][] aPrimeArray;

    public Gmr21PsuServer(Rpc serverRpc, Party clientParty, Gmr21PsuConfig config) {
        super(Gmr21PsuPtoDesc.getInstance(), serverRpc, clientParty, config);
        cuckooHashOprfReceiver = OprfFactory.createOprfReceiver(serverRpc, clientParty, config.getCuckooHashOprfConfig());
        cuckooHashOprfReceiver.addLogLevel();
        osnReceiver = OsnFactory.createReceiver(serverRpc, clientParty, config.getOsnConfig());
        osnReceiver.addLogLevel();
        peqtOprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getPeqtOprfConfig());
        peqtOprfSender.addLogLevel();
        coreCotSender = CoreCotFactory.createSender(serverRpc, clientParty, config.getCoreCotConfig());
        coreCotSender.addLogLevel();
        okvsType = config.getOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        crhf = CrhfFactory.createInstance(getEnvType(), CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        cuckooHashOprfReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        osnReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        peqtOprfSender.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
        coreCotSender.setTaskId(taskIdPrf.getLong(3, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        cuckooHashOprfReceiver.setParallel(parallel);
        osnReceiver.setParallel(parallel);
        peqtOprfSender.setParallel(parallel);
        coreCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        cuckooHashOprfReceiver.addLogLevel();
        osnReceiver.addLogLevel();
        peqtOprfSender.addLogLevel();
        coreCotSender.addLogLevel();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        // 初始化各个子协议
        cuckooHashOprfReceiver.init(maxBinNum);
        osnReceiver.init(maxBinNum);
        peqtOprfSender.init(maxBinNum);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxBinNum);
        // 初始化多项式有限域哈希，根据论文实现，固定为64比特
        finiteFieldHash = HashFactory.createInstance(envType, Gmr21PsuPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        List<byte[]> keysPayload = new LinkedList<>();
        // 初始化OKVS密钥
        int okvsHashKeyNum = OkvsFactory.getHashNum(okvsType);
        okvsHashKeys = IntStream.range(0, okvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                keysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader keysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keysHeader, keysPayload));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void psu(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize, elementByteLength);
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        // 设置OKVS大小
        okvsM = OkvsFactory.getM(okvsType, clientElementSize * cuckooHashNum);
        // 初始化PEQT哈希
        Hash peqtHash = HashFactory.createInstance(envType, Gmr21PsuPtoDesc.getPeqtByteLength(binNum));
        // 构造交换映射
        List<Integer> shufflePermutationList = IntStream.range(0, binNum)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(shufflePermutationList, secureRandom);
        permutationMap = shufflePermutationList.stream().mapToInt(permutation -> permutation).toArray();
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashTime);

        stopWatch.start();
        // 生成服务端元素输入列表，即哈希桶中的元素 = 原始元素 || hashindex，贮存区中的元素 = 原始元素
        generateCuckooHashOprfInput();
        OprfReceiverOutput cuckooHashOprfReceiverOutput = cuckooHashOprfReceiver.oprf(extendEntryBytes);
        IntStream oprfIntStream = IntStream.range(0, binNum);
        oprfIntStream = parallel ? oprfIntStream.parallel() : oprfIntStream;
        fArray = oprfIntStream
            .mapToObj(cuckooHashOprfReceiverOutput::getPrf)
            .map(finiteFieldHash::digestToBytes)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long cuckooHashOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashOprfTime);

        stopWatch.start();
        DataPacketHeader okvsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsPayload = rpc.receive(okvsHeader).getPayload();
        handleOkvsPayload(okvsPayload);
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), okvsTime);

        stopWatch.start();
        OsnPartyOutput osnReceiverOutput = osnReceiver.osn(permutationMap, Gmr21PsuPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        handleOsnReceiverOutput(osnReceiverOutput);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), osnTime);

        stopWatch.start();
        OprfSenderOutput peqtOprfSenderOutput = peqtOprfSender.oprf(binNum);
        IntStream aPrimeOprfIntStream = IntStream.range(0, binNum);
        aPrimeOprfIntStream = parallel ? aPrimeOprfIntStream.parallel() : aPrimeOprfIntStream;
        List<byte[]> aPrimeOprfPayload = aPrimeOprfIntStream
            .mapToObj(aPrimeIndex -> peqtOprfSenderOutput.getPrf(aPrimeIndex, aPrimeArray[aPrimeIndex]))
            .map(peqtHash::digestToBytes)
            .collect(Collectors.toList());
        DataPacketHeader aPrimeOprfHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_A_PRIME_OPRFS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(aPrimeOprfHeader, aPrimeOprfPayload));
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 5/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), peqtTime);

        stopWatch.start();
        // 求并集的时候服务端发给客户端的是交换后的数据顺序
        Vector<Integer> permutedIndexVector = IntStream.range(0, binNum).boxed()
            .collect(Collectors.toCollection(Vector::new));
        permutedIndexVector = BenesNetworkUtils.permutation(permutationMap, permutedIndexVector);
        int[] permutedIndexArray = permutedIndexVector.stream().mapToInt(permutedIndex -> permutedIndex).toArray();
        // 加密数据
        CotSenderOutput cotSenderOutput = coreCotSender.send(binNum);
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream encIntStream = IntStream.range(0, binNum);
        encIntStream = parallel ? encIntStream.parallel() : encIntStream;
        List<byte[]> encPayload = encIntStream
            .mapToObj(index -> {
                int permuteIndex = permutedIndexArray[index];
                byte[] key = cotSenderOutput.getR0(index);
                key = crhf.hash(key);
                byte[] ciphertext = encPrg.extendToBytes(key);
                BytesUtils.xori(ciphertext, cuckooHashBin.getHashBinEntry(permuteIndex).getItem().array());
                return ciphertext;
            })
            .collect(Collectors.toList());
        DataPacketHeader encHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encHeader, encPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 6/6 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), encTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        // 设置布谷鸟哈希，如果发现不能构造成功，则可以重复构造
        boolean success = false;
        byte[][] cuckooHashKeys = null;
        while (!success) {
            try {
                cuckooHashKeys = IntStream.range(0, cuckooHashNum)
                    .mapToObj(hashIndex -> {
                        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        secureRandom.nextBytes(key);
                        return key;
                    })
                    .toArray(byte[][]::new);
                cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
                    envType, cuckooHashBinType, serverElementSize, cuckooHashKeys
                );
                // 将客户端消息插入到CuckooHash中
                cuckooHashBin.insertItems(serverElementArrayList);
                if (cuckooHashBin.itemNumInStash() == 0) {
                    success = true;
                }
            } catch (ArithmeticException ignored) {
                // 如果插入不成功，就重新插入
            }
        }
        // 如果成功，则向布谷鸟哈希的空余位置插入空元素
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        return Arrays.stream(cuckooHashKeys).collect(Collectors.toList());
    }

    private void generateCuckooHashOprfInput() {
        extendEntryBytes = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                HashBinEntry<ByteBuffer> hashBinEntry = cuckooHashBin.getHashBinEntry(binIndex);
                int hashIndex = hashBinEntry.getHashIndex();
                byte[] entryBytes = hashBinEntry.getItemByteArray();
                ByteBuffer extendEntryByteBuffer = ByteBuffer.allocate(entryBytes.length + Integer.BYTES);
                // x || i
                extendEntryByteBuffer.put(entryBytes);
                extendEntryByteBuffer.putInt(hashIndex);
                return extendEntryByteBuffer.array();
            })
            .toArray(byte[][]::new);
    }

    private void handleOkvsPayload(List<byte[]> okvsPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(okvsPayload.size() == okvsM);
        byte[][] storage = okvsPayload.toArray(new byte[0][]);
        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(
            envType, okvsType, clientElementSize * cuckooHashNum,
            Gmr21PsuPtoDesc.FINITE_FIELD_BYTE_LENGTH * Byte.SIZE, okvsHashKeys
        );
        IntStream okvsDecodeIntStream = IntStream.range(0, binNum);
        okvsDecodeIntStream = parallel ? okvsDecodeIntStream.parallel() : okvsDecodeIntStream;
        tVector = okvsDecodeIntStream
            .mapToObj(index -> {
                // 扩展输入
                byte[] extendBytes = extendEntryBytes[index];
                ByteBuffer valueBytes = ByteBuffer.wrap(finiteFieldHash.digestToBytes(extendBytes));
                byte[] pi = okvs.decode(storage, valueBytes);
                byte[] fi = fArray[index];
                BytesUtils.xori(pi, fi);
                return pi;
            })
            .collect(Collectors.toCollection(Vector::new));
        // 清理fArray
        fArray = null;
        extendEntryBytes = null;
    }

    private void handleOsnReceiverOutput(OsnPartyOutput osnReceiverOutput) {
        // 交换ts
        Vector<byte[]> tPiVector = BenesNetworkUtils.permutation(permutationMap, tVector);
        tVector = null;
        aPrimeArray = IntStream.range(0, binNum)
            .mapToObj(index -> {
                byte[] ai = osnReceiverOutput.getShare(index);
                byte[] ti = tPiVector.elementAt(index);
                BytesUtils.xori(ai, ti);
                return ai;
            })
            .toArray(byte[][]::new);
    }
}
