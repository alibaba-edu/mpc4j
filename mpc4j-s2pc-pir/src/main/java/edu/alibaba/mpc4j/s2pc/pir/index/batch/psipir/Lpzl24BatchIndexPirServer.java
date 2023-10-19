package edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiParams;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiServer;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir.Lpzl24BatchIndexPirPtoDesc.*;

/**
 * PSI-PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzl24BatchIndexPirServer extends AbstractBatchIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * UPSI server
     */
    private final Cmg21UpsiServer<ByteBuffer> upsiServer;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * PRF key
     */
    private BigInteger alpha;
    /**
     * encoded database
     */
    private List<List<byte[]>> plaintexts;
    /**
     * bin size
     */
    private int[] binSize;
    /**
     * max bin size
     */
    private int maxBinSize;
    /**
     * encryption params
     */
    private byte[] encryptionParams;
    /**
     * relinearization keys
     */
    private byte[] relinKeys;
    /**
     * ecc
     */
    private final ByteFullEcc ecc;

    public Lpzl24BatchIndexPirServer(Rpc serverRpc, Party clientParty, Lpzl24BatchIndexPirConfig config) {
        super(Lpzl24BatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        upsiServer = new Cmg21UpsiServer<>(serverRpc, clientParty, (Cmg21UpsiConfig) config.getUpsiConfig());
        addSubPtos(upsiServer);
        ecc = ByteEccFactory.createFullInstance(envType);
    }

    @Override
    public void init(NaiveDatabase database, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(database, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // UPSI params
        Cmg21UpsiParams params = null;
        if (num <= 1 << 20) {
            if (maxRetrievalSize <= 256) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_256;
            } else if (maxRetrievalSize <= 512) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_COM;
            } else if (maxRetrievalSize <= 1024) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_COM;
            } else if (maxRetrievalSize <= 2048) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_COM;
            } else if (maxRetrievalSize <= 4096) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_COM;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        } else {
            if (maxRetrievalSize <= 1024) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_2048;
            } else if (maxRetrievalSize <= 4096) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_4096;
            } else if (maxRetrievalSize <= 11041) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_11041;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        }
        assert params != null;
        upsiServer.init(params);

        DataPacketHeader bfvParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> bfvKeyPair = rpc.receive(bfvParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(bfvKeyPair.size() == 2);
        encryptionParams = bfvKeyPair.remove(0);
        relinKeys = bfvKeyPair.remove(0);

        stopWatch.start();
        hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashNum(), secureRandom);
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        binSize = new int[partitionSize];
        List<RandomPadHashBin<ByteBuffer>> hashBins =
            (parallel ? IntStream.range(0, partitionSize).parallel() : IntStream.range(0, partitionSize))
                .mapToObj(i -> {
                    // compute PRF
                    List<ByteBuffer> elementPrf = computeElementPrf(i);
                    // complete hash bin
                    return generateCompleteHashBin(elementPrf, i);
                }).collect(Collectors.toList());
        maxBinSize = Arrays.stream(binSize, 0, partitionSize).max().orElse(0);
        plaintexts = (parallel ? IntStream.range(0, partitionSize).parallel() : IntStream.range(0, partitionSize))
            .mapToObj(i -> {
                // padding hash bin
                List<List<HashBinEntry<ByteBuffer>>> paddingHashBin = paddingHashBin(hashBins.get(i));
                // compute coefficients
                List<long[][]> coeffs = upsiServer.encodeDatabase(paddingHashBin, maxBinSize);
                IntStream intStream = IntStream.range(0, coeffs.size());
                intStream = parallel ? intStream.parallel() : intStream;
                return intStream
                    .mapToObj(j -> Lpzl24BatchIndexPirNativeUtils.processDatabase(
                        encryptionParams, coeffs.get(j), upsiServer.params.getPsLowDegree()
                    ))
                    .collect(Collectors.toCollection(ArrayList::new));
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // MP-OPRF
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();
        List<byte[]> blindPrfPayload = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrfPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Server runs OPRFs");

        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == upsiServer.params.getCiphertextNum() * upsiServer.params.getQueryPowers().length,
            "The size of query is incorrect"
        );

        stopWatch.start();
        int[][] powerDegree = upsiServer.computePowerDegree();
        List<byte[]> ciphertextPowers = computeQueryPowers(queryPayload, powerDegree);
        List<byte[]> response = IntStream.range(0, partitionSize)
            .mapToObj(i -> computeResponse(ciphertextPowers, powerDegree, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader keywordResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keywordResponseHeader, response));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * compute element prf.
     *
     * @return element prf.
     */
    private List<ByteBuffer> computeElementPrf(int partitionIndex) {
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        IntStream intStream = IntStream.range(0, databases[partitionIndex].rows());
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> ecc.hashToCurve(databases[partitionIndex].getBytesData(i)))
            .map(hash -> ecc.mul(hash, alpha))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * generate complete hash bin.
     *
     * @param elementList    element list.
     * @param partitionIndex partition index.
     * @return complete hash bin.
     */
    private RandomPadHashBin<ByteBuffer> generateCompleteHashBin(List<ByteBuffer> elementList, int partitionIndex) {
        int binNum = upsiServer.params.getBinNum();
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, num, hashKeys);
        completeHash.insertItems(elementList);
        binSize[partitionIndex] = IntStream.range(0, binNum).map(completeHash::binSize).max().orElse(0);
        return completeHash;
    }

    private List<List<HashBinEntry<ByteBuffer>>> paddingHashBin(RandomPadHashBin<ByteBuffer> completeHash) {
        int binNum = upsiServer.params.getBinNum();
        List<List<HashBinEntry<ByteBuffer>>> completeHashBins = new ArrayList<>();
        byte[] randomBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(ByteBuffer.wrap(randomBytes));
        for (int i = 0; i < binNum; i++) {
            List<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        return completeHashBins;
    }

    /**
     * handle blind element.
     *
     * @param blindElements blind element.
     * @return blind element prf.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> handleBlindPayload(List<byte[]> blindElements) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindElements.size() > 0);
        Stream<byte[]> blindStream = blindElements.stream();
        blindStream = parallel ? blindStream.parallel() : blindStream;
        return blindStream
            .map(element -> ecc.mul(element, alpha))
            .collect(Collectors.toList());
    }

    /**
     * compute query powers.
     *
     * @param query       query powers.
     * @param powerDegree power degree.
     * @return query powers.
     */
    private List<byte[]> computeQueryPowers(List<byte[]> query, int[][] powerDegree) {
        IntStream intStream = IntStream.range(0, upsiServer.params.getCiphertextNum());
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Lpzl24BatchIndexPirNativeUtils.computeEncryptedPowers(
                encryptionParams,
                relinKeys,
                query.subList(
                    i * upsiServer.params.getQueryPowers().length, (i + 1) * upsiServer.params.getQueryPowers().length),
                powerDegree,
                upsiServer.params.getQueryPowers(),
                upsiServer.params.getPsLowDegree())
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * compute response.
     *
     * @param clientQuery client query.
     * @return server response.
     */
    private List<byte[]> computeResponse(List<byte[]> clientQuery, int[][] powerDegree, int partitionIndex) {
        int count = CommonUtils.getUnitNum(maxBinSize, upsiServer.params.getMaxPartitionSizePerBin());
        int partitionCount = plaintexts.size() / partitionSize;
        IntStream intStream = IntStream.range(0, upsiServer.params.getCiphertextNum());
        if (upsiServer.params.getPsLowDegree() > 0) {
            return intStream
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, count).parallel() : IntStream.range(0, count))
                        .mapToObj(j -> Lpzl24BatchIndexPirNativeUtils.optComputeMatches(
                            encryptionParams,
                            relinKeys,
                            plaintexts.get(i * count + j + partitionIndex * partitionCount),
                            clientQuery.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            upsiServer.params.getPsLowDegree()
                            ))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            return intStream
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, count).parallel() : IntStream.range(0, count))
                        .mapToObj(j -> Lpzl24BatchIndexPirNativeUtils.naiveComputeMatches(
                            encryptionParams,
                            plaintexts.get(i * count + j + partitionIndex * partitionCount),
                            clientQuery.subList(i * powerDegree.length, (i + 1) * powerDegree.length)
                            ))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        }
    }

    @Override
    protected void setInitInput(NaiveDatabase database, int maxRetrievalSize) {
        MathPreconditions.checkPositive("serverElementSize", database.rows());
        num = database.rows();
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        MathPreconditions.checkPositive("elementBitLength", database.getL());
        this.elementBitLength = database.getL();
        this.partitionSize = CommonUtils.getUnitNum(elementBitLength, 1);
        databases = new ZlDatabase[partitionSize];
        int byteLength = CommonUtils.getByteLength(elementBitLength);
        for (int i = 0; i < partitionSize; i++) {
            List<byte[]> temp = new ArrayList<>();
            for (int j = 0; j < num; j++) {
                boolean value = BinaryUtils.getBoolean(database.getBytesData(j), byteLength * Byte.SIZE - 1 - i);
                if (value) {
                    temp.add(IntUtils.intToByteArray(j));
                }
            }
            int paddingNum = num - temp.size();
            for (int j = 0; j < paddingNum; j++) {
                temp.add(IntUtils.intToByteArray(num + j));
            }
            databases[i] = ZlDatabase.create(Integer.BYTES * Byte.SIZE, temp.toArray(new byte[0][]));
        }
        initState();
    }
}
