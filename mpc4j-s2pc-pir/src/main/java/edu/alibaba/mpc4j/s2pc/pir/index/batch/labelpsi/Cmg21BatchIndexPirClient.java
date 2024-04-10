package edu.alibaba.mpc4j.s2pc.pir.index.batch.labelpsi;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirNativeUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import static edu.alibaba.mpc4j.s2pc.pir.index.batch.labelpsi.Cmg21BatchIndexPirPtoDesc.PtoStep;

/**
 * CMG21 batch Index PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Cmg21BatchIndexPirClient extends AbstractBatchIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * CMG21 params
     */
    private Cmg21BatchIndexPirParams params;
    /**
     * encryption params
     */
    private byte[] encryptionParams;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * secret key
     */
    private byte[] secretKey;
    /**
     * partition count
     */
    private int partitionCount;

    public Cmg21BatchIndexPirClient(Rpc clientRpc, Party serverParty, Cmg21BatchIndexPirConfig config) {
        super(Cmg21BatchIndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(serverElementSize, elementBitLength, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // CMG21 params
        if (serverElementSize <= 1 << 12) {
            if (maxRetrievalSize <= 1 << 10) {
                params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_12_CLIENT_LOG_SIZE_10.copy();
            } else {
                params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_12_CLIENT_LOG_SIZE_12.copy();
            }
        } else if (serverElementSize <= 1 << 16) {
            if (maxRetrievalSize <= 1 << 10) {
                params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_16_CLIENT_LOG_SIZE_10.copy();
            } else {
                params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_16_CLIENT_LOG_SIZE_12.copy();
            }
        } else if (serverElementSize <= 1 << 20) {
            params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_12.copy();
        } else if (serverElementSize <= 1 << 22) {
            params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_22_CLIENT_LOG_SIZE_12.copy();
        } else {
            params = Cmg21BatchIndexPirParams.SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_12.copy();
        }
        assert maxRetrievalSize <= params.maxClientElementSize() : "retrieval size is larger than the upper bound.";
        assert serverElementSize <= params.getPlainModulus();

        stopWatch.start();
        encryptionParams = Cmg21KwPirNativeUtils.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits()
        );
        List<byte[]> clientPublicKeyPayload = generateKeyPair();
        DataPacketHeader clientPublicKeyPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeyPayloadHeader, clientPublicKeyPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, initTime);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getCuckooHashNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);

        stopWatch.start();
        partitionCount = generateCompleteHashBin();
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hashTime, "Client computes partition count");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<Integer, byte[]> pir(List<Integer> indexList) throws MpcAbortException {
        setPtoInput(indexList);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // generate query
        IntNoStashCuckooHashBin cuckooHashBin = generateCuckooHashBin();
        List<byte[]> query = generateQuery(cuckooHashBin);
        DataPacketHeader clientQueryDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryDataPacketHeader, query));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, genQueryTime, "Client generates query");

        // receive server response
        List<byte[]> keyResponsePayload = new ArrayList<>();
        if (partitionCount > 1) {
            DataPacketHeader keyResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            keyResponsePayload = rpc.receive(keyResponseHeader).getPayload();
            MpcAbortPreconditions.checkArgument(keyResponsePayload.size() == params.getCiphertextNum() * partitionCount);
        }
        DataPacketHeader valueResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> valueResponsePayload = rpc.receive(valueResponseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(valueResponsePayload.size() % (params.getCiphertextNum() * partitionCount) == 0);

        // decode response
        stopWatch.start();
        Map<Integer, byte[]> retrievalResult = handleResponse(keyResponsePayload, valueResponsePayload, cuckooHashBin);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return retrievalResult;
    }

    /**
     * generate complete hash bins.
     *
     * @return partition count.
     */
    private int generateCompleteHashBin() {
        int[] index = IntStream.range(0, serverElementSize).toArray();
        IntHashBin intHashBin = new SimpleIntHashBin(envType, params.getBinNum(), serverElementSize, hashKeys);
        intHashBin.insertItems(index);
        int maxBinSize = IntStream.range(0, intHashBin.binNum()).map(intHashBin::binSize).max().orElse(0);
        return CommonUtils.getUnitNum(maxBinSize, params.getMaxPartitionSizePerBin());
    }

    /**
     * client generates key pair.
     *
     * @return public keys.
     */
    private List<byte[]> generateKeyPair() {
        List<byte[]> keyPair = Cmg21KwPirNativeUtils.keyGen(encryptionParams);
        assert (keyPair.size() == 3);
        publicKey = keyPair.remove(0);
        secretKey = keyPair.remove(0);
        List<byte[]> publicKeys = new ArrayList<>();
        // add public key
        publicKeys.add(publicKey);
        // add Relin keys
        publicKeys.add(keyPair.remove(0));
        return publicKeys;
    }

    /**
     * generate cuckoo hash bin.
     *
     * @return cuckoo hash bin.
     */
    private IntNoStashCuckooHashBin generateCuckooHashBin() {
        IntNoStashCuckooHashBin cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType, IntCuckooHashBinType.NO_STASH_NAIVE, maxRetrievalSize, params.getBinNum(), hashKeys
        );
        int[] index = IntStream.range(0, retrievalSize).map(indexList::get).toArray();
        cuckooHashBin.insertItems(index);
        return cuckooHashBin;
    }

    /**
     * generate query.
     *
     * @param cuckooHashBin cuckoo hash bin.
     * @return client query.
     */
    private List<byte[]> generateQuery(IntNoStashCuckooHashBin cuckooHashBin) {
        List<long[][]> encodedQueryList = encodeQuery(cuckooHashBin);
        Stream<long[][]> encodedQueryStream = encodedQueryList.stream();
        encodedQueryStream = parallel ? encodedQueryStream.parallel() : encodedQueryStream;
        return encodedQueryStream
            .map(i -> Cmg21KwPirNativeUtils.generateQuery(encryptionParams, publicKey, secretKey, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * encode query.
     *
     * @param cuckooHashBin cuckoo hash bin.
     * @return encoded query.
     */
    private List<long[][]> encodeQuery(IntNoStashCuckooHashBin cuckooHashBin) {
        long[][] items = new long[params.getCiphertextNum()][params.getPolyModulusDegree()];
        for (int i = 0; i < params.getCiphertextNum(); i++) {
            for (int j = 0; j < params.getItemPerCiphertext(); j++) {
                if (cuckooHashBin.getBinHashIndex(i * params.getItemPerCiphertext() + j) >= 0) {
                    items[i][j] = cuckooHashBin.getBinEntry(i * params.getItemPerCiphertext() + j);
                }
            }
        }
        Zp64 zp64 = Zp64Factory.createInstance(envType, params.getPlainModulus());
        IntStream ciphertextStream = IntStream.range(0, params.getCiphertextNum());
        ciphertextStream = parallel ? ciphertextStream.parallel() : ciphertextStream;
        return ciphertextStream
            .mapToObj(i -> PirUtils.computePowers(zp64, items[i], params.getQueryPowers()))
            .collect(Collectors.toList());
    }



    /**
     * handle server response.
     *
     * @param keyResponse   key response.
     * @param valueResponse value response.
     * @param cuckooHashBin cuckoo hash bin.
     * @return retrieval result map.
     */
    private Map<Integer, byte[]> handleResponse(List<byte[]> keyResponse, List<byte[]> valueResponse,
                                                IntNoStashCuckooHashBin cuckooHashBin) {
        List<long[]> decryptedKeyResponse = new ArrayList<>();
        if (partitionCount > 1) {
            Stream<byte[]> keyResponseStream = keyResponse.stream();
            keyResponseStream = parallel ? keyResponseStream.parallel() : keyResponseStream;
            decryptedKeyResponse = keyResponseStream
                .map(i -> Cmg21KwPirNativeUtils.decodeReply(encryptionParams, secretKey, i))
                .collect(Collectors.toCollection(ArrayList::new));
        }
        Stream<byte[]> valueResponseStream = valueResponse.stream();
        valueResponseStream = parallel ? valueResponseStream.parallel() : valueResponseStream;
        List<long[]> decryptedValueResponse = valueResponseStream
            .map(i -> Cmg21KwPirNativeUtils.decodeReply(encryptionParams, secretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        return recoverPirResult(decryptedKeyResponse, decryptedValueResponse, cuckooHashBin);
    }

    /**
     * recover PIR result.
     *
     * @param key           decrypted key response.
     * @param value         decrypted value response.
     * @param cuckooHashBin cuckoo hash bin.
     * @return PIR result.
     */
    private Map<Integer, byte[]> recoverPirResult(List<long[]> key, List<long[]> value,
                                                  IntNoStashCuckooHashBin cuckooHashBin) {
        Map<Integer, byte[]> resultMap = new HashMap<>(retrievalSize);
        int labelPartitionNum = CommonUtils.getUnitNum(
            elementByteLength * Byte.SIZE, PirUtils.getBitLength(params.getPlainModulus()) - 1
        );
        int shiftBits = CommonUtils.getUnitNum(elementByteLength * Byte.SIZE, labelPartitionNum);
        if (partitionCount > 1) {
            for (int i = 0; i < key.size(); i++) {
                List<Integer> matchedItem = new ArrayList<>();
                for (int j = 0; j < params.getItemPerCiphertext(); j++) {
                    if (key.get(i)[j] == 0 && cuckooHashBin.getBinHashIndex(j + (i / partitionCount) * params.getItemPerCiphertext()) >= 0) {
                        matchedItem.add(j);
                    }
                }
                for (Integer integer : matchedItem) {
                    int hashBinIndex = integer + (i / partitionCount) * params.getItemPerCiphertext();
                    BigInteger label = BigInteger.ZERO;
                    int index = 0;
                    for (int l = 0; l < labelPartitionNum; l++) {
                        BigInteger temp = BigInteger.valueOf(value.get(i * labelPartitionNum + l)[integer])
                            .shiftLeft(shiftBits * index);
                        label = label.add(temp);
                        index++;
                    }
                    byte[] plaintextLabel = BigIntegerUtils.nonNegBigIntegerToByteArray(label, elementByteLength);
                    resultMap.put(cuckooHashBin.getBinEntry(hashBinIndex), plaintextLabel);
                }
            }
        } else {
            List<Integer> matchedItem = new ArrayList<>();
            int[] index = new int[retrievalSize];
            int count = 0;
            for (int i = 0; i < cuckooHashBin.binNum(); i++) {
                if (cuckooHashBin.getBinHashIndex(i) >= 0) {
                    index[count] = i;
                    count++;
                }
            }
            for (int j = 0; j < retrievalSize; j++) {
                matchedItem.add(index[j]);
            }
            for (Integer hashBinIndex : matchedItem) {
                BigInteger label = BigInteger.ZERO;
                int s = 0;
                for (int l = 0; l < labelPartitionNum; l++) {
                    int cipherIndex = hashBinIndex / params.getItemPerCiphertext();
                    BigInteger temp = BigInteger.valueOf(
                        value.get(l + cipherIndex * labelPartitionNum)[hashBinIndex % params.getItemPerCiphertext()]
                        ).shiftLeft(shiftBits * s);
                    label = label.add(temp);
                    s++;
                }
                byte[] plaintextLabel = BigIntegerUtils.nonNegBigIntegerToByteArray(label, elementByteLength);
                resultMap.put(cuckooHashBin.getBinEntry(hashBinIndex), plaintextLabel);
            }
        }

        return resultMap;
    }
}