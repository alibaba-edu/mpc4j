package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.phe.PheFactory;
import edu.alibaba.mpc4j.crypto.phe.impl.pai99.Pai99PheEngine;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.AbstractZlCoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15HeZlCoreMtgPtoDesc.PtoStep;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The receiver for DSZ15 HE-based Zl core multiplication triple generation protocol.
 *
 * @author Li Peng, Weiran Liu
 * @date 2023/2/20
 */
public class Dsz15HeZlCoreMtgReceiver extends AbstractZlCoreMtgParty {
    /**
     * the Paillier PHE scheme
     */
    private final Pai99PheEngine pai99PheEngine;
    /**
     * the PHE public key
     */
    private PhePublicKey pk;
    /**
     * plaintext bit length = 2l + 1 + σ
     */
    private int elementBitLength;
    /**
     * element module = {0, 1}^{2l + 1 + σ}
     */
    private BigInteger elementModule;
    /**
     * the maximal number of triples that can be packed into one PHE ciphertext
     */
    private int packetNum;
    /**
     * ciphertext byte length
     */
    private int ciphertextByteLength;
    /**
     * the number of ciphertext
     */
    private int ciphertextNum;
    /**
     * batch num = packetNum * ciphertextNum
     */
    private int batchNum;
    /**
     * a1
     */
    private BigInteger[] a1;
    /**
     * b1
     */
    private BigInteger[] b1;
    /**
     * c1 = a1 * b1 - r1
     */
    private BigInteger[] c1;

    public Dsz15HeZlCoreMtgReceiver(Rpc receiverRpc, Party senderParty, Dsz15HeZlCoreMtgConfig config) {
        super(Dsz15HeZlCoreMtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
        pai99PheEngine = new Pai99PheEngine(secureRandom);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // the receiver receives the HE public key.
        DataPacketHeader phePublicKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PHE_PUBLIC_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> phePublicKeyPayload = rpc.receive(phePublicKeyHeader).getPayload();

        stopWatch.start();
        // the receiver parses the HE public key.
        pk = PheFactory.phasePhePublicKey(phePublicKeyPayload);
        stopWatch.stop();
        long pheKeyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, pheKeyGenTime, "parse HE public key");

        stopWatch.start();
        // the modulus bit length in a PHE plaintexts. We need to subtract 1 so that all {0, 1}^l are valid plaintexts.
        int lBitLength = pk.getModulus().bitLength() - 1;
        // element bit length = 2l + 1 + σ
        elementBitLength = 2 * l + 1 + CommonConstants.STATS_BIT_LENGTH;
        // packet num.
        packetNum = (int) Math.floor((double) (lBitLength) / elementBitLength);
        MathPreconditions.checkPositive("maxPacketNum", packetNum);
        elementModule = BigInteger.ONE.shiftLeft(elementBitLength);
        // ciphertext byte length
        ciphertextByteLength = CommonUtils.getByteLength(pk.getCiphertextModulus().bitLength());
        stopWatch.stop();
        long paramsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, paramsTime, "calculate parameters");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ZlTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // calculate ciphertext num and batch num
        ciphertextNum = CommonUtils.getUnitNum(num, packetNum);
        batchNum = ciphertextNum * packetNum;
        initParams();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, initTime);

        // P_1 receives Enc_0(<a>_0), Enc_0(<b>_0)
        DataPacketHeader senderCiphertextHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CIPHERTEXT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderCiphertextPayload = rpc.receive(senderCiphertextHeader).getPayload();

        stopWatch.start();
        // P_1 calculates and sends d
        List<byte[]> receiverCiphertextPayload = generateReceiverCiphertextPayload(senderCiphertextPayload);
        DataPacketHeader receiverCiphertextHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CIPHERTEXT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverCiphertextHeader, receiverCiphertextPayload));
        stopWatch.stop();
        long operateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, operateTime);

        stopWatch.start();
        ZlTriple senderOutput = computeTriples();
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, tripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void initParams() {
        // P_1 generates <a>_1, <b>_1
        a1 = new BigInteger[batchNum];
        b1 = new BigInteger[batchNum];
        c1 = new BigInteger[batchNum];
        IntStream indexIntStream = IntStream.range(0, batchNum);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(arrayIndex -> {
            // Let P_1 randomly generate <a>_1, <b>_1
            a1[arrayIndex] = new BigInteger(l, secureRandom);
            b1[arrayIndex] = new BigInteger(l, secureRandom);
        });
    }

    private List<byte[]> generateReceiverCiphertextPayload(List<byte[]> senderCiphertextPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(senderCiphertextPayload.size() == batchNum * 2);
        BigInteger[] ciphertexts = senderCiphertextPayload.stream()
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        IntStream ciphertextIntStream = IntStream.range(0, ciphertextNum);
        ciphertextIntStream = parallel ? ciphertextIntStream.parallel() : ciphertextIntStream;
        return ciphertextIntStream
            .mapToObj(ciphertextIndex -> {
                // create a random r and calculates Enc(r)
                BigInteger rMask = BigInteger.ZERO;
                BigInteger[] rs = new BigInteger[packetNum];
                for (int i = packetNum - 1; i >= 0; i--) {
                    rs[i] = new BigInteger(elementBitLength, secureRandom);
                    if (i != (packetNum - 1)) {
                        rMask = rMask.shiftLeft(elementBitLength);
                    }
                    rMask = rMask.add(rs[i]);
                }
                BigInteger rMaskCiphertext = pai99PheEngine.rawEncrypt(pk, rMask);
                // Enc(d)
                BigInteger dCiphertext = pai99PheEngine.rawEncrypt(pk, BigInteger.ZERO);
                for (int i = packetNum - 1; i >= 0; i--) {
                    int index = ciphertextIndex * packetNum + i;
                    // Enc(<a>_0)
                    BigInteger a0Ciphertext = ciphertexts[2 * index];
                    // Enc(<b>_0)
                    BigInteger b0Ciphertext = ciphertexts[2 * index + 1];
                    // Enc(<a>_0) * b1
                    BigInteger a0b1 = pai99PheEngine.rawMultiply(pk, a0Ciphertext, b1[index]);
                    // Enc(<b>_0) * a1
                    BigInteger a1b0 = pai99PheEngine.rawMultiply(pk, b0Ciphertext, a1[index]);
                    // Enc(<a>_0) * b1 + Enc(<b>_0) * a1
                    BigInteger sum = pai99PheEngine.rawAdd(pk, a0b1, a1b0);
                    // <c>_1 = <a>_1 * <b>_1 - r mod 2^l
                    c1[index] = a1[index].multiply(b1[index]).subtract(rs[i]).and(mask);
                    if (i != (packetNum - 1)) {
                        // we do not need to shift for the first ciphertext
                        dCiphertext = pai99PheEngine.rawMultiply(pk, dCiphertext, elementModule);
                    }
                    dCiphertext = pai99PheEngine.rawAdd(pk, dCiphertext, sum);
                }
                return pai99PheEngine.rawAdd(pk, dCiphertext, rMaskCiphertext);
            }).map(ciphertext -> BigIntegerUtils.nonNegBigIntegerToByteArray(ciphertext, ciphertextByteLength))
            .collect(Collectors.toList());
    }

    private ZlTriple computeTriples() {
        // create and reduce triple
        ZlTriple zlTriple = ZlTriple.create(zl, batchNum, a1, b1, c1);
        zlTriple.reduce(num);
        return zlTriple;
    }
}
