package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.lll24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24.Lll24BstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.AbstractPstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LLL24 PST receiver
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public class Lll24PstReceiver extends AbstractPstReceiver implements PstReceiver {
    /**
     * LLL24 BST receiver
     */
    private final Lll24BstReceiver bstReceiver;
    /**
     * LLL24 BST receiver
     */
    private final CotSender cotSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Lll24PstReceiver(Rpc receiverRpc, Party senderParty, Lll24PstConfig config) {
        super(Lll24PstPtoDesc.getInstance(), receiverRpc, senderParty, config);
        bstReceiver = new Lll24BstReceiver(receiverRpc, senderParty, config.getBstConfig());
        addSubPto(bstReceiver);
        cotSender = CotFactory.createSender(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotSender);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        bstReceiver.init();
        cotSender.init(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength, boolean isLeft) throws MpcAbortException {
        setPtoInput(batchNum, eachNum, byteLength);
        CotSenderOutput cotSenderOutput = cotSender.sendRandom(cotNum);
        return shareTranslate(batchNum, eachNum, byteLength, cotSenderOutput, isLeft);
    }

    @Override
    public BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength,
                                            CotSenderOutput preSenderOutput, boolean isLeft) throws MpcAbortException {
        if (preSenderOutput == null) {
            return shareTranslate(batchNum, eachNum, byteLength, isLeft);
        }
        setPtoInput(batchNum, eachNum, byteLength, preSenderOutput);
        this.cotSenderOutput = preSenderOutput;
        // 改变传入的 preSenderOutput，使其填充为所需要的长度
        int extendLen = BstFactory.getPrecomputeNum(config.getBstConfig(), batchNum, eachNum);
        byte[] delta = cotSenderOutput.getDelta();
        byte[][] r0New = new byte[extendLen][];
        if (eachNum == 2) {
            assert extendLen == cotSenderOutput.getNum() * 2;
            IntStream intStream = parallel ? IntStream.range(0, cotSenderOutput.getNum()).parallel() : IntStream.range(0, cotSenderOutput.getNum());
            intStream.forEach(i -> {
                r0New[i * 2] = cotSenderOutput.getR0(i);
                r0New[i * 2 + 1] = cotSenderOutput.getR1(i);
            });
        } else {
            assert extendLen == cotSenderOutput.getNum() + (eachNum / 2 - 1) * batchNum + (eachNum / 4 - 1) * batchNum;
            int logEachNum = LongUtils.ceilLog2(eachNum);
            int afterEachGroupOt = (eachNum - 1) * logEachNum;
            int beforeEachGroupOt = afterEachGroupOt - (eachNum / 2 - 1) - (eachNum / 4 - 1);
            IntStream intStream = parallel ? IntStream.range(0, batchNum).parallel() : IntStream.range(0, batchNum);
            if (isLeft) {
                // 相邻的两个PPRF，choice的最低位不同；并且相邻四个数的倒数第二位的choice只有2^3中选择
                intStream.forEach(i -> {
                    int srcStartIndex = i * beforeEachGroupOt;
                    int destStartIndex = i * afterEachGroupOt;
                    for (int rowIndex = 0; rowIndex < eachNum - 1; rowIndex++) {
                        switch (rowIndex % 4) {
                            case 0, 2:
                                System.arraycopy(cotSenderOutput.getR0Array(), srcStartIndex, r0New, destStartIndex, logEachNum);
                                srcStartIndex += logEachNum;
                                break;
                            case 1:
                                System.arraycopy(cotSenderOutput.getR0Array(), srcStartIndex, r0New, destStartIndex, logEachNum - 2);
                                r0New[destStartIndex - 2 + logEachNum] = BytesUtils.xor(cotSenderOutput.getR0(srcStartIndex - 2 + logEachNum), r0New[destStartIndex - 2]);
                                r0New[destStartIndex - 1 + logEachNum] = cotSenderOutput.getR1(srcStartIndex - 1);
                                srcStartIndex += logEachNum - 1;
                                break;
                            case 3:
                                System.arraycopy(cotSenderOutput.getR0Array(), srcStartIndex, r0New, destStartIndex, logEachNum - 2);
                                r0New[destStartIndex - 2 + logEachNum] = BytesUtils.xor(cotSenderOutput.getR0(srcStartIndex - 2 - logEachNum), r0New[destStartIndex - 2]);
                                r0New[destStartIndex - 1 + logEachNum] = cotSenderOutput.getR1(srcStartIndex - 1);
                                srcStartIndex += logEachNum - 2;
                        }
                        destStartIndex += logEachNum;
                    }
                    assert srcStartIndex == (i + 1) * beforeEachGroupOt;
                    assert destStartIndex == (i + 1) * afterEachGroupOt;
                });
            } else {
                int halfNum = eachNum / 2;
                int quarterNum = eachNum / 4;
                // 相隔为eachNum / 2的两个PPRF，choice的最高位不同
                intStream.forEach(i -> {
                    int srcStartIndex = i * beforeEachGroupOt;
                    byte[][] firstBitRes = new byte[halfNum][];
                    byte[][] secondBitRes = new byte[halfNum][];
                    byte[][] twoSwitchXorRes = new byte[quarterNum][];
                    for (int rowIndex = 0; rowIndex < eachNum - 1; rowIndex++) {
                        int destStartIndex = i * afterEachGroupOt + rowIndex * logEachNum;
                        switch (rowIndex / quarterNum) {
                            case 0:
                            case 1:
                                firstBitRes[rowIndex] = cotSenderOutput.getR1(srcStartIndex);
                                secondBitRes[rowIndex] = cotSenderOutput.getR0(srcStartIndex + 1);

                                System.arraycopy(cotSenderOutput.getR0Array(), srcStartIndex, r0New, destStartIndex, logEachNum);
                                srcStartIndex += logEachNum;
                                break;
                            case 2:
                                twoSwitchXorRes[rowIndex - halfNum] = cotSenderOutput.getR0(srcStartIndex);
                                srcStartIndex++;
                            case 3:
                                int halfSearchIndex = rowIndex - halfNum;
                                int quarterSearchIndex = rowIndex >= 3 * quarterNum ? rowIndex - 3 * quarterNum : rowIndex - halfNum;
                                r0New[destStartIndex++] = firstBitRes[halfSearchIndex];
                                r0New[destStartIndex++] = BytesUtils.xor(secondBitRes[halfSearchIndex], twoSwitchXorRes[quarterSearchIndex]);

                                System.arraycopy(cotSenderOutput.getR0Array(), srcStartIndex, r0New, destStartIndex, logEachNum - 2);
                                srcStartIndex += logEachNum - 2;
                                break;
                        }
                    }
                    assert srcStartIndex == (i + 1) * beforeEachGroupOt;
                });
            }
        }
        cotSenderOutput = null;
        return bstReceiver.shareTranslate(batchNum, eachNum, byteLength, CotSenderOutput.create(delta, r0New));
    }
}
