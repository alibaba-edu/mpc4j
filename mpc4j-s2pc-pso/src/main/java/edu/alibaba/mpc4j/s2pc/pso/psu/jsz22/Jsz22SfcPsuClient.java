package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

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
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JSZ22-SFC-PSU协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/03/18
 */
public class Jsz22SfcPsuClient extends AbstractPsuClient {
    /**
     * OSN发送方
     */
    private final OsnSender osnSender;
    /**
     * OPRF接收方
     */
    private final OprfReceiver oprfReceiver;
    /**
     * 核COT接收方
     */
    private final CoreCotReceiver coreCotReceiver;
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
     * 布谷鸟哈希
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * OPRF输出映射
     */
    private Hash oprfOutputMap;
    /**
     * 客户端OPRF集合
     */
    private Set<ByteBuffer> clientOprfSet;

    public Jsz22SfcPsuClient(Rpc clientRpc, Party serverParty, Jsz22SfcPsuConfig config) {
        super(Jsz22SfcPsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        osnSender = OsnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        osnSender.addLogLevel();
        oprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getOprfConfig());
        oprfReceiver.addLogLevel();
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        crhf = CrhfFactory.createInstance(getEnvType(), CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        osnSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        oprfReceiver.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
        coreCotReceiver.setTaskId(taskIdPrf.getLong(3, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        osnSender.setParallel(parallel);
        oprfReceiver.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        osnSender.addLogLevel();
        oprfReceiver.addLogLevel();
        coreCotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        // 初始化各个子协议
        osnSender.init(maxBinNum);
        oprfReceiver.init(maxBinNum);
        coreCotReceiver.init(maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Set<ByteBuffer> psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        // 初始化OPRF哈希
        int oprfOutputByteLength = Jsz22SfcPsuPtoDesc.getOprfByteLength(binNum);
        oprfOutputMap = HashFactory.createInstance(getEnvType(), oprfOutputByteLength);
        // R inserts set Y into the Cuckoo hash table, and adds a dummy item d in each empty bin.
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashTime);

        stopWatch.start();
        // 构建客户端元素向量(y_1, ..., y_m)
        Vector<byte[]> yVector = IntStream.range(0, binNum)
            .mapToObj(binIndex -> cuckooHashBin.getHashBinEntry(binIndex).getItemByteArray())
            .collect(Collectors.toCollection(Vector::new));
        cuckooHashBin = null;
        // S and R invoke the ideal functionality F_{PS}.
        // R acts as P_0 with input set Y_C, obtains the shuffled shares {a_1, a_2, ... , a_b}.
        OsnPartyOutput osnSenderOutput = osnSender.osn(yVector, elementByteLength);
        byte[][] aArray = IntStream.range(0, binNum)
            .mapToObj(osnSenderOutput::getShare)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), osnTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{mpOPRF}
        // R acts as P_0 with her shuffled shares {a_i}_{i ∈ [b]}, and obtains the outputs {F(k, a_i)}_{i ∈ [b]};
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(aArray);
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallel ? binIndexIntStream.parallel() : binIndexIntStream;
        clientOprfSet = binIndexIntStream
            .mapToObj(oprfReceiverOutput::getPrf)
            .map(oprf -> oprfOutputMap.digestToBytes(oprf))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // R checks if {F(k, a_j)}_{j ∈ [b]} ∩ I_i \neq ∅. If so, R sets b_i = 1, otherwise, sets bi = 0;
        DataPacketHeader serverOprfHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_OPRFS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverOprfPayload = rpc.receive(serverOprfHeader).getPayload();
        boolean[] choiceArray = handleServerOprfPayload(serverOprfPayload);
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), checkTime);

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choiceArray);
        DataPacketHeader encHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> encPayload = rpc.receive(encHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encPayload.size() == serverElementSize);
        ArrayList<byte[]> encArrayList = new ArrayList<>(encPayload);
        // Y \cup Z
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream decIntStream = IntStream.range(0, serverElementSize);
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
        info("{}{} Client Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return union;
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
                    envType, cuckooHashBinType, clientElementSize, cuckooHashKeys
                );
                // 将客户端消息插入到CuckooHash中
                cuckooHashBin.insertItems(clientElementArrayList);
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

    private boolean[] handleServerOprfPayload(List<byte[]> serverOprfPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverOprfPayload.size() == serverElementSize * cuckooHashNum);
        byte[][] serverFlattenOprfs = serverOprfPayload.toArray(new byte[0][]);
        IntStream serverElementIndexStream = IntStream.range(0, serverElementSize);
        serverElementIndexStream = parallel ? serverElementIndexStream.parallel() : serverElementIndexStream;
        boolean[] choiceArray = new boolean[serverElementSize];
        serverElementIndexStream.forEach(index -> {
            for (int hashIndex = 0; hashIndex < cuckooHashNum; hashIndex++) {
                if (clientOprfSet.contains(ByteBuffer.wrap(serverFlattenOprfs[index * cuckooHashNum + hashIndex]))) {
                    choiceArray[index] = true;
                    break;
                }
            }
        });
        clientOprfSet = null;
        return choiceArray;
    }
}
