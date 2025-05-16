package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi;

import edu.alibaba.mpc4j.common.rpc.*;
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
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.AbstractStdKsPirClient;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.createCuckooHashBin;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirPtoDesc.getInstance;

/**
 * Label PSI standard KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class LabelpsiStdKsPirClient<T> extends AbstractStdKsPirClient<T> {
    /**
     * stream cipher
     */
    private final StreamCipher streamCipher;
    /**
     * Label PSI PIR params
     */
    private final LabelpsiStdKsPirParams params;
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
     * client keys
     */
    private List<byte[]> clientKeys;
    /**
     * iv byte length
     */
    private final int ivByteLength;

    public LabelpsiStdKsPirClient(Rpc clientRpc, Party serverParty, LabelpsiStdKsPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        streamCipher = StreamCipherFactory.createInstance(envType);
        ecc = ByteEccFactory.createFullInstance(envType);
        ivByteLength = 0;
        params = config.getParams();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        assert maxBatchNum <= params.maxRetrievalSize();
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        Pair<List<byte[]>, List<byte[]>> keyPair = generateKeyPair();
        clientKeys = keyPair.getLeft();
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), keyPair.getRight());
        List<byte[]> hashKeyPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal());
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getCuckooHashKeyNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(ArrayList<T> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // run MP-OPRF
        stopWatch.start();
        List<byte[]> blindPayload = generateBlindPayload(keys);
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_BLIND.ordinal(), blindPayload);
        List<byte[]> blindPrfPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_BLIND_PRF.ordinal());
        List<ByteBuffer> keysPrf = handleBlindPrf(blindPrfPayload);
        Map<ByteBuffer, ByteBuffer> prfKeyMap = IntStream.range(0, batchNum)
            .boxed()
            .collect(Collectors.toMap(
                keysPrf::get, i -> ByteBuffer.wrap(ObjectUtils.objectToByteArray(keys.get(i))), (a, b) -> b)
            );
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, oprfTime, "Client executes OPRF");

        stopWatch.start();
        // generate query
        ArrayList<ByteBuffer> distinctKeys = new ArrayList<>();
        IntStream.range(0, batchNum).filter(i -> !distinctKeys.contains(keysPrf.get(i))).forEach(i -> distinctKeys.add(keysPrf.get(i)));
        CuckooHashBin<ByteBuffer> cuckooHashBin = generateCuckooHashBin(distinctKeys);
        List<byte[]> queryPayload = query(cuckooHashBin);
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, genQueryTime, "Client generates query");

        List<byte[]> keyResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal());
        List<byte[]> valueResponsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal());

        stopWatch.start();
        byte[][] entries = handleResponse(keyResponsePayload, valueResponsePayload, prfKeyMap, cuckooHashBin, keys);
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, decodeResponseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private byte[][] handleResponse(List<byte[]> keyResponse, List<byte[]> valueResponse,
                                    Map<ByteBuffer, ByteBuffer> prfKeyMap, CuckooHashBin<ByteBuffer> cuckooHashBin,
                                    ArrayList<T> keys)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(keyResponse.size() % params.getCiphertextNum() == 0);
        MpcAbortPreconditions.checkArgument(valueResponse.size() % params.getCiphertextNum() == 0);
        Stream<byte[]> keyResponseStream = parallel ? keyResponse.stream().parallel() : keyResponse.stream();
        List<long[]> decryptedKeyResponse = keyResponseStream
            .map(i -> LabelpsiStdKsPirNativeUtils.decodeReply(params.getEncryptionParams(), clientKeys.get(1), i))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<byte[]> valueResponseStream = parallel ? valueResponse.stream().parallel() : valueResponse.stream();
        List<long[]> decryptedValueResponse = valueResponseStream
            .map(i -> LabelpsiStdKsPirNativeUtils.decodeReply(params.getEncryptionParams(), clientKeys.get(1), i))
            .collect(Collectors.toCollection(ArrayList::new));
        return recoverPirResult(decryptedKeyResponse, decryptedValueResponse, prfKeyMap, cuckooHashBin, keys);
    }

    private List<byte[]> query(CuckooHashBin<ByteBuffer> cuckooHashBin) {
        List<long[][]> encodedQueryList = encodeQuery(cuckooHashBin);
        Stream<long[][]> encodedQueryStream = parallel ? encodedQueryList.stream().parallel() : encodedQueryList.stream();
        return encodedQueryStream
            .map(i -> LabelpsiStdKsPirNativeUtils.generateQuery(
                params.getEncryptionParams(), clientKeys.get(0), clientKeys.get(1), i)
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private byte[][] recoverPirResult(List<long[]> decryptedKeyReply, List<long[]> decryptedValueReply,
                                      Map<ByteBuffer, ByteBuffer> prfKeyMap, CuckooHashBin<ByteBuffer> cuckooHashBin,
                                      ArrayList<T> keys) {
        byte[][] entries = new byte[batchNum][];
        int itemPartitionNum = decryptedKeyReply.size() / params.getCiphertextNum();
        int labelPartitionNum = CommonUtils.getUnitNum((byteL + ivByteLength) * Byte.SIZE,
            (LongUtils.ceilLog2(params.getPlainModulus()) - 1) * params.getItemEncodedSlotSize());
        int shiftBits = CommonUtils.getUnitNum((byteL + ivByteLength) * Byte.SIZE,
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
                        byte[] keyBytes = BlockUtils.zeroBlock();
                        System.arraycopy(oprf, 0, keyBytes, 0, CommonConstants.BLOCK_BYTE_LENGTH);
                        byte[] ciphertextLabel = BigIntegerUtils.nonNegBigIntegerToByteArray(label, byteL + ivByteLength);
                        byte[] paddingCipher = BytesUtils.paddingByteArray(
                            ciphertextLabel, byteL + CommonConstants.BLOCK_BYTE_LENGTH
                        );
                        byte[] plaintextLabel = streamCipher.ivDecrypt(keyBytes, paddingCipher);
                        ByteBuffer item = prfKeyMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem());
                        for (int pos = 0; pos < batchNum; pos++) {
                            ByteBuffer key = ByteBuffer.wrap(ObjectUtils.objectToByteArray(keys.get(pos)));
                            if (key.equals(item)) {
                                entries[pos] = plaintextLabel;
                            }
                        }
                        j = j + params.getItemEncodedSlotSize() - 1;
                    }
                }
            }
        }
        return entries;
    }

    private CuckooHashBin<ByteBuffer> generateCuckooHashBin(List<ByteBuffer> items)
        throws MpcAbortException {
        CuckooHashBin<ByteBuffer> cuckooHashBin = createCuckooHashBin(
            envType, params.getCuckooHashBinType(), batchNum, params.getBinNum(), hashKeys
        );
        boolean success = false;
        cuckooHashBin.insertItems(items);
        if (cuckooHashBin.itemNumInStash() == 0) {
            success = true;
        }
        cuckooHashBin.insertPaddingItems(botByteBuffer);
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
        IntStream ciphertextStream =
            parallel ? IntStream.range(0, params.getCiphertextNum()).parallel() : IntStream.range(0, params.getCiphertextNum());
        return ciphertextStream
            .mapToObj(i -> computePowers(items[i], params.getPlainModulus(), params.getQueryPowers()))
            .collect(Collectors.toList());
    }

    private List<byte[]> generateBlindPayload(ArrayList<T> keys) {
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[batchNum];
        IntStream intStream = parallel ? IntStream.range(0, batchNum).parallel() : IntStream.range(0, batchNum);
        return intStream
            .mapToObj(index -> {
                // generate blind factor
                BigInteger beta = BigIntegerUtils.randomPositive(n, secureRandom);
                inverseBetas[index] = beta.modInverse(n);
                byte[] item = ObjectUtils.objectToByteArray(keys.get(index));
                // hash to point
                byte[] element = ecc.hashToCurve(item);
                // blinding
                return ecc.mul(element, beta);
            })
            .collect(Collectors.toList());
    }

    private List<ByteBuffer> handleBlindPrf(List<byte[]> blindPrf) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPrf.size() == batchNum);
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        byte[][] blindPrfArray = blindPrf.toArray(new byte[0][]);
        IntStream intStream = parallel ? IntStream.range(0, batchNum).parallel() : IntStream.range(0, batchNum);
        return intStream
            .mapToObj(index -> ecc.mul(blindPrfArray[index], inverseBetas[index]))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }

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

    private Pair<List<byte[]>, List<byte[]>> generateKeyPair() {
        List<byte[]> keyPair = LabelpsiStdKsPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 3);
        List<byte[]> clientKeys = new ArrayList<>();
        clientKeys.add(keyPair.get(0));
        clientKeys.add(keyPair.get(1));
        List<byte[]> serverKeys = new ArrayList<>();
        serverKeys.add(keyPair.get(0));
        serverKeys.add(keyPair.get(2));
        return Pair.of(clientKeys, serverKeys);
    }
}