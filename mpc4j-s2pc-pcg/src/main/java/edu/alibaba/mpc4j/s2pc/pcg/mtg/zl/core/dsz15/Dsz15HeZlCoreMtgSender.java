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
import edu.alibaba.mpc4j.crypto.phe.params.PheKeyGenParams;
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.AbstractZlCoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15HeZlCoreMtgPtoDesc.PtoStep;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The sender for DSZ15 HE-based Zl core multiplication triple generation protocol.
 *
 * @author Li Peng, Weiran Liu
 * @date 2023/2/20
 */
public class Dsz15HeZlCoreMtgSender extends AbstractZlCoreMtgParty {
    /**
     * the Paillier PHE scheme
     */
    private final Pai99PheEngine pai99PheEngine;
    /**
     * the PHE secret key
     */
    private PhePrivateKey sk;
    /**
     * element bit length = 2l + 1 + σ
     */
    private int elementBitLength;
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
     * a0
     */
    private BigInteger[] a0;
    /**
     * b0
     */
    private BigInteger[] b0;

    public Dsz15HeZlCoreMtgSender(Rpc senderRpc, Party receiverParty, Dsz15HeZlCoreMtgConfig config) {
        super(Dsz15HeZlCoreMtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        pai99PheEngine = new Pai99PheEngine(secureRandom);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // the sender generates the HE public key to the receiver.
        pai99PheEngine.getPheType();
        int precision = PheFactory.getModulusBitLength(PheFactory.PheType.PAI99, Dsz15HeZlCoreMtgPtoDesc.PHE_SEC_LEVEL);
        PheKeyGenParams pheKeyGenParams = new PheKeyGenParams(Dsz15HeZlCoreMtgPtoDesc.PHE_SEC_LEVEL, false, precision);
        sk = pai99PheEngine.keyGen(pheKeyGenParams);
        PhePublicKey pk = sk.getPublicKey();
        // the sender sends the HE public key to the receiver
        List<byte[]> phePublicKeyPayload = pk.toByteArrayList();
        DataPacketHeader phePublicKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PHE_PUBLIC_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(phePublicKeyHeader, phePublicKeyPayload));
        stopWatch.stop();
        long pheKeyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, pheKeyGenTime, "send PHE public key");

        stopWatch.start();
        // the modulus bit length in a PHE plaintexts. We need to subtract 1 so that all {0, 1}^l are valid plaintexts.
        int lBitLength = pk.getModulus().bitLength() - 1;
        // element bit length = 2l + 1 + σ
        elementBitLength = 2 * l + 1 + CommonConstants.STATS_BIT_LENGTH;
        // packet num.
        packetNum = (int) Math.floor((double) (lBitLength) / elementBitLength);
        MathPreconditions.checkPositive("maxPacketNum", packetNum);
        // ciphertext byte length
        ciphertextByteLength = CommonUtils.getByteLength(pk.getCiphertextModulus().bitLength());
        stopWatch.stop();
        long paramsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, paramsTime, "calculate parameters");

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
        // P_0 sends Enc_0(<a>_0), Enc_0(<b>_0)
        List<byte[]> senderCiphertextPayload = generateSenderCiphertextPayload();
        DataPacketHeader senderCiphertextHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CIPHERTEXT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderCiphertextHeader, senderCiphertextPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, encTime);

        // P_0 receives d
        DataPacketHeader receiverCiphertextHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CIPHERTEXT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiverCiphertextPayload = rpc.receive(receiverCiphertextHeader).getPayload();

        stopWatch.start();
        ZlTriple senderOutput = computeTriples(receiverCiphertextPayload);
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, tripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateSenderCiphertextPayload() {
        // P_0 generates <a>_0, <b>_0
        a0 = new BigInteger[batchNum];
        b0 = new BigInteger[batchNum];
        IntStream indexIntStream = IntStream.range(0, batchNum);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
            .mapToObj(arrayIndex -> {
                // Let P_0 randomly generate <a>_0, <b>_0
                a0[arrayIndex] = new BigInteger(l, secureRandom);
                b0[arrayIndex] = new BigInteger(l, secureRandom);
                // Enc(<a>_0), Enc(<b>_0)
                BigInteger[] ciphertextPair = new BigInteger[2];
                ciphertextPair[0] = pai99PheEngine.rawEncrypt(sk, a0[arrayIndex]);
                ciphertextPair[1] = pai99PheEngine.rawEncrypt(sk, b0[arrayIndex]);
                return ciphertextPair;
            })
            .flatMap(Arrays::stream)
            .map(ciphertext -> BigIntegerUtils.nonNegBigIntegerToByteArray(ciphertext, ciphertextByteLength))
            .collect(Collectors.toList());
    }

    private ZlTriple computeTriples(List<byte[]> receiverCiphertextPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverCiphertextPayload.size() == ciphertextNum);
        BigInteger[] ciphertexts = receiverCiphertextPayload.stream()
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        // decrypt the ciphertext
        BigInteger[] c0 = new BigInteger[batchNum];
        IntStream ciphertextIntStream = IntStream.range(0, ciphertextNum);
        ciphertextIntStream = parallel ? ciphertextIntStream.parallel() : ciphertextIntStream;
        ciphertextIntStream.forEach(ciphertextIndex -> {
            BigInteger ciphertext = ciphertexts[ciphertextIndex];
            BigInteger plaintext = pai99PheEngine.rawDecrypt(sk, ciphertext);
            for (int i = 0; i < packetNum; i++) {
                c0[ciphertextIndex * packetNum + i] = plaintext.and(mask);
                plaintext = plaintext.shiftRight(elementBitLength);
            }
        });
        // <C>_0 = <a>_0 * <b>_0 + d
        IntStream.range(0, batchNum).forEach(index -> c0[index] = a0[index].multiply(b0[index]).add(c0[index]).and(mask));
        // create and reduce triple
        ZlTriple zlTriple = ZlTriple.create(zl, batchNum, a0, b0, c0);
        zlTriple.reduce(num);
        return zlTriple;
    }
}
