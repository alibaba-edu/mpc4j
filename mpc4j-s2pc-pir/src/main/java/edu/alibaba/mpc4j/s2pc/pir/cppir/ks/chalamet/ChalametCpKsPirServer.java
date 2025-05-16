package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.fusefilter.Arity3ByteFuseFilter;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.bobhash.BobIntHash;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.HintCpKsPirServer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet.ChalametCpKsPirDesc.*;

/**
 * Chalamet client-specific preprocessing KSPIR server.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class ChalametCpKsPirServer<T> extends AbstractCpKsPirServer<T> implements HintCpKsPirServer<T> {
    /**
     * database
     */
    private IntMatrix db;
    /**
     * filter length
     */
    private int filterLength;

    public ChalametCpKsPirServer(Rpc serverRpc, Party clientParty, ChalametCpKsPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(Map<T, byte[]> keyValueMap, int l, int matchBatchNum) throws MpcAbortException {
        setInitInput(keyValueMap, l, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // concat Hash(key) and value
        Hash hash = HashFactory.createInstance(envType, DIGEST_BYTE_L);
        Map<T, byte[]> keyValueConcatMap = new HashMap<>();
        keyValueMap.keySet().forEach(key -> {
            byte[] digest = hash.digestToBytes(ObjectUtils.objectToByteArray(key));
            keyValueConcatMap.put(key, Bytes.concat(digest, keyValueMap.get(key)));
        });
        Arity3ByteFuseFilter<T> fuseFilter = new Arity3ByteFuseFilter<>(
            envType, keyValueConcatMap, byteL + DIGEST_BYTE_L, secureRandom
        );
        sendOtherPartyPayload(
            PtoStep.SERVER_SEND_FUSE_FILTER_SEED.ordinal(), Collections.singletonList(fuseFilter.seed())
        );
        stopWatch.stop();
        long initFuseFilterTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initFuseFilterTime, "Server generates fuse filter");

        stopWatch.start();
        // random matrix A
        byte[] matrixSeed = BlockUtils.randomBlock(secureRandom);
        sendOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal(), Collections.singletonList(matrixSeed));
        filterLength = fuseFilter.filterLength();
        db = IntMatrix.createZeros(filterLength, byteL + DIGEST_BYTE_L);
        for (int i = 0; i < fuseFilter.storage().length; i++) {
            byte[] entry = fuseFilter.storage()[i];
            assert entry.length == byteL + DIGEST_BYTE_L;
            for (int j = 0; j < byteL + DIGEST_BYTE_L; j++) {
                db.set(i, j, entry[j] & 0xFF);
            }
        }
        // server runs M ← A · D, recall that A ∈ Z_q^{n×m}
        IntHash intHash = new BobIntHash();
        IntStream intStream = parallel ? IntStream.range(0, N).parallel() : IntStream.range(0, N);
        List<byte[]> hintPayload = intStream
            .mapToObj(i -> {
                int[] rowEntry = new int[filterLength];
                for (int j = 0; j < filterLength; j++) {
                    rowEntry[j] = intHash.hash(matrixSeed, i * filterLength + j);
                }
                IntVector row = IntVector.create(rowEntry);
                IntVector col = IntVector.createZeros(db.getColumns());
                for (int j = 0; j < db.getColumns(); j++) {
                    int[] entry = new int[filterLength];
                    for (int k = 0; k < filterLength; k++) {
                        entry[k] = db.get(k, j);
                    }
                    col.setElement(j, row.innerMul(IntVector.create(entry)));
                }
                return col.getElements();
            })
            .map(IntUtils::intArrayToByteArray)
            .toList();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal(), hintPayload);
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "Server generates hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            answer();
        }
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses query");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void answer() throws MpcAbortException {
        List<byte[]> clientQueryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == 1);
        // parse qu
        IntVector qu = IntVector.create(IntUtils.byteArrayToIntArray(clientQueryPayload.get(0)));
        MpcAbortPreconditions.checkArgument(qu.getNum() == filterLength);
        // generate response
        IntVector ans = db.leftMul(qu);
        List<byte[]> responsePayload = Collections.singletonList(IntUtils.intArrayToByteArray(ans.getElements()));
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }
}
