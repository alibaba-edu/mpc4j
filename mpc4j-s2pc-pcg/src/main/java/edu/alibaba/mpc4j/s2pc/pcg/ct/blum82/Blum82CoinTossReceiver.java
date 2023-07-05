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
 * Blum82 coin-tossing protocol receiver.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public class Blum82CoinTossReceiver extends AbstractCoinTossParty {
    /**
     * commitment scheme
     */
    private final Commit commit;
    /**
     * hash
     */
    private final Hash hash;

    public Blum82CoinTossReceiver(Rpc receiverRpc, Party senderParty, Blum82CoinTossConfig config) {
        super(Blum82CoinTossPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        // receiver generates coins and sends the commitment.
        byte[][] receiverCoins = IntStream.range(0, num)
            .mapToObj(index -> BytesUtils.randomByteArray(byteLength, bitLength, secureRandom))
            .toArray(byte[][]::new);
        byte[] flatReceiverCoins = new byte[num * byteLength];
        for (int index = 0; index < num; index++) {
            System.arraycopy(receiverCoins[index], 0, flatReceiverCoins, index * byteLength, byteLength);
        }
        byte[] flatReceiverCoinHash = hash.digestToBytes(flatReceiverCoins);
        Commitment receiverCommit = commit.commit(flatReceiverCoinHash);
        List<byte[]> receiverCommitmentPayload = new LinkedList<>();
        receiverCommitmentPayload.add(receiverCommit.getCommitment());
        DataPacketHeader receiverCommitmentHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_COMMITMENT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverCommitmentHeader, receiverCommitmentPayload));
        stopWatch.stop();
        long receiverCommitmentTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, receiverCommitmentTime, "Receiver sends commitment");

        DataPacketHeader senderCommitmentHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_COMMITMENT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderCommitmentPayload = rpc.receive(senderCommitmentHeader).getPayload();
        MpcAbortPreconditions.checkArgument(senderCommitmentPayload.size() == 1);
        byte[] senderCommitment = senderCommitmentPayload.remove(0);

        stopWatch.start();
        // receiver sends receiver's coins
        List<byte[]> receiverCoinsPayload = new LinkedList<>();
        receiverCoinsPayload.add(receiverCommit.getSecret());
        receiverCoinsPayload.addAll(Arrays.stream(receiverCoins).collect(Collectors.toList()));
        DataPacketHeader receiverCoinsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_COINS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverCoinsHeader, receiverCoinsPayload));
        stopWatch.stop();
        long receiverCoinsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, receiverCoinsTime, "Receiver sends coins");

        DataPacketHeader senderCoinsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_COINS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderCoinsPayload = rpc.receive(senderCoinsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(senderCoinsPayload.size() == num + 1);

        stopWatch.start();
        byte[] senderSecret = senderCoinsPayload.remove(0);
        // sender open the commitment
        byte[][] senderCoins = senderCoinsPayload.toArray(new byte[0][]);
        for (int index = 0; index < num; index++) {
            MpcAbortPreconditions.checkArgument(BytesUtils.isFixedReduceByteArray(senderCoins[index], byteLength, bitLength));
        }
        byte[] flatSenderCoins = new byte[num * byteLength];
        for (int index = 0; index < num; index++) {
            System.arraycopy(senderCoins[index], 0, flatSenderCoins, index * byteLength, byteLength);
        }
        byte[] senderCoinsHash = hash.digestToBytes(flatSenderCoins);
        Commitment senderCommit = new Commitment(senderSecret, senderCommitment);
        MpcAbortPreconditions.checkArgument(commit.isRevealed(senderCoinsHash, senderCommit));
        // sender returns coins
        byte[][] coins = IntStream.range(0, num)
            .mapToObj(index -> BytesUtils.xor(receiverCoins[index], senderCoins[index]))
            .toArray(byte[][]::new);
        stopWatch.stop();
        long coinsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, coinsTime, "Receiver computes coins");

        logPhaseInfo(PtoState.PTO_END);
        return coins;
    }
}
