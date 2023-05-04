package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.upso.upsi.AbstractUpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiParams;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CMG21非平衡PSI协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/5/25
 */
public class Cmg21UpsiClient<T> extends AbstractUpsiClient<T> {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * MP-OPRF协议接收方
     */
    private final MpOprfReceiver mpOprfReceiver;
    /**
     * 非平衡PSI方案参数
     */
    private Cmg21UpsiParams params;
    /**
     * 无贮存区布谷鸟哈希分桶
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;

    public Cmg21UpsiClient(Rpc clientRpc, Party serverParty, Cmg21UpsiConfig config) {
        super(Cmg21UpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(clientRpc, serverParty, config.getMpOprfConfig());
        addSubPtos(mpOprfReceiver);
    }

    @Override
    public void init(UpsiParams upsiParams) throws MpcAbortException {
        setInitInput(upsiParams.maxClientElementSize());
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        assert (upsiParams instanceof Cmg21UpsiParams);
        params = (Cmg21UpsiParams) upsiParams;
        mpOprfReceiver.init(params.maxClientElementSize());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_5535;
        mpOprfReceiver.init(params.maxClientElementSize());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet) throws MpcAbortException {
        setPtoInput(clientElementSet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 客户端执行MP-OPRF协议
        ArrayList<ByteBuffer> oprfOutputs = oprf();
        Map<ByteBuffer, ByteBuffer> oprfMap = IntStream.range(0, clientElementSize)
            .boxed()
            .collect(Collectors.toMap(oprfOutputs::get, i -> clientElementArrayList.get(i), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, oprfTime, "OPRF");

        stopWatch.start();
        // 客户端布谷鸟哈希分桶，并发送hash函数的key
        boolean success = false;
        byte[][] hashKeys;
        do {
            hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
            cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
                envType, params.getCuckooHashBinType(), clientElementSize, params.getBinNum(), hashKeys
            );
            cuckooHashBin.insertItems(oprfOutputs);
            if (cuckooHashBin.itemNumInStash() == 0) {
                success = true;
            }
        } while (!success);
        // 向布谷鸟哈希的空余位置插入空元素
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        DataPacketHeader hashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(hashKeyHeader, hashKeyPayload));
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, cuckooHashKeyTime, "Client generates cuckoo hash keys");

        stopWatch.start();
        // 客户端生成BFV算法密钥
        List<byte[]> encryptionParams = Cmg21UpsiNativeUtils.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits()
        );
        List<byte[]> fheParams = encryptionParams.subList(0, 2);
        DataPacketHeader fheParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(fheParamsHeader, fheParams));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, keyGenTime, "Client generates FHE keys");

        stopWatch.start();
        // 客户端加密查询信息
        List<long[][]> encodedQuery = encodeQuery();
        Stream<long[][]> encodeStream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        List<byte[]> queryPayload = encodeStream
            .map(i -> Cmg21UpsiNativeUtils.generateQuery(
                encryptionParams.get(0), encryptionParams.get(2), encryptionParams.get(3), i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryHeader, queryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, genQueryTime, "Client generates query");

        // 客户端接收服务端的计算结果
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        stopWatch.start();
        // 客户端解密密文匹配结果
        Stream<byte[]> responseStream = parallel ? responsePayload.stream().parallel() : responsePayload.stream();
        ArrayList<long[]> decodedResponse = responseStream
            .map(i -> Cmg21UpsiNativeUtils.decodeReply(encryptionParams.get(0), encryptionParams.get(3), i))
            .collect(Collectors.toCollection(ArrayList::new));
        Set<T> intersectionSet = recoverPsiResult(decodedResponse, oprfMap);
        stopWatch.stop();
        long decodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, decodeTime, "Client decodes response");

        logPhaseInfo(PtoState.PTO_END);
        return intersectionSet;
    }

    /**
     * 客户端（接收方）执行MP-OPRF协议。
     *
     * @return MP-OPRF接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<ByteBuffer> oprf() throws MpcAbortException {
        byte[][] oprfReceiverInputs = IntStream.range(0, clientElementSize)
            .mapToObj(i -> clientElementArrayList.get(i).array())
            .toArray(byte[][]::new);
        OprfReceiverOutput oprfReceiverOutput = mpOprfReceiver.oprf(oprfReceiverInputs);
        return IntStream.range(0, clientElementSize)
            .mapToObj(i -> ByteBuffer.wrap(oprfReceiverOutput.getPrf(i)))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 恢复隐私集合交集。
     *
     * @param decryptedResponse 解密后的服务端回复。
     * @param oprfMap           OPRF映射。
     * @return 隐私集合交集。
     */
    private Set<T> recoverPsiResult(ArrayList<long[]> decryptedResponse, Map<ByteBuffer, ByteBuffer> oprfMap) {
        Set<T> intersectionSet = new HashSet<>();
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int partitionCount = decryptedResponse.size() / ciphertextNum;
        for (int i = 0; i < decryptedResponse.size(); i++) {
            // 找到匹配元素的所在行
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < params.getItemEncodedSlotSize() * itemPerCiphertext; j++) {
                if (decryptedResponse.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - params.getItemEncodedSlotSize() + 1; j++) {
                if (matchedItem.get(j) % params.getItemEncodedSlotSize() == 0) {
                    if (matchedItem.get(j + params.getItemEncodedSlotSize() - 1) - matchedItem.get(j)
                        == params.getItemEncodedSlotSize() - 1) {
                        int hashBinIndex = (matchedItem.get(j) / params.getItemEncodedSlotSize()) + (i / partitionCount) * itemPerCiphertext;
                        intersectionSet.add(byteArrayObjectMap.get(
                            oprfMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem())));
                        j = j + params.getItemEncodedSlotSize() - 1;
                    }
                }
            }
        }
        return intersectionSet;
    }

    /**
     * 返回查询信息的编码。
     *
     * @return 查询信息的编码。
     */
    public List<long[][]> encodeQuery() {
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int ciphertextNum = params.getBinNum() / itemPerCiphertext;
        long[][] items = new long[ciphertextNum][params.getPolyModulusDegree()];
        for (int i = 0; i < ciphertextNum; i++) {
            for (int j = 0; j < itemPerCiphertext; j++) {
                long[] item = params.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * itemPerCiphertext + j), true
                );
                System.arraycopy(item, 0, items[i], j * params.getItemEncodedSlotSize(), params.getItemEncodedSlotSize());
            }
            for (int j = itemPerCiphertext * params.getItemEncodedSlotSize(); j < params.getPolyModulusDegree(); j++) {
                items[i][j] = 0;
            }
        }
        return IntStream.range(0, ciphertextNum)
            .mapToObj(i -> computePowers(items[i]))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 计算幂次方。
     *
     * @param base 底数。
     * @return 幂次方。
     */
    private long[][] computePowers(long[] base) {
        Zp64 zp64 = Zp64Factory.createInstance(envType, (long) params.getPlainModulus());
        int[] exponents = params.getQueryPowers();
        assert exponents[0] == 1;
        long[][] result = new long[exponents.length][base.length];
        result[0] = base;
        IntStream intStream = IntStream.range(1, exponents.length);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i ->
                IntStream.range(0, base.length).forEach(j -> result[i][j] = zp64.pow(base[j], exponents[i]))
        );
        return result;
    }
}