package edu.alibaba.mpc4j.work.payable.pir.zlp24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.KeyPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirFactory;
import edu.alibaba.mpc4j.work.payable.pir.AbstractPayablePirClient;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.work.payable.pir.zlp24.Zlp24PayablePirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.work.payable.pir.zlp24.Zlp24PayablePirPtoDesc.getInstance;

/**
 * ZLP24 payable PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class Zlp24PayablePirClient extends AbstractPayablePirClient {

    /**
     * ecc
     */
    private final ByteFullEcc ecc;
    /**
     * keyword PIR client
     */
    private final KeyPirClient<ByteBuffer> kwPirClient;
    /**
     * β^{-1}
     */
    private BigInteger inverseBeta;

    public Zlp24PayablePirClient(Rpc clientRpc, Party serverParty, Zlp24PayablePirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        ecc = ByteEccFactory.createFullInstance(envType);
        kwPirClient = StdKsPirFactory.createClient(clientRpc, serverParty, config.getKsPirConfig());
        addSubPto(kwPirClient);
    }

    @Override
    public void init(int n, int byteL) throws MpcAbortException {
        setInitInput(n, byteL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        kwPirClient.init(n, byteL * Byte.SIZE, 1);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[] pir(ByteBuffer retrievalKey) throws MpcAbortException {
        setPtoInput(retrievalKey);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // run MP-OPRF
        stopWatch.start();
        byte[] entry = kwPirClient.pir(retrievalKey);
        stopWatch.stop();
        long kwPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, kwPirTime, "Client executes keyword PIR");

        stopWatch.start();
        List<byte[]> blindPayload = new ArrayList<>();
        blindPayload.add(entry == null ? IntUtils.intToByteArray(0) : IntUtils.intToByteArray(1));
        blindPayload.add(generateBlindPayload());
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_BLIND.ordinal(), blindPayload);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime, "Client executes OPRF");

        stopWatch.start();
        byte[] result = null;
        if (entry != null) {
            List<byte[]> blindPrfPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_BLIND_PRF.ordinal());
            MpcAbortPreconditions.checkArgument(blindPrfPayload.size() == 1);
            result = handleBlindPrf(blindPrfPayload.get(0), entry);
        }
        stopWatch.stop();
        long decodeResultTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, decodeResultTime, "Client decodes result");

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    private byte[] generateBlindPayload() {
        BigInteger beta = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        // generate blind factor
        inverseBeta = beta.modInverse(ecc.getN());
        // hash to point
        byte[] element = ecc.hashToCurve(retrievalKey.array());
        // blinding
        return ecc.mul(element, beta);
    }

    private byte[] handleBlindPrf(byte[] blindPrf, byte[] encryptedLabel) {
        byte[] elementPrf = ecc.mul(blindPrf, inverseBeta);
        Prg prg = PrgFactory.createInstance(envType, byteL);
        Hash hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] digest = hash.digestToBytes(elementPrf);
        byte[] extendedPrf = prg.extendToBytes(digest);
        return BytesUtils.xor(extendedPrf, encryptedLabel);
    }
}
