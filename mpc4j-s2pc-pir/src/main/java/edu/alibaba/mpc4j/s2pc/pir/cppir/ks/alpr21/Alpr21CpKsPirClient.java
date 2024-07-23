package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * ALPR21 client-specific preprocessing KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class Alpr21CpKsPirClient<T> extends AbstractCpKsPirClient<T> {
    /**
     * single index client-specific preprocessing PIR client
     */
    private final CpIdxPirClient cpIdxPirClient;
    /**
     * sq-OPRF receiver
     */
    private final SqOprfReceiver sqOprfReceiver;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * hash byte length (for keyword)
     */
    private final int hashByteLength;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * h: {0, 1}^* → {0, 1}^l
     */
    private final Hash hash;
    /**
     * PRG used for encrypt concat value
     */
    private Prg prg;
    /**
     * bin num
     */
    private int binNum;
    /**
     * cuckoo hash PRFs
     */
    private Prf[] prfs;

    public Alpr21CpKsPirClient(Rpc clientRpc, Party serverParty, Alpr21CpKsPirConfig config) {
        super(Alpr21CpKsPirDesc.getInstance(), clientRpc, serverParty, config);
        cpIdxPirClient = CpIdxPirFactory.createClient(clientRpc, serverParty, config.getIndexCpPirConfig());
        addSubPto(cpIdxPirClient);
        sqOprfReceiver = SqOprfFactory.createReceiver(clientRpc, serverParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        hashByteLength = config.hashByteLength();
        hash = HashFactory.createInstance(envType, hashByteLength);
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        MathPreconditions.checkGreaterOrEqual("keyword_hash_byte_length",
            hashByteLength * Byte.SIZE, PirUtils.getBitLength(n) + CommonConstants.STATS_BIT_LENGTH
        );
        prg = PrgFactory.createInstance(envType, hashByteLength + hashByteLength + byteL);
        sqOprfReceiver.init(maxBatchNum);
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, sqOprfTime, "Client inits sq-OPRF");

        List<byte[]> cuckooHashKeysPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal());
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == hashNum);

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == hashNum);
        byte[][] hashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, n);
        prfs = Arrays.stream(hashKeys)
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, cuckooHashTime, "Client inits cuckoo hashes");

        stopWatch.start();
        int binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, n);
        cpIdxPirClient.init(binNum, (hashByteLength + byteL) * Byte.SIZE, hashNum * maxBatchNum);
        stopWatch.stop();
        long initPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, initPirTime, "Client inits PIR");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(ArrayList<T> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[][] hashes = keys.stream()
            .map(x -> hash.digestToBytes(ObjectUtils.objectToByteArray(x)))
            .toArray(byte[][]::new);
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(hashes);
        // split the OPRF key into hash_key || encrypt_key
        byte[][] hashKeys = new byte[batchNum][hashByteLength];
        byte[][] encryptKeys = new byte[batchNum][hashByteLength + byteL];
        IntStream.range(0, batchNum).forEach(i -> {
            byte[] concatKey = prg.extendToBytes(sqOprfReceiverOutput.getPrf(i));
            ByteBuffer.wrap(concatKey).get(hashKeys[i]).get(encryptKeys[i]);
        });
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, sqOprfTime, "Client runs sq-OPRF");

        stopWatch.start();
        int[] xs = new int[batchNum * hashNum];
        IntStream.range(0, batchNum).forEach(i -> {
            byte[] hash = hashes[i];
            for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
                xs[i * hashNum + hashIndex] = prfs[hashIndex].getInteger(hash, binNum);
            }
        });
        byte[][] ciphertexts = cpIdxPirClient.pir(xs);
        stopWatch.stop();
        long pirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, pirTime, "Client runs PIR");

        stopWatch.start();
        byte[][] entries = handleResponse(ciphertexts, hashKeys, encryptKeys);
        stopWatch.stop();
        long valueTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, valueTime, "Client handles value");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private byte[][] handleResponse(byte[][] ciphertexts, byte[][] hashKeys, byte[][] encryptKeys) {
        return IntStream.range(0, batchNum)
            .mapToObj(i -> {
                int offset = i * hashNum;
                for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
                    int index = offset + hashIndex;
                    // decrypt using encrypt_key
                    byte[] ciphertext = ciphertexts[index];
                    byte[] plaintext = BytesUtils.xor(ciphertext, encryptKeys[i]);
                    byte[] hash = BytesUtils.clone(plaintext, 0, hashByteLength);
                    // match using hash_key
                    if (BytesUtils.equals(hash, hashKeys[i])) {
                        return BytesUtils.clone(plaintext, hashByteLength, byteL);
                    }
                }
                return null;
            })
            .toArray(byte[][]::new);
    }
}
