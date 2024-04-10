package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipher;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.createCuckooHashBin;

/**
 * CMG21 keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirClient extends AbstractKwPirClient {
    /**
     * stream cipher
     */
    private final StreamCipher streamCipher;
    /**
     * CMG21 keyword PIR params
     */
    private Cmg21KwPirParams params;
    /**
     * β^{-1}
     */
    private BigInteger[] inverseBetas;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * ecc
     */
    private final ByteFullEcc ecc;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * secret key
     */
    private byte[] secretKey;
    /**
     * iv byte length
     */
    private final int ivByteLength;

    public Cmg21KwPirClient(Rpc clientRpc, Party serverParty, Cmg21KwPirConfig config) {
        super(Cmg21KwPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        streamCipher = StreamCipherFactory.createInstance(envType);
        ecc = ByteEccFactory.createFullInstance(envType);
        ivByteLength = 0;
    }

    @Override
    public void init(KwPirParams kwPirParams, int serverElementSize, int maxRetrievalSize, int valueByteLength)
        throws MpcAbortException {
        setInitInput(maxRetrievalSize, serverElementSize, valueByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        assert (kwPirParams instanceof Cmg21KwPirParams);
        params = (Cmg21KwPirParams) kwPirParams;
        assert maxRetrievalSize <= params.maxRetrievalSize();

        stopWatch.start();
        List<byte[]> clientPublicKeyPayload = generateKeyPair(params.getEncryptionParams());
        DataPacketHeader clientPublicKeyPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeyPayloadHeader, clientPublicKeyPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, initTime);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getCuckooHashKeyNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxRetrievalSize, int serverElementSize, int valueByteLength) throws MpcAbortException {
        if (maxRetrievalSize > 1) {
            params = Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096;
        } else {
            if (serverElementSize <= (1 << 20)) {
                params = Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1;
            } else {
                params = Cmg21KwPirParams.SERVER_16M_CLIENT_MAX_1;
            }
        }
        setInitInput(maxRetrievalSize, serverElementSize, valueByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        assert maxRetrievalSize <= params.maxRetrievalSize();

        stopWatch.start();
        List<byte[]> clientPublicKeyPayload = generateKeyPair(params.getEncryptionParams());
        DataPacketHeader clientPublicKeyPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeyPayloadHeader, clientPublicKeyPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, initTime);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getCuckooHashKeyNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<ByteBuffer, byte[]> pir(Set<ByteBuffer> retrievalKeySet) throws MpcAbortException {
        setPtoInput(retrievalKeySet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // run MP-OPRF
        stopWatch.start();
        List<byte[]> blindPayload = generateBlindPayload();
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindHeader, blindPayload));
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
        List<ByteBuffer> keywordPrfs = handleBlindPrf(blindPrfPayload);
        Map<ByteBuffer, ByteBuffer> prfKeyMap = IntStream.range(0, retrievalKeySize)
            .boxed()
            .collect(Collectors.toMap(keywordPrfs::get, i -> retrievalKeyList.get(i), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, oprfTime, "Client executes OPRF");

        stopWatch.start();
        // generate query
        CuckooHashBin<ByteBuffer> cuckooHashBin = generateCuckooHashBin(keywordPrfs, params.getBinNum(), hashKeys);
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

        DataPacketHeader keyResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> keyResponsePayload = rpc.receive(keyResponseHeader).getPayload();
        DataPacketHeader valueResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> valueResponsePayload = rpc.receive(valueResponseHeader).getPayload();

        stopWatch.start();
        Map<ByteBuffer, byte[]> pirResult = handleResponse(
            keyResponsePayload, valueResponsePayload, prfKeyMap, cuckooHashBin
        );
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, decodeResponseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * handle server response.
     *
     * @param keyResponse      key response.
     * @param valueResponse    value response.
     * @param prfKeyMap        prf keyword map.
     * @return retrieval result map.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private Map<ByteBuffer, byte[]> handleResponse(List<byte[]> keyResponse, List<byte[]> valueResponse,
                                                       Map<ByteBuffer, ByteBuffer> prfKeyMap,
                                                       CuckooHashBin<ByteBuffer> cuckooHashBin)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(keyResponse.size() % params.getCiphertextNum() == 0);
        MpcAbortPreconditions.checkArgument(valueResponse.size() % params.getCiphertextNum() == 0);
        Stream<byte[]> keyResponseStream = keyResponse.stream();
        keyResponseStream = parallel ? keyResponseStream.parallel() : keyResponseStream;
        List<long[]> decryptedKeyResponse = keyResponseStream
            .map(i -> Cmg21KwPirNativeUtils.decodeReply(params.getEncryptionParams(), secretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<byte[]> valueResponseStream = valueResponse.stream();
        valueResponseStream = parallel ? valueResponseStream.parallel() : valueResponseStream;
        List<long[]> decryptedValueResponse = valueResponseStream
            .map(i -> Cmg21KwPirNativeUtils.decodeReply(params.getEncryptionParams(), secretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        return recoverPirResult(decryptedKeyResponse, decryptedValueResponse, prfKeyMap, cuckooHashBin);
    }

    /**
     * generate query.
     *
     * @param cuckooHashBin cuckoo hash bin.
     * @return client query.
     */
    private List<byte[]> generateQuery(CuckooHashBin<ByteBuffer> cuckooHashBin) {
        List<long[][]> encodedQueryList = encodeQuery(cuckooHashBin);
        Stream<long[][]> encodedQueryStream = encodedQueryList.stream();
        encodedQueryStream = parallel ? encodedQueryStream.parallel() : encodedQueryStream;
        return encodedQueryStream
            .map(i -> Cmg21KwPirNativeUtils.generateQuery(params.getEncryptionParams(), publicKey, secretKey, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * recover PIR result.
     *
     * @param decryptedKeyReply   decrypted key response.
     * @param decryptedValueReply decrypted value response.
     * @param prfKeyMap           prf key map.
     * @return PIR result.
     */
    private Map<ByteBuffer, byte[]> recoverPirResult(List<long[]> decryptedKeyReply,
                                                         List<long[]> decryptedValueReply,
                                                         Map<ByteBuffer, ByteBuffer> prfKeyMap,
                                                         CuckooHashBin<ByteBuffer> cuckooHashBin) {
        Map<ByteBuffer, byte[]> resultMap = new HashMap<>(retrievalKeySize);
        int itemPartitionNum = decryptedKeyReply.size() / params.getCiphertextNum();
        int labelPartitionNum = CommonUtils.getUnitNum((valueByteLength + ivByteLength) * Byte.SIZE,
            (LongUtils.ceilLog2(params.getPlainModulus()) - 1) * params.getItemEncodedSlotSize());
        int shiftBits = CommonUtils.getUnitNum((valueByteLength + ivByteLength) * Byte.SIZE,
            params.getItemEncodedSlotSize() * labelPartitionNum);
        for (int i = 0; i < decryptedKeyReply.size(); i++) {
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < params.getItemEncodedSlotSize() * params.getItemPerCiphertext(); j++) {
                if (decryptedKeyReply.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - params.getItemEncodedSlotSize() + 1; j++) {
                if (matchedItem.get(j) % params.getItemEncodedSlotSize() == 0) {
                    if (matchedItem.get(j + params.getItemEncodedSlotSize() - 1) - matchedItem.get(j) ==
                        params.getItemEncodedSlotSize() - 1) {
                        int hashBinIndex = matchedItem.get(j) / params.getItemEncodedSlotSize() + (i / itemPartitionNum)
                            * params.getItemPerCiphertext();
                        BigInteger label = BigInteger.ZERO;
                        int index = 0;
                        for (int l = 0; l < labelPartitionNum; l++) {
                            for (int k = 0; k < params.getItemEncodedSlotSize(); k++) {
                                BigInteger temp = BigInteger.valueOf(
                                    decryptedValueReply.get(i * labelPartitionNum + l)[matchedItem.get(j + k)]
                                    ).shiftLeft(shiftBits * index);
                                label = label.add(temp);
                                index++;
                            }
                        }
                        byte[] oprf = cuckooHashBin.getHashBinEntry(hashBinIndex).getItem().array();
                        byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        System.arraycopy(oprf, 0, keyBytes, 0, CommonConstants.BLOCK_BYTE_LENGTH);
                        byte[] ciphertextLabel = BigIntegerUtils.nonNegBigIntegerToByteArray(
                            label, valueByteLength + ivByteLength
                        );
                        byte[] paddingCipher = BytesUtils.paddingByteArray(
                            ciphertextLabel, valueByteLength + CommonConstants.BLOCK_BYTE_LENGTH
                        );
                        byte[] plaintextLabel = streamCipher.ivDecrypt(keyBytes, paddingCipher);
                        resultMap.put(
                            prfKeyMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem()),
                            plaintextLabel
                        );
                        j = j + params.getItemEncodedSlotSize() - 1;
                    }
                }
            }
        }
        return resultMap;
    }

    /**
     * generate cuckoo hash bin.
     *
     * @param items    items.
     * @param binNum   bin num.
     * @param hashKeys hash keys.
     * @return cuckoo hash bin.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private CuckooHashBin<ByteBuffer> generateCuckooHashBin(List<ByteBuffer> items, int binNum, byte[][] hashKeys)
        throws MpcAbortException {
        CuckooHashBin<ByteBuffer> cuckooHashBin = createCuckooHashBin(
            envType, params.getCuckooHashBinType(), retrievalKeySize, binNum, hashKeys
        );
        boolean success = false;
        cuckooHashBin.insertItems(items);
        if (cuckooHashBin.itemNumInStash() == 0) {
            success = true;
        }
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        MpcAbortPreconditions.checkArgument(success, "failed to generate cuckoo hash bin.");
        return cuckooHashBin;
    }

    /**
     * encode query.
     *
     * @param cuckooHashBin cuckoo hash bin.
     * @return encoded query.
     */
    private List<long[][]> encodeQuery(CuckooHashBin<ByteBuffer> cuckooHashBin) {
        long[][] items = new long[params.getCiphertextNum()][params.getPolyModulusDegree()];
        for (int i = 0; i < params.getCiphertextNum(); i++) {
            for (int j = 0; j < params.getItemPerCiphertext(); j++) {
                long[] item = params.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * params.getItemPerCiphertext() + j), true, secureRandom
                );
                System.arraycopy(item, 0, items[i], j * params.getItemEncodedSlotSize(), params.getItemEncodedSlotSize());
            }
            for (int j = params.getItemPerCiphertext() * params.getItemEncodedSlotSize(); j < params.getPolyModulusDegree(); j++) {
                items[i][j] = 0;
            }
        }
        IntStream ciphertextStream = IntStream.range(0, params.getCiphertextNum());
        ciphertextStream = parallel ? ciphertextStream.parallel() : ciphertextStream;
        return ciphertextStream
            .mapToObj(i -> computePowers(items[i], params.getPlainModulus(), params.getQueryPowers()))
            .collect(Collectors.toList());
    }

    /**
     * generate blind elements.
     *
     * @return blind elements.
     */
    private List<byte[]> generateBlindPayload() {
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[retrievalKeySize];
        IntStream intStream = IntStream.range(0, retrievalKeySize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(index -> {
                // generate blind factor
                BigInteger beta = BigIntegerUtils.randomPositive(n, secureRandom);
                inverseBetas[index] = beta.modInverse(n);
                // hash to point
                byte[] element = ecc.hashToCurve(retrievalKeyList.get(index).array());
                // blinding
                return ecc.mul(element, beta);
            })
            .collect(Collectors.toList());
    }

    /**
     * handle blind elements PRF.
     *
     * @param blindPrf blind elements PRF.
     * @return elements PRF.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<ByteBuffer> handleBlindPrf(List<byte[]> blindPrf) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPrf.size() == retrievalKeySize);
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        byte[][] blindPrfArray = blindPrf.toArray(new byte[0][]);
        IntStream intStream = IntStream.range(0, retrievalKeySize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(index -> ecc.mul(blindPrfArray[index], inverseBetas[index]))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * compute powers.
     *
     * @param base      base.
     * @param modulus   modulus.
     * @param exponents exponents.
     * @return powers.
     */
    private long[][] computePowers(long[] base, long modulus, int[] exponents) {
        Zp64 zp64 = Zp64Factory.createInstance(envType, modulus);
        long[][] result = new long[exponents.length][];
        assert exponents[0] == 1;
        result[0] = base;
        for (int i = 1; i < exponents.length; i++) {
            long[] temp = new long[base.length];
            for (int j = 0; j < base.length; j++) {
                temp[j] = zp64.pow(base[j], exponents[i]);
            }
            result[i] = temp;
        }
        return result;
    }

    /**
     * client generates key pair.
     *
     * @return public keys.
     */
    private List<byte[]> generateKeyPair(byte[] encryptionParams) {
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
}