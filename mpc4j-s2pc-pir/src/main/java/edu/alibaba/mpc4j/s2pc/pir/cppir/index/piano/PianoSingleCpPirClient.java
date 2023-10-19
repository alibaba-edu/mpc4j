package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractSingleCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleCpPirDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoBackupHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoDirectPrimaryHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoPrimaryHint;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoProgrammedPrimaryHint;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PIANO client-specific preprocessing PIR client.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoSingleCpPirClient extends AbstractSingleCpPirClient {
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
     * M2 (per group), the number of backup hints for each Chunk ID.
     */
    private int m2PerGroup;
    /**
     * primary hints
     */
    private PianoPrimaryHint[] primaryHints;
    /**
     * backup hint group
     */
    private ArrayList<ArrayList<PianoBackupHint>> backupHintGroup;
    /**
     * missing entries
     */
    private TIntObjectMap<byte[]> missingEntries;
    /**
     * local cache entries
     */
    private TIntObjectMap<byte[]> localCacheEntries;

    public PianoSingleCpPirClient(Rpc clientRpc, Party serverParty, PianoSingleCpPirConfig config) {
        super(PianoSingleCpPirDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int n, int l) throws MpcAbortException {
        setInitInput(n, l);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        chunkSize = PianoSingleCpPirUtils.getChunkSize(n);
        chunkNum = PianoSingleCpPirUtils.getChunkNum(n);
        assert chunkSize * chunkNum >= n
            : "chunkSize * chunkNum must be greater than or equal to n (" + n + "): " + chunkSize * chunkNum;
        roundQueryNum = PianoSingleCpPirUtils.getRoundQueryNum(n);
        m1 = PianoSingleCpPirUtils.getM1(n);
        m2PerGroup = PianoSingleCpPirUtils.getM2PerGroup(n);
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paramTime,
            String.format(
                "Client sets params: n = %d, ChunkSize = %d, ChunkNum = %d, n (pad) = %d, Q = %d, M1 = %d, M2 (per group) = %d",
                n, chunkSize, chunkNum, chunkSize * chunkNum, roundQueryNum, m1, m2PerGroup
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // init primary hints and backup hints data structure
        IntStream primaryHintIntStream = IntStream.range(0, m1);
        primaryHintIntStream = parallel ? primaryHintIntStream.parallel() : primaryHintIntStream;
        primaryHints = primaryHintIntStream
            .mapToObj(index -> new PianoDirectPrimaryHint(chunkSize, chunkNum, l, secureRandom))
            .toArray(PianoPrimaryHint[]::new);
        IntStream backupHintGroupIntStream = IntStream.range(0, chunkNum);
        backupHintGroupIntStream = parallel ? backupHintGroupIntStream.parallel() : backupHintGroupIntStream;
        backupHintGroup = backupHintGroupIntStream
            .mapToObj(chunkId ->
                IntStream.range(0, m2PerGroup)
                    .mapToObj(index -> new PianoBackupHint(chunkSize, chunkNum, l, chunkId, secureRandom))
                    .collect(Collectors.toCollection(ArrayList::new))
            )
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
            // receive stream request
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
            // update the parity for the primary hints
            // hitMap is irrelevant to the scheme. We want to know if any indices are missed.
            boolean[] hitMap = new boolean[chunkSize];
            primaryHintIntStream = IntStream.range(0, m1);
            primaryHintIntStream = parallel ? primaryHintIntStream.parallel() : primaryHintIntStream;
            primaryHintIntStream.forEach(primaryHintIndex -> {
                PianoPrimaryHint primaryHint = primaryHints[primaryHintIndex];
                int offset = primaryHint.expandOffset(finalChunkId);
                hitMap[offset] = true;
                // XOR parity
                primaryHint.xori(chunkDataArray[offset]);
            });
            // update the parity for the backup hints
            backupHintGroupIntStream = IntStream.range(0, chunkNum);
            backupHintGroupIntStream = parallel ? backupHintGroupIntStream.parallel() : backupHintGroupIntStream;
            backupHintGroupIntStream.forEach(backupHintGroupIndex -> {
                // we need to ignore the group for the chunk ID.
                if (backupHintGroupIndex != finalChunkId) {
                    ArrayList<PianoBackupHint> backupHints = backupHintGroup.get(backupHintGroupIndex);
                    for (PianoBackupHint backupHint : backupHints) {
                        int offset = backupHint.expandOffset(finalChunkId);
                        backupHint.xori(chunkDataArray[offset]);
                    }
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
            return requestCacheQuery(x);
        } else {
            return requestActualQuery(x);
        }
    }

    private byte[] requestMissQuery(int x) throws MpcAbortException {
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

    private byte[] requestCacheQuery(int x) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // when client asks a query with x in cache, we make a dummy query, otherwise we would also leak information.
        ByteBuffer queryByteBuffer = ByteBuffer.allocate(Short.BYTES * (chunkNum - 1));
        for (int i = 0; i < chunkNum - 1; i++) {
            queryByteBuffer.putShort((short) secureRandom.nextInt(chunkSize));
        }
        List<byte[]> queryRequestPayload = Collections.singletonList(queryByteBuffer.array());
        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryRequestHeader, queryRequestPayload));
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client requests dummy query");

        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryResponsePayload = rpc.receive(queryResponseHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 1);
        byte[] responseByteArray = queryResponsePayload.get(0);
        MpcAbortPreconditions.checkArgument(responseByteArray.length == byteL * chunkNum);
        // ignore the result
        // we need to obtain value here, because local cache may be cleaned in preprocessing().
        byte[] value = localCacheEntries.get(x);
        currentQueryNum++;
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles dummy response");

        // when query num exceeds the maximum, rerun preprocessing.
        if (currentQueryNum >= roundQueryNum) {
            preprocessing();
        }

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
        // expand the set
        PianoPrimaryHint primaryHint = primaryHints[primaryHintId];
        int[] offsets = primaryHint.expandOffset();
        int[] puncturedOffsets = new int[chunkNum - 1];
        // puncture the set by removing x from the offset vector
        int puncturedChunkId = x / chunkSize;
        for (int i = 0; i < chunkNum; i++) {
            if (i < puncturedChunkId) {
                puncturedOffsets[i] = offsets[i];
            } else if (i == puncturedChunkId) {
                // skip the punctured chunk ID
            } else {
                puncturedOffsets[i - 1] = offsets[i];
            }
        }
        // send the punctured set to the server
        ByteBuffer queryByteBuffer = ByteBuffer.allocate(Short.BYTES * (chunkNum - 1));
        for (int i = 0; i < chunkNum - 1; i++) {
            queryByteBuffer.putShort((short) puncturedOffsets[i]);
        }
        List<byte[]> queryRequestPayload = Collections.singletonList(queryByteBuffer.array());
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
        MpcAbortPreconditions.checkArgument(responseByteArray.length == byteL * chunkNum);
        // pick the correct guess
        byte[] value = Arrays.copyOfRange(responseByteArray, puncturedChunkId * byteL, (puncturedChunkId + 1) * byteL);
        // get value and update the local cache
        BytesUtils.xori(value, primaryHint.getParity());
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, responseTime, "Client handles actual response");

        stopWatch.start();
        // pick one set from the Chunk ID group.
        ArrayList<PianoBackupHint> backupHints = backupHintGroup.get(puncturedChunkId);
        MpcAbortPreconditions.checkArgument(backupHints.size() > 0);
        PianoBackupHint backupHint = backupHints.remove(0);
        // adds x to the set and adds the set to the local set list
        primaryHints[primaryHintId] = new PianoProgrammedPrimaryHint(backupHint, x, value);
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
