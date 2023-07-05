package edu.alibaba.mpc4j.s2pc.pcg.ct.blum82;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.commit.Commit;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ct.AbstractCoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ct.blum82.Blum82CoinTossPtoDesc.PtoStep;
import org.bouncycastle.crypto.Commitment;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Blum82 coin-tossing protocol sender.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public class Blum82CoinTossSender extends AbstractCoinTossParty {
    /**
     * commitment scheme
     */
    private final Commit commit;
    /**
     * hash
     */
    private final Hash hash;

    public Blum82CoinTossSender(Rpc senderRpc, Party receiverParty, Blum82CoinTossConfig config) {
        super(Blum82CoinTossPtoDesc.getInstance(), senderRpc, receiverParty, config);
        commit = CommitFactory.createInstance(envType, secureRandom);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);
        // empty init step
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] coinToss(int num, int bitLength) throws MpcAbortException {
        setPtoInput(num, bitLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // sender generates coins and sends the commitment.
        byte[][] senderCoins = IntStream.range(0, num)
            .mapToObj(index -> BytesUtils.randomByteArray(byteLength, bitLength, secureRandom))
            .toArray(byte[][]::new);
        byte[] flatSenderCoins = new byte[num * byteLength];
        for (int index = 0; index < num; index++) {
            System.arraycopy(senderCoins[index], 0, flatSenderCoins, index * byteLength, byteLength);
        }
        byte[] flatSenderCoinsHash = hash.digestToBytes(flatSenderCoins);
        Commitment senderCommit = commit.commit(flatSenderCoinsHash);
        List<byte[]> senderCommitmentPayload = new LinkedList<>();
        senderCommitmentPayload.add(senderCommit.getCommitment());
        DataPacketHeader senderCommitmentHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_COMMITMENT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderCommitmentHeader, senderCommitmentPayload));
        stopWatch.stop();
        long senderCommitmentTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, senderCommitmentTime, "Sender sends commitment");

        DataPacketHeader receiverCommitmentHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_COMMITMENT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiverCommitmentPayload = rpc.receive(receiverCommitmentHeader).getPayload();
        MpcAbortPreconditions.checkArgument(receiverCommitmentPayload.size() == 1);
        byte[] receiverCommitment = receiverCommitmentPayload.remove(0);

        stopWatch.start();
        // sender sends the sender's coins
        List<byte[]> senderCoinsPayload = new LinkedList<>();
        senderCoinsPayload.add(senderCommit.getSecret());
        senderCoinsPayload.addAll(Arrays.stream(senderCoins).collect(Collectors.toList()));
        DataPacketHeader senderCoinsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_COINS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderCoinsHeader, senderCoinsPayload));
        stopWatch.stop();
        long senderCoinsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, senderCoinsTime, "Sender sends coins");

        DataPacketHeader receiverCoinsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_COINS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiverCoinsPayload = rpc.receive(receiverCoinsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(receiverCoinsPayload.size() == num + 1);

        stopWatch.start();
        byte[] receiverSecret = receiverCoinsPayload.remove(0);
        // sender open the commitment
        byte[][] receiverCoins = receiverCoinsPayload.toArray(new byte[0][]);
        for (int index = 0; index < num; index++) {
            MpcAbortPreconditions.checkArgument(BytesUtils.isFixedReduceByteArray(receiverCoins[index], byteLength, bitLength));
        }
        byte[] flatReceiverCoins = new byte[num * byteLength];
        for (int index = 0; index < num; index++) {
            System.arraycopy(receiverCoins[index], 0, flatReceiverCoins, index * byteLength, byteLength);
        }
        byte[] flatReceiverCoinsHash = hash.digestToBytes(flatReceiverCoins);
        Commitment receiverCommit = new Commitment(receiverSecret, receiverCommitment);
        MpcAbortPreconditions.checkArgument(commit.isRevealed(flatReceiverCoinsHash, receiverCommit));
        // sender returns coins
        byte[][] coins = IntStream.range(0, num)
            .mapToObj(index -> BytesUtils.xor(senderCoins[index], receiverCoins[index]))
            .toArray(byte[][]::new);
        stopWatch.stop();
        long coinsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, coinsTime, "Sender computes coins");

        logPhaseInfo(PtoState.PTO_END);
        return coins;
    }
}
