package edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.filter.Filter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.Kkrt16PsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * KKRT16-PSI服务端。
 *
 * @author Weiran Liu
 * @date 2022/9/20
 */
public class Kkrt16PsiServer<T> extends AbstractPsiServer<T> {
    /**
     * OPRF发送方
     */
    private final OprfSender oprfSender;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * 过滤器类型
     */
    private final FilterType filterType;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * 布谷鸟哈希桶所用的哈希函数
     */
    private Prf[] binHashes;
    /**
     * 布谷鸟哈希桶个数
     */
    private int binNum;
    /**
     * OPRF发送方输出
     */
    private OprfSenderOutput oprfSenderOutput;

    public Kkrt16PsiServer(Rpc serverRpc, Party clientParty, Kkrt16PsiConfig config) {
        super(Kkrt16PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        oprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getOprfConfig());
        oprfSender.addLogLevel();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        filterType = config.getFilterType();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        oprfSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        oprfSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        oprfSender.addLogLevel();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxOprfBatchNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize)
            + CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxClientElementSize);
        oprfSender.init(maxOprfBatchNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        int stashSize = CuckooHashBinFactory.getStashSize(cuckooHashBinType, clientElementSize);
        // 接收布谷鸟哈希密钥
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        stopWatch.start();
        oprfSenderOutput = oprfSender.oprf(binNum + stashSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // 发送服务端哈希桶PRF过滤器
        for (int hashIndex = 0; hashIndex < cuckooHashNum; hashIndex++) {
            List<byte[]> serverBinPrfPayload = generateBinPrfPayload(hashIndex);
            DataPacketHeader serverBinPrfHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BIN_PRFS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(serverBinPrfHeader, serverBinPrfPayload));
            extraInfo++;
        }
        // 发送服务端贮存区PRF过滤器
        for (int stashIndex = 0; stashIndex < stashSize; stashIndex++) {
            List<byte[]> serverStashPrfPayload = generateStashPrfPayload(stashIndex);
            DataPacketHeader serverStashPrfHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_STASH_PRFS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(serverStashPrfHeader, serverStashPrfPayload));
            extraInfo++;
        }
        oprfSenderOutput = null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), serverPrfTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashKeyPayload.size() == cuckooHashNum);
        binHashes = cuckooHashKeyPayload.stream()
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
    }

    private List<byte[]> generateBinPrfPayload(int hashIndex) {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> binPrfList = serverElementStream
            .map(element -> {
                byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                int keyIndex = binHashes[hashIndex].getInteger(elementByteArray, binNum);
                // OPRF(x || hashIndex)
                byte[] extendElementByteArray = ByteBuffer.allocate(elementByteArray.length + Integer.BYTES)
                    .put(elementByteArray)
                    .putInt(hashIndex)
                    .array();
                byte[] binPrf = oprfSenderOutput.getPrf(keyIndex, extendElementByteArray);
                return peqtHash.digestToBytes(binPrf);
            })
            .collect(Collectors.toList());
        Collections.shuffle(binPrfList, secureRandom);
        // 构建过滤器
        Filter<byte[]> binPrfFilter = FilterFactory.createFilter(envType, filterType, serverElementSize, secureRandom);
        binPrfList.forEach(binPrfFilter::put);
        return binPrfFilter.toByteArrayList();
    }

    private List<byte[]> generateStashPrfPayload(int stashIndex) {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> serverStashPrfList = serverElementStream
            .map(element -> {
                byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                int keyIndex = binNum + stashIndex;
                byte[] stashPrf = oprfSenderOutput.getPrf(keyIndex, elementByteArray);
                return peqtHash.digestToBytes(stashPrf);
            }).collect(Collectors.toList());
        Collections.shuffle(serverStashPrfList, secureRandom);
        // 构建过滤器
        Filter<byte[]> stashPrfFilter = FilterFactory.createFilter(envType, filterType, serverElementSize, secureRandom);
        serverStashPrfList.forEach(stashPrfFilter::put);
        return stashPrfFilter.toByteArrayList();
    }
}
