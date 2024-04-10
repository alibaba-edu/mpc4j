package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
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
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.AbstractSingleCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleCpKsPirDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ALPR21 client-specific preprocessing KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class Alpr21SingleCpKsPirClient<T> extends AbstractSingleCpKsPirClient<T> {
    /**
     * single index client-specific preprocessing PIR client
     */
    private final SingleCpPirClient singleCpPirClient;
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

    public Alpr21SingleCpKsPirClient(Rpc clientRpc, Party serverParty, Alpr21SingleCpKsPirConfig config) {
        super(Alpr21SingleCpKsPirDesc.getInstance(), clientRpc, serverParty, config);
        singleCpPirClient = SingleCpPirFactory.createClient(clientRpc, serverParty, config.getIndexCpPirConfig());
        addSubPto(singleCpPirClient);
        sqOprfReceiver = SqOprfFactory.createReceiver(clientRpc, serverParty, config.getSqOprfConfig());
        addSubPto(sqOprfReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        hashByteLength = config.hashByteLength();
        hash = HashFactory.createInstance(envType, hashByteLength);
    }

    @Override
    public void init(int n, int l) throws MpcAbortException {
        setInitInput(n, l);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        MathPreconditions.checkGreaterOrEqual("keyword_hash_byte_length",
            hashByteLength * Byte.SIZE, PirUtils.getBitLength(n) + CommonConstants.STATS_BIT_LENGTH
        );
        prg = PrgFactory.createInstance(envType, hashByteLength + hashByteLength + byteL);
        sqOprfReceiver.init(1);
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, sqOprfTime, "Client inits sq-OPRF");

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
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
        singleCpPirClient.init(binNum, (hashByteLength + byteL) * Byte.SIZE);
        stopWatch.stop();
        long initPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, initPirTime, "Client inits PIR");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[] pir(T item) throws MpcAbortException {
        setPtoInput(item);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        ByteBuffer itemHash = ByteBuffer.wrap(hash.digestToBytes(ObjectUtils.objectToByteArray(item)));
        SqOprfReceiverOutput sqOprfReceiverOutput = sqOprfReceiver.oprf(new byte[][]{itemHash.array()});
        // split the OPRF key into hash_key || encrypt_key
        byte[] concatKey = prg.extendToBytes(sqOprfReceiverOutput.getPrf(0));
        byte[] hashKey = new byte[hashByteLength];
        byte[] encryptKey = new byte[hashByteLength + byteL];
        ByteBuffer.wrap(concatKey).get(hashKey).get(encryptKey);
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, sqOprfTime, "Client runs sq-OPRF");

        stopWatch.start();
        byte[][] ciphertexts = new byte[hashNum][byteL];
        for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
            int x = prfs[hashIndex].getInteger(itemHash.array(), binNum);
            ciphertexts[hashIndex] = singleCpPirClient.pir(x);
        }
        stopWatch.stop();
        long pirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, pirTime, "Client runs PIR");

        stopWatch.start();
        byte[] value = handleResponse(ciphertexts, hashKey, encryptKey);
        stopWatch.stop();
        long valueTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, valueTime, "Client handles value");

        logPhaseInfo(PtoState.PTO_END);
        return value;
    }

    private byte[] handleResponse(byte[][] ciphertexts, byte[] hashKey, byte[] encryptKey) {
        for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
            // decrypt using encrypt_key
            byte[] ciphertext = ciphertexts[hashIndex];
            byte[] plaintext = BytesUtils.xor(ciphertext, encryptKey);
            byte[] hash = BytesUtils.clone(plaintext, 0, hashByteLength);
            // match using hash_key
            if (BytesUtils.equals(hash, hashKey)) {
                return BytesUtils.clone(plaintext, hashByteLength, byteL);
            }
        }
        return null;
    }
}
