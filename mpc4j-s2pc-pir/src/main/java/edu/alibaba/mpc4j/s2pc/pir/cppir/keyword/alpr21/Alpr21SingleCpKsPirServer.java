package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleCpKsPirServerOutput;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.AbstractSingleCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleCpKsPirDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ALPR21 client-specific preprocessing KSPIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class Alpr21SingleCpKsPirServer<T> extends AbstractSingleCpKsPirServer<T> {
    /**
     * single index client-specific preprocessing PIR server
     */
    private final SingleCpPirServer singleCpPirServer;
    /**
     * sq-OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * h: {0, 1}^* → {0, 1}^l
     */
    private final Hash hash;
    /**
     * hash byte length (for keyword)
     */
    private final int hashByteLength;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * PRG used for encrypt concat value
     */
    private Prg prg;
    /**
     * OPRF key
     */
    private SqOprfKey sqOprfKey;
    /**
     * hash for ⊥
     */
    private ByteBuffer botHash;
    /**
     * hash keys
     */
    private byte[][] hashKeys;

    public Alpr21SingleCpKsPirServer(Rpc serverRpc, Party clientParty, Alpr21SingleCpKsPirConfig config) {
        super(Alpr21SingleCpKsPirDesc.getInstance(), serverRpc, clientParty, config);
        singleCpPirServer = SingleCpPirFactory.createServer(serverRpc, clientParty, config.getIndexCpPirConfig());
        addSubPto(singleCpPirServer);
        sqOprfSender = SqOprfFactory.createSender(serverRpc, clientParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        hashByteLength = config.hashByteLength();
        hash = HashFactory.createInstance(envType, hashByteLength);

    }

    @Override
    public void init(Map<T, byte[]> keyValueMap, int labelBitLength) throws MpcAbortException {
        setInitInput(keyValueMap, labelBitLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        MathPreconditions.checkGreaterOrEqual("keyword_hash_byte_length",
            hashByteLength * Byte.SIZE, PirUtils.getBitLength(n) + CommonConstants.STATS_BIT_LENGTH
        );
        prg = PrgFactory.createInstance(envType, hashByteLength + hashByteLength + byteL);
        sqOprfKey = sqOprfSender.keyGen();
        sqOprfSender.init(1, sqOprfKey);
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, sqOprfTime, "Server inits sq-OPRF");

        stopWatch.start();
        // compute hash-value map
        botHash = ByteBuffer.wrap(hash.digestToBytes(botByteBuffer.array()));
        Stream<Map.Entry<T, byte[]>> keywordStream = keyValueMap.entrySet().stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        Map<ByteBuffer, byte[]> hashValueMap = keywordStream
            .collect(Collectors.toMap(
                entry -> {
                    ByteBuffer keywordHash = ByteBuffer.wrap(hash.digestToBytes(ObjectUtils.objectToByteArray(entry.getKey())));
                    assert !keywordHash.equals(botHash);
                    return keywordHash;
                },
                Entry::getValue)
            );
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, hashTime, "Server inits cuckoo hash");

        stopWatch.start();
        // init index pir
        ZlDatabase database = generateCuckooHashBin(hashValueMap);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        singleCpPirServer.init(database);
        stopWatch.stop();
        long initPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, initPirTime, "Server inits PIR");

        logPhaseInfo(PtoState.INIT_END);
    }

    private ZlDatabase generateCuckooHashBin(Map<ByteBuffer, byte[]> hashValueMap) {
        CuckooHashBin<ByteBuffer> cuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, n, hashValueMap.keySet(), secureRandom
        );
        hashKeys = cuckooHashBin.getHashKeys();
        cuckooHashBin.insertPaddingItems(botHash);
        int binNum = cuckooHashBin.binNum();
        byte[][] cuckooHashBinItems = new byte[binNum][];
        IntStream binIndexStream = IntStream.range(0, binNum);
        binIndexStream = parallel ? binIndexStream.parallel() : binIndexStream;
        binIndexStream.forEach(binIndex -> {
            HashBinEntry<ByteBuffer> hashBinEntry = cuckooHashBin.getHashBinEntry(binIndex);
            ByteBuffer keywordHash = hashBinEntry.getItem();
            byte[] value = new byte[byteL];
            if (hashBinEntry.getHashIndex() != HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                value = hashValueMap.get(hashBinEntry.getItem());
            } else {
                secureRandom.nextBytes(value);
            }
            byte[] oprfKey = prg.extendToBytes(sqOprfKey.getPrf(keywordHash.array()));
            // split the OPRF key into hash_key || encrypt_key
            byte[] hashKey = new byte[hashByteLength];
            byte[] encryptKey = new byte[hashByteLength + byteL];
            ByteBuffer.wrap(oprfKey).get(hashKey).get(encryptKey);
            // value = hash_key || value
            byte[] concatHashValue = Bytes.concat(hashKey, value);
            // encrypt using encrypt_key
            BytesUtils.xori(concatHashValue, encryptKey);
            cuckooHashBinItems[binIndex] = concatHashValue;
        });
        return ZlDatabase.create((hashByteLength + byteL) * Byte.SIZE, cuckooHashBinItems);
    }

    @Override
    public SingleCpKsPirServerOutput pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        sqOprfSender.oprf(1);
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sqOprfTime, "Server runs sq-OPRF");

        stopWatch.start();
        for (int i = 0; i < hashNum; i++) {
            singleCpPirServer.pir();
        }
        stopWatch.stop();
        long pirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, pirTime, "Server runs PIR");

        logPhaseInfo(PtoState.PTO_END);
        return SingleCpKsPirServerOutput.UNKNOWN;
    }
}
