package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.fusefilter.Arity3ByteFusePosition;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirDesc.*;

/**
 * Simple naive client-specific preprocessing KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SimpleNaiveCpKsPirClient<T> extends AbstractCpKsPirClient<T> {
    /**
     * simple index PIR client
     */
    private final SimpleCpIdxPirClient simpleCpIdxPirClient;
    /**
     * fuse position
     */
    private Arity3ByteFusePosition<T> arity3ByteFusePosition;
    /**
     * hash
     */
    private Hash hash;

    public SimpleNaiveCpKsPirClient(Rpc clientRpc, Party serverParty, SimpleNaiveCpKsPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        SimpleCpIdxPirConfig simpleCpIdxPirConfig = new SimpleCpIdxPirConfig.Builder(gaussianLweParam).build();
        simpleCpIdxPirClient = new SimpleCpIdxPirClient(clientRpc, serverParty, simpleCpIdxPirConfig);
        addSubPto(simpleCpIdxPirClient);
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        hash = HashFactory.createInstance(envType, DIGEST_BYTE_L);
        List<byte[]> fuseFilterSeedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_FUSE_FILTER_SEED.ordinal());
        MpcAbortPreconditions.checkArgument(fuseFilterSeedPayload.size() == 1);
        byte[] fuseFilterSeed = fuseFilterSeedPayload.get(0);
        arity3ByteFusePosition = new Arity3ByteFusePosition<>(envType, n, byteL + DIGEST_BYTE_L, fuseFilterSeed);
        int filterLength = arity3ByteFusePosition.filterLength();
        stopWatch.stop();
        long fusePosTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, fusePosTime, "Client generates fuse position");

        stopWatch.start();
        simpleCpIdxPirClient.init(filterLength, (byteL + DIGEST_BYTE_L) * Byte.SIZE, arity3ByteFusePosition.arity() * maxBatchNum);
        stopWatch.stop();
        long initSimplePirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initSimplePirTime, "Client initializes Simple PIR");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(ArrayList<T> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(keys.get(i));
        }
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = decode(keys.get(i));
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private void query(T key) {
        int[] xs = arity3ByteFusePosition.positions(key);
        for (int i = 0; i < arity3ByteFusePosition.arity(); i++) {
            simpleCpIdxPirClient.query(xs[i]);
        }
    }

    private byte[] decode(T key) throws MpcAbortException {
        int[] xs = arity3ByteFusePosition.positions(key);
        byte[][] entries = new byte[arity3ByteFusePosition.arity()][];
        entries[0] = simpleCpIdxPirClient.recover(xs[0]);
        for (int i = 1; i < arity3ByteFusePosition.arity(); i++) {
            entries[i] = simpleCpIdxPirClient.recover(xs[i]);
            addi(entries[0], entries[i], byteL + DIGEST_BYTE_L);
        }
        byte[] actualDigest = BytesUtils.clone(entries[0], 0, DIGEST_BYTE_L);
        byte[] expectDigest = hash.digestToBytes(ObjectUtils.objectToByteArray(key));
        if (BytesUtils.equals(actualDigest, expectDigest)) {
            return BytesUtils.clone(entries[0], DIGEST_BYTE_L, byteL);
        } else {
            return null;
        }
    }

    private void addi(byte[] p, byte[] q, int byteLength) {
        assert p.length == byteLength && q.length == byteLength;
        for (int i = 0; i < byteLength; i++) {
            p[i] += q[i];
        }
    }
}
