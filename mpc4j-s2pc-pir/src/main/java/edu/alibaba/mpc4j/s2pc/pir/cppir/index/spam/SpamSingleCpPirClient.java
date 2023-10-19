package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractSingleCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint.SpamBackupHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint.SpamDirectPrimaryHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint.SpamPrimaryHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint.SpamProgrammedPrimaryHint;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * SPAM client-specific preprocessing PIR client.
 *
 * @author Weiran Liu
 * @date 2023/9/4
 */
public class SpamSingleCpPirClient extends AbstractSingleCpPirClient {
    /**
     * chunk size
     */
    private int chunkSize;
    /**
     * chunk num
     */
    private int chunkNum;
    /**
     * query num for each preprocessing round
     */
    private int roundQueryNum;
    /**
     * current query num
     */
    private int currentQueryNum;
    /**
     * M1, the total number of primary hints.
     */
    private int m1;
    /**
     * M2, the total number of backup hints.
     */
    private int m2;
    /**
     * primary hints
     */
    private SpamPrimaryHint[] primaryHints;
    /**
     * backup hints
     */
    private ArrayList<SpamBackupHint> backupHints;
    /**
     * missing entries
     */
    private TIntObjectMap<byte[]> missingEntries;
    /**
     * local cache entries
     */
    private TIntObjectMap<byte[]> localCacheEntries;

