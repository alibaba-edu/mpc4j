package edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.structure.matrix.zl64.Zl64Matrix;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirServer;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils.INT_MAX_VALUE;
import static edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir.CuckooHashBatchSimplePirPtoDesc.PtoStep;

/**
 * Batch Simple PIR based on Cuckoo Hash server.
 *
 * @author Liqiang Peng
 * @date 2023/7/11
 */
public class CuckooHashBatchSimplePirServer extends AbstractBatchIndexPirServer {

    /**
     * cuckoo hash bin type
     */
    private final IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType;
    /**
     * simple PIR server
     */
    private final Hhcm23SimpleSingleIndexPirServer simplePirServer;
    /**
     * bin num
     */
    private int binNum;
    /**
     * column num
     */
    private int cols;
    /**
     * database
     */
    private Zl64Matrix[][] db;
    /**
     * partition database
     */
    ZlDatabase[][] databases;
    /**
     * seed
     */
    byte[] seed;
    /**
     * communication optimal
     */
    private final boolean isCommunicationOptimal;
    /**
     * hash num
     */
    private final int hashNum;

    public CuckooHashBatchSimplePirServer(Rpc serverRpc, Party clientParty, CuckooHashBatchSimplePirConfig config) {
        super(CuckooHashBatchSimplePirPtoDesc.getInstance(), serverRpc, clientParty, config);
        simplePirServer = new Hhcm23SimpleSingleIndexPirServer(serverRpc, clientParty, config.getSimplePirConfig());
        cuckooHashBinType = config.getCuckooHashBinType();
        isCommunicationOptimal = config.isCommunicationOptimal();
        hashNum = IntCuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(NaiveDatabase database, int maxRetrievalSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        setInitInput(database, maxRetrievalSize);

        stopWatch.start();
        // generate simple hash bin
        binNum = IntCuckooHashBinFactory.getBinNum(cuckooHashBinType, maxRetrievalSize);
        byte[][] hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        NaiveDatabase[] binDatabase = generateSimpleHashBin(database, hashKeys);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        stopWatch.start();
        // init single index PIR server
        simplePirServer.setParallel(parallel);
        simplePirServer.setDefaultParams();
        Zl64Matrix[][] hint = setup(binDatabase, binDatabase[0].rows());
        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(seedPayloadHeader, Collections.singletonList(seed)));
        List<byte[]> hintPayload = new ArrayList<>();
        for (int i = 0; i < binNum; i++) {
            for (int j = 0; j < partitionSize; j++) {
                hintPayload.add(LongUtils.longArrayToByteArray(hint[i][j].elements));
            }
        }
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hintPayloadHeader, hintPayload));
        stopWatch.stop();
        long initIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initIndexPirTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive query
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == binNum * partitionSize);

        // generate response for each bucket
        stopWatch.start();
        List<byte[]> responsePayload = IntStream.range(0, binNum)
            .mapToObj(i ->
                generateResponse(clientQueryPayload.subList(i * partitionSize, (i + 1) * partitionSize), db[i])
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates response");

        logPhaseInfo(PtoState.PTO_END);
    }

    public List<byte[]> generateResponse(List<byte[]> clientQuery, Zl64Matrix[] db) {
        long[] queryElements = LongUtils.byteArrayToLongArray(clientQuery.get(0));
        Zl64Vector query = Zl64Vector.create(simplePirServer.params.zl64, queryElements);
        return IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(db[i].matrixMulVector(query).getElements()))
            .collect(Collectors.toList());
    }

    /**
     * generate simple hash bin.
     *
     * @param hashKeys hash keys.
     * @param database database.
     * @return bin database.
     */
    private NaiveDatabase[] generateSimpleHashBin(NaiveDatabase database, byte[][] hashKeys) {
        int[] totalIndex = IntStream.range(0, num).toArray();
        IntHashBin intHashBin = new SimpleIntHashBin(envType, binNum, num, hashKeys);
        intHashBin.insertItems(totalIndex);
        int maxBinSize = IntStream.range(0, binNum).map(intHashBin::binSize).max().orElse(0);
        byte[][][] paddingCompleteHashBin = new byte[binNum][maxBinSize][elementByteLength];
        byte[] paddingEntry = BytesUtils.randomByteArray(elementByteLength, elementBitLength, secureRandom);
        for (int i = 0; i < binNum; i++) {
            int size = intHashBin.binSize(i);
            for (int j = 0; j < size; j++) {
                paddingCompleteHashBin[i][j] = database.getBytesData(intHashBin.getBin(i)[j]);
            }
            int paddingNum = maxBinSize - size;
            for (int j = 0; j < paddingNum; j++) {
                paddingCompleteHashBin[i][j + size] = BytesUtils.clone(paddingEntry);
            }
        }
        return IntStream.range(0, binNum)
            .mapToObj(i -> NaiveDatabase.create(elementBitLength, paddingCompleteHashBin[i]))
            .toArray(NaiveDatabase[]::new);
    }

    /**
     * generate hint.
     *
     * @param binIndex bin index.
     * @param rows     rows.
     * @param d        d.
     * @param a        matrix A.
     * @return hint.
     */
    public Zl64Matrix[] generateHint(int binIndex, int rows, int d, Zl64Matrix a) {
        for (int i = 0; i < partitionSize; i++) {
            db[binIndex][i] = Zl64Matrix.createZeros(simplePirServer.params.zl64, rows, cols);
            db[binIndex][i].setParallel(parallel);
        }
        Zl64Matrix[] hint = new Zl64Matrix[partitionSize];
        int rowElementsNum = rows / d;
        for (int i = 0; i < partitionSize; i++) {
            for (int j = 0; j < cols; j++) {
                for (int l = 0; l < rowElementsNum; l++) {
                    if (j * rowElementsNum + l < databases[binIndex][i].rows()) {
                        byte[] element = databases[binIndex][i].getBytesData(j * rowElementsNum + l);
                        long[] coeffs = PirUtils.convertBytesToCoeffs(
                            simplePirServer.params.logP - 1, 0, element.length, element
                        );
                        // values mod the plaintext modulus p
                        for (int k = 0; k < d; k++) {
                            db[binIndex][i].set(l * d + k, j, coeffs[k]);
                        }
                    }
                }
            }
            hint[i] = db[binIndex][i].matrixMul(a);
        }
        return hint;
    }

    /**
     * server setup.
     *
     * @param binDatabase bin database.
     * @param binSize     bin size.
     * @return hint.
     */
    Zl64Matrix[][] setup(NaiveDatabase[] binDatabase, int binSize) {
        int maxPartitionBitLength = 0, rows = 0, d = 0;
        int upperBound = CommonUtils.getUnitNum(elementBitLength, simplePirServer.params.logP - 1);
        for (int count = 1; count < upperBound + 1; count++) {
            maxPartitionBitLength = CommonUtils.getUnitNum(elementBitLength, count);
            int maxPartitionByteLength = CommonUtils.getByteLength(maxPartitionBitLength);
            d = CommonUtils.getUnitNum(maxPartitionByteLength * Byte.SIZE, simplePirServer.params.logP - 1);
            if ((BigInteger.valueOf(d).multiply(BigInteger.valueOf(binSize)))
                .compareTo(INT_MAX_VALUE.shiftRight(1)) < 0) {
                if (isCommunicationOptimal) {
                    rows = (int) Math.max(2, Math.ceil(Math.sqrt(d * binSize * binNum)));
                    rows = CommonUtils.getUnitNum(rows, binNum);
                    long rem = rows % d;
                    if (rem != 0) {
                        rows += d - rem;
                    }
                    cols = (int) Math.ceil((double) d * binSize / rows);
                } else {
                    int[] dims = PirUtils.approxSquareDatabaseDims(binSize, d);
                    rows = dims[0];
                    cols = dims[1];
                }
                simplePirServer.params.setPlainModulo(PirUtils.getBitLength(cols));
                break;
            }
        }
        int partitionBitLength = Math.min(maxPartitionBitLength, elementBitLength);
        partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        databases = new ZlDatabase[binNum][partitionSize];
        for (int i = 0; i < binNum; i++) {
            databases[i] = binDatabase[i].partitionZl(partitionBitLength);
        }
        // public matrix A
        seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        Zl64Matrix a = Zl64Matrix.createRandom(simplePirServer.params.zl64, cols, simplePirServer.params.n, seed);
        db = new Zl64Matrix[binNum][partitionSize];
        // generate the client's hint, which is the database multiplied by A. Also known as the setup.
        Zl64Matrix[][] hint = new Zl64Matrix[binNum][partitionSize];
        for (int i = 0; i < binNum; i++) {
            hint[i] = generateHint(i, rows, d, a);
        }
        return hint;
    }
}