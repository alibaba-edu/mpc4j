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
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuServer;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JSZ22-SFC-PSU协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/03/14
 */
public class Jsz22SfcPsuServer extends AbstractPsuServer {
    /**
     * OSN接收方
     */
    private final OsnReceiver osnReceiver;
    /**
     * OPRF发送方
     */
    private final OprfSender oprfSender;
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
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
     * OPRF输出字节长度
     */
    private int oprfOutputByteLength;
    /**
     * 桶数量
     */
    private int binNum;
    /**
     * OPRF输出映射
     */
    private Hash oprfOutputMap;
    /**
     * 布谷鸟哈希桶所用的哈希函数
     */
    private Prf[] hashes;
    /**
     * 交换映射
     */
    private int[] inversePi;
    /**
     * a'_1, ..., a'_m
     */
    private byte[][] aPrimeArray;
    /**
     * OPRF发送方输出
     */
    OprfSenderOutput oprfSenderOutput;

    public Jsz22SfcPsuServer(Rpc serverRpc, Party clientParty, Jsz22SfcPsuConfig config) {
        super(Jsz22SfcPsuPtoDesc.getInstance(), serverRpc, clientParty, config);
        osnReceiver = OsnFactory.createReceiver(serverRpc, clientParty, config.getOsnConfig());
        osnReceiver.addLogLevel();
        oprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getOprfConfig());
        oprfSender.addLogLevel();
        coreCotSender = CoreCotFactory.createSender(serverRpc, clientParty, config.getCoreCotConfig());
        coreCotSender.addLogLevel();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        crhf = CrhfFactory.createInstance(getEnvType(), CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        osnReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        oprfSender.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
        coreCotSender.setTaskId(taskIdPrf.getLong(3, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        osnReceiver.setParallel(parallel);
        oprfSender.setParallel(parallel);
        coreCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        osnReceiver.addLogLevel();
        oprfSender.addLogLevel();
        coreCotSender.addLogLevel();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        // 初始化各个子协议
        osnReceiver.init(maxBinNum);
        oprfSender.init(maxBinNum);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void psu(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize, elementByteLength);
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        // 初始化OPRF哈希
        oprfOutputByteLength = Jsz22SfcPsuPtoDesc.getOprfByteLength(binNum);
        oprfOutputMap = HashFactory.createInstance(getEnvType(), oprfOutputByteLength);
        // 构造交换映射
        List<Integer> piList = IntStream.range(0, binNum)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(piList, secureRandom);
        int[] pi = piList.stream().mapToInt(permutation -> permutation).toArray();
        // 构造交换映射的逆映射
        inversePi = new int[binNum];
        for (int index = 0; index < binNum; index++) {
            inversePi[pi[index]] = index;
        }
        // 设置布谷鸟哈希
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{PS}.
        // S acts as P_1 with a permutation π, obtains the shuffled shares {a'_1, a'_2, ... , a'_b}.
        OsnPartyOutput osnReceiverOutput = osnReceiver.osn(pi, elementByteLength);
        aPrimeArray = IntStream.range(0, binNum)
            .mapToObj(osnReceiverOutput::getShare)
            .toArray(byte[][]::new);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), osnTime);

        stopWatch.start();
        // S and R invoke the ideal functionality F_{mpOPRF}
        // S obtains the key k;
        oprfSenderOutput = oprfSender.oprf(binNum);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // For j ∈ [γ], S computes q_j = π^{−1}(h_j(x'_i))
        List<byte[]> serverOprfPayload = generateServerOprfPayload();
        DataPacketHeader serverOprfHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_OPRFS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverOprfHeader, serverOprfPayload));
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), checkTime);

        stopWatch.start();
        // 加密数据
        CotSenderOutput cotSenderOutput = coreCotSender.send(serverElementSize);
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream encIntStream = IntStream.range(0, serverElementSize);
        encIntStream = parallel ? encIntStream.parallel() : encIntStream;
        List<byte[]> encPayload = encIntStream
            .mapToObj(index -> {
                byte[] key = cotSenderOutput.getR0(index);
                key = crhf.hash(key);
                byte[] ciphertext = encPrg.extendToBytes(key);
                BytesUtils.xori(ciphertext, serverElementArrayList.get(index).array());
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
        info("{}{} Server Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), encTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashKeyPayload.size() == cuckooHashNum);
        hashes = cuckooHashKeyPayload.stream()
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
    }

    private List<byte[]> generateServerOprfPayload() {
        Stream<ByteBuffer> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> serverOprfPayload = serverElementStream
            .map(element -> {
                int[] positions = Arrays.stream(hashes)
                    .mapToInt(hash -> hash.getInteger(element.array(), binNum))
                    .distinct()
                    .toArray();
                byte[][] oprfs = new byte[cuckooHashNum][oprfOutputByteLength];
                for (int index = 0; index < positions.length; index++) {
                    // F(k, x′_i ⊕ a′_{q_j})
                    int binIndex = inversePi[positions[index]];
                    byte[] input = BytesUtils.xor(element.array(), aPrimeArray[binIndex]);
                    oprfs[index] = oprfOutputMap.digestToBytes(oprfSenderOutput.getPrf(binIndex, input));
                }
                // r ← {0, 1}^{l_2}
                for (int index = positions.length; index < cuckooHashNum; index++) {
                    secureRandom.nextBytes(oprfs[index]);
                }
                return oprfs;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        oprfSenderOutput = null;
        inversePi = null;
        return serverOprfPayload;
    }
}
