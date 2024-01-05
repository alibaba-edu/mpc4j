package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.structure.matrix.zl64.Zl64Matrix;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractSingleCpPirServer;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils.INT_MAX_VALUE;
import static edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleCpPirDesc.*;

/**
 * Simple client-specific preprocessing PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
public class SimpleSingleCpPirServer extends AbstractSingleCpPirServer {
    /**
     * Simple PIR params
     */
    private SimpleSingleCpPirParams params;
    /**
     * random seed to compress the random matrix A, see Section 4.1, modification 3:
     * <p>
     * We compress A using pseudorandomness. Specifically, the server and the clients can derive A as the output of a
     * public hash function, modelled as a random oracle, applied to a fixed string in counter mode. This saves on
     * bandwidth and storage, as the server and the clients communicate and store only a small seed to generate A.
     * </p>
     */
    private byte[] seed;
    /**
     * database matrix
     */
    private Zl64Matrix[] databaseMatrix;
    /**
     * number of columns
     */
    private int cols;
    /**
     * partition size
     */
    private int partitionSize;

    public SimpleSingleCpPirServer(Rpc serverRpc, Party clientParty, SimpleSingleCpPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(ZlDatabase database) throws MpcAbortException {
        setInitInput(database);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // server generates and sends the seed for the random matrix A.
        seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(seedPayloadHeader, Collections.singletonList(seed)));
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, seedTime, "Server generates seed");

        stopWatch.start();
        // server partition the database, generates and sends hints
        Zl64Matrix[] hints = serverSetup(database);
        List<byte[]> hintPayload = IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(hints[i].elements))
            .collect(Collectors.toList());
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hintPayloadHeader, hintPayload));
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "Server generates hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    private Zl64Matrix[] serverSetup(ZlDatabase database) {
        // compute max number of partitions
        int maxPartitionNum = CommonUtils.getUnitNum(database.getL(), SimpleSingleCpPirParams.LOG_P - 1);
        int tempPartitionL = 0, rows = 0, d = 0;
        for (int partitionNum = 1; partitionNum <= maxPartitionNum; partitionNum++) {
            tempPartitionL = CommonUtils.getUnitNum(database.getL(), partitionNum);
            int tempPartitionByteL = CommonUtils.getByteLength(tempPartitionL);
            // each row would be divided into d rows in this partition
            d = CommonUtils.getUnitNum(tempPartitionByteL * Byte.SIZE, SimpleSingleCpPirParams.LOG_P - 1);
            // now the database is expanded to d * N, and this may exceed Integer.MAX_VALUE
            BigInteger expandN = BigInteger.valueOf(d).multiply(BigInteger.valueOf(database.rows()));
            if (expandN.compareTo(INT_MAX_VALUE.shiftRight(1)) < 0) {
                // this means we get as large N as possible while satisfying N < Integer.MAX_VALUE
                int[] dimensions = PirUtils.approxSquareDatabaseDims(database.rows(), d);
                rows = dimensions[0];
                cols = dimensions[1];
                params = new SimpleSingleCpPirParams(envType, PirUtils.getBitLength(cols));
                break;
            }
            // since database.rows() must be less than Integer.MAX_VALUE, and we can make as many partitions as possible
            // the loop must break at some point.
        }
        int partitionL = Math.min(tempPartitionL, database.getL());
        NaiveDatabase naiveDatabase = NaiveDatabase.create(database.getL(), database.getBytesData());
        ZlDatabase[] databases = naiveDatabase.partitionZl(partitionL);
        partitionSize = databases.length;
        // public matrix A
        Zl64Matrix matrixA = Zl64Matrix.createRandom(params.zl64, cols, SimpleSingleCpPirParams.N, seed);
        // init the partitioned database matrix
        databaseMatrix = new Zl64Matrix[partitionSize];
        for (int i = 0; i < partitionSize; i++) {
            databaseMatrix[i] = Zl64Matrix.createZeros(params.zl64, rows, cols);
            databaseMatrix[i].setParallel(parallel);
        }
        // init hints
        Zl64Matrix[] hint = new Zl64Matrix[partitionSize];
        int rowElementsNum = rows / d;
        for (int i = 0; i < partitionSize; i++) {
            for (int j = 0; j < cols; j++) {
                for (int l = 0; l < rowElementsNum; l++) {
                    if (j * rowElementsNum + l < n) {
                        byte[] element = databases[i].getBytesData(j * rowElementsNum + l);
                        long[] coeffs = PirUtils.convertBytesToCoeffs(SimpleSingleCpPirParams.LOG_P - 1, 0, element.length, element);
                        // values mod the plaintext modulus p
                        for (int k = 0; k < d; k++) {
                            databaseMatrix[i].set(l * d + k, j, coeffs[k]);
                        }
                    }
                }
            }
            hint[i] = databaseMatrix[i].matrixMul(matrixA);
        }
        return hint;
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

        // generate response
        stopWatch.start();
        List<byte[]> serverResponsePayload = generateResponse(clientQueryPayload);
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * server generates response.
     *
     * @param clientQuery client query.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> generateResponse(List<byte[]> clientQuery) throws MpcAbortException {
        long[] queryElements = LongUtils.byteArrayToLongArray(clientQuery.get(0));
        MpcAbortPreconditions.checkArgument(queryElements.length == cols);
        Zl64Vector query = Zl64Vector.create(params.zl64, queryElements);
        return IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(databaseMatrix[i].matrixMulVector(query).getElements()))
            .collect(Collectors.toList());
    }
}