    public SpamSingleCpPirClient(Rpc clientRpc, Party serverParty, SpamSingleCpPirConfig config) {
        super(SpamSingleCpPirDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int n, int l) throws MpcAbortException {
        setInitInput(n, l);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        chunkSize = SpamSingleCpPirUtils.getChunkSize(n);
        chunkNum = SpamSingleCpPirUtils.getChunkNum(n);
        assert chunkSize * chunkNum >= n
            : "chunkSize * chunkNum must be greater than or equal to n (" + n + "): " + chunkSize * chunkNum;
        roundQueryNum = SpamSingleCpPirUtils.getRoundQueryNum(n);
        m1 = SpamSingleCpPirUtils.getM1(n);
        m2 = SpamSingleCpPirUtils.getM2(n);
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paramTime,
            String.format(
                "Client sets params: n = %d, ChunkSize = %d, ChunkNum = %d, n (pad) = %d, Q = %d, M1 = %d, M2 = %d",
                n, chunkSize, chunkNum, chunkSize * chunkNum, roundQueryNum, m1, m2
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // init primary hints and backup hints
        IntStream primaryHintIntStream = IntStream.range(0, m1);
        primaryHintIntStream = parallel ? primaryHintIntStream.parallel() : primaryHintIntStream;
        primaryHints = primaryHintIntStream
            .mapToObj(index -> new SpamDirectPrimaryHint(chunkSize, chunkNum, l, secureRandom))
            .toArray(SpamPrimaryHint[]::new);
        IntStream backupHintIntStream = IntStream.range(0, m2);
        backupHintIntStream = parallel ? backupHintIntStream.parallel() : backupHintIntStream;
        backupHints = backupHintIntStream
            .mapToObj(index -> new SpamBackupHint(chunkSize, chunkNum, l, secureRandom))
            .collect(Collectors.toCollection(ArrayList::new));
        missingEntries = new TIntObjectHashMap<>();
        localCacheEntries = new TIntObjectHashMap<>();
        stopWatch.stop();
        long allocateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, allocateTime, "Client allocates hints");

        stopWatch.start();
        // stream receiving the database
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            final int finalChunkId = chunkId;
            // download DB[k * √N : (k + 1) * √N - 1] from the server
            DataPacketHeader streamRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> streamRequestPayload = rpc.receive(streamRequestHeader).getPayload();

            MpcAbortPreconditions.checkArgument(streamRequestPayload.size() == 1);
            byte[] streamDataByteArray = streamRequestPayload.get(0);
            MpcAbortPreconditions.checkArgument(streamDataByteArray.length == byteL * chunkSize);
            // split the stream database
            ByteBuffer byteBuffer = ByteBuffer.wrap(streamDataByteArray);
            byte[][] chunkDataArray = new byte[chunkSize][byteL];
            for (int j = 0; j < chunkSize; j++) {
                byteBuffer.get(chunkDataArray[j]);
            }
            // update the parity for the primary hints (for j = 0, 1, 2, ..., M)
            // hitMap is irrelevant to the scheme. We want to know if any indices are missed.
            boolean[] hitMap = new boolean[chunkSize];
            Stream<SpamPrimaryHint> primaryHintStream = Arrays.stream(primaryHints);
            primaryHintStream = parallel ? primaryHintStream.parallel() : primaryHintStream;
            primaryHintStream.forEach(primaryHint -> {
                int offset = primaryHint.expandOffset(finalChunkId);
                if (primaryHint.containsChunkId(finalChunkId)) {
                    // if v_{j,k} < ˆv_j then P_j = P_j ⊕ x, here we also include the case for the extra index e_j
                    hitMap[offset] = true;
                    primaryHint.xori(chunkDataArray[offset]);
                }
            });
            // update the parity for the backup hints (for j = M + 1, ..., 1.5M - 1)
            Stream<SpamBackupHint> backupHintStream = backupHints.stream();
            backupHintStream = parallel ? backupHintStream.parallel() : backupHintStream;
            backupHintStream.forEach(backupHint -> {
                int offset = backupHint.expandOffset(finalChunkId);
                if (backupHint.containsChunkId(finalChunkId)) {
                    // if v_{j,k} < ˆv_j then P_j = P_j ⊕ x
                    backupHint.xoriLeftParity(chunkDataArray[offset]);
                } else {
                    // else P'_j = P'_j ⊕ x
                    backupHint.xoriRightParity(chunkDataArray[offset]);
                }
            });
            // if some indices are missed, we need to fetch the corresponding elements
            for (int j = 0; j < chunkSize; j++) {
                if (!hitMap[j]) {
                    missingEntries.put(j + chunkSize * chunkId, chunkDataArray[j]);
                }
            }
            // send response
            DataPacketHeader streamResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(streamResponseHeader, new LinkedList<>()));
            extraInfo++;
        }
        // reset current query num
        currentQueryNum = 0;
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime, "Client handles " + chunkNum + " chunk");
    }

    @Override
    public byte[] pir(int x) throws MpcAbortException {
        setPtoInput(x);

        if (missingEntries.containsKey(x)) {
            return requestMissQuery(x);
        } else if (localCacheEntries.containsKey(x)) {
            return requestLocalQuery(x);
        } else {
            return requestActualQuery(x);
        }
    }

    private byte[] requestMissQuery(int x) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryRequestHeader, new LinkedList<>()));
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client requests miss query");

        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryResponsePayload = rpc.receive(queryResponseHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 0);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles miss response");

        logPhaseInfo(PtoState.PTO_END);
        return missingEntries.get(x);
    }

    private byte[] requestLocalQuery(int x) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // when client asks a query with x in cache, we make a dummy query, otherwise we would also leak information.
        SpamDirectPrimaryHint dummyPrimaryHint = new SpamDirectPrimaryHint(chunkSize, chunkNum, l, secureRandom);
        // we need to sample a dummy target ChunkID that are in the dummy primary hint
        int targetChunkId = -1;
        boolean success = false;
        while (!success) {
            targetChunkId = secureRandom.nextInt(chunkNum);
            success = dummyPrimaryHint.containsChunkId(targetChunkId);
        }
        BitVector bitVector = BitVectorFactory.createZeros(chunkNum);
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (chunkId != targetChunkId) {
                bitVector.set(chunkId, dummyPrimaryHint.containsChunkId(chunkId));
            }
        }
        byte[] bitVectorByteArray = bitVector.getBytes();
        int[] offsets = dummyPrimaryHint.expandOffset();
        // send the dummy punctured set to the server
        ByteBuffer offsetByteBuffer = ByteBuffer.allocate(Short.BYTES * chunkNum);
        for (int i = 0; i < chunkNum; i++) {
            offsetByteBuffer.putShort((short) offsets[i]);
        }
        List<byte[]> queryRequestPayload = new LinkedList<>();
        queryRequestPayload.add(bitVectorByteArray);
        queryRequestPayload.add(offsetByteBuffer.array());
        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryRequestHeader, queryRequestPayload));
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, queryTime, "Client requests dummy query");

        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryResponsePayload = rpc.receive(queryResponseHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 1);
        byte[] responseByteArray = queryResponsePayload.get(0);
        MpcAbortPreconditions.checkArgument(responseByteArray.length == byteL * 2);
        // ignore the result
        // we need to obtain value here, because local cache may be cleaned in preprocessing().
        byte[] value = localCacheEntries.get(x);
        currentQueryNum++;
        // when query num exceeds the maximum, rerun preprocessing.
        if (currentQueryNum >= roundQueryNum) {
            preprocessing();
        }
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles dummy response");

        return value;
    }

    private byte[] requestActualQuery(int x) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // client finds a primary hint that contains x
        int primaryHintId = -1;
        for (int i = 0; i < m1; i++) {
            if (primaryHints[i].contains(x)) {
                primaryHintId = i;
                break;
            }
        }
        // if still no hit set found, then fail.
        MpcAbortPreconditions.checkArgument(primaryHintId >= 0);
        // expand the set and compute the query
        SpamPrimaryHint primaryHint = primaryHints[primaryHintId];
        int targetChunkId = x / chunkSize;
        BitVector bitVector = BitVectorFactory.createZeros(chunkNum);
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            if (chunkId != targetChunkId) {
                bitVector.set(chunkId, primaryHint.containsChunkId(chunkId));
            }
        }
        // randomly shuffle the two sets
        boolean flip = secureRandom.nextBoolean();
        if (flip) {
            BitVector flipBitVector = BitVectorFactory.createOnes(chunkNum);
            bitVector.xori(flipBitVector);
        }
        byte[] bitVectorByteArray = bitVector.getBytes();
        // real subset S and dummy subset S' share the same offset vector r
        int[] offsets = primaryHint.expandOffset();
        // send the punctured set to the server
        ByteBuffer offsetByteBuffer = ByteBuffer.allocate(Short.BYTES * chunkNum);
        for (int i = 0; i < chunkNum; i++) {
            offsetByteBuffer.putShort((short) offsets[i]);
        }
        List<byte[]> queryRequestPayload = new LinkedList<>();
        queryRequestPayload.add(bitVectorByteArray);
        queryRequestPayload.add(offsetByteBuffer.array());
        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryRequestHeader, queryRequestPayload));
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, queryTime, "Client requests actual query");

        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryResponsePayload = rpc.receive(queryResponseHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 1);
        byte[] responseByteArray = queryResponsePayload.get(0);
        MpcAbortPreconditions.checkArgument(responseByteArray.length == byteL * 2);
        // pick the correct guess
        byte[] value = flip
            ? Arrays.copyOfRange(responseByteArray, byteL, byteL * 2)
            : Arrays.copyOfRange(responseByteArray, 0, byteL);
        // get value and update the local cache
        BytesUtils.xori(value, primaryHint.getParity());
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, responseTime, "Client handles actual response");

        stopWatch.start();
        // pick one backup hint
        MpcAbortPreconditions.checkArgument(backupHints.size() > 0);
        SpamBackupHint backupHint = backupHints.remove(0);
        // adds x to the set and adds the set to the local set list
        primaryHints[primaryHintId] = new SpamProgrammedPrimaryHint(backupHint, x, value);
        // add x to the local cache
        localCacheEntries.put(x, value);
        currentQueryNum++;
        stopWatch.stop();
        long refreshTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, refreshTime, "Client refreshes hint");

        // when query num exceeds the maximum, rerun preprocessing.
        if (currentQueryNum >= roundQueryNum) {
            preprocessing();
        }

        logPhaseInfo(PtoState.PTO_END);
        return value;
    }
}
