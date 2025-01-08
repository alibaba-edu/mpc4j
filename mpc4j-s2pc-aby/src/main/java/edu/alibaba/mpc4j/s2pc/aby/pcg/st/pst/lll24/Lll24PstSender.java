package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.lll24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.lll24.Lll24BstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.AbstractPstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LLL24 PST sender
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public class Lll24PstSender extends AbstractPstSender implements PstSender {
    /**
     * LLL24 BST receiver
     */
    private final Lll24BstSender bstSender;
    /**
     * LLL24 BST receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * COT sender output
     */
    private CotReceiverOutput cotReceiverOutput;

    public Lll24PstSender(Rpc senderRpc, Party receiverParty, Lll24PstConfig config) {
        super(Lll24PstPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bstSender = new Lll24BstSender(senderRpc, receiverParty, config.getBstConfig());
        addSubPto(bstSender);
        cotReceiver = CotFactory.createReceiver(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotReceiver);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bstSender.init();
        cotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BstSenderOutput shareTranslate(int[][] piArray, int byteLength, boolean isLeft) throws MpcAbortException {
        setPtoInput(piArray, byteLength);
        CotReceiverOutput cotSenderOutput = cotReceiver.receiveRandom(cotNum);
        return shareTranslate(piArray, byteLength, cotSenderOutput, isLeft);
    }

    @Override
    public BstSenderOutput shareTranslate(int[][] piArray, int byteLength,
                                          CotReceiverOutput preReceiverOutput, boolean isLeft) throws MpcAbortException {
        if (preReceiverOutput == null) {
            return shareTranslate(piArray, byteLength, isLeft);
        }
        setPtoInput(piArray, byteLength, preReceiverOutput);
        this.cotReceiverOutput = preReceiverOutput;
        // 改变传入的 preSenderOutput，使其填充为所需要的长度
        int extendLen = BstFactory.getPrecomputeNum(config.getBstConfig(), batchNum, eachNum);
        boolean[] flag = new boolean[extendLen];
        byte[][] resByte = new byte[extendLen][];
        if (eachNum == 2) {
            assert extendLen == cotReceiverOutput.getNum() * 2;
            IntStream intStream = parallel ? IntStream.range(0, cotReceiverOutput.getNum()).parallel() : IntStream.range(0, cotReceiverOutput.getNum());
            intStream.forEach(i -> {
                flag[i * 2] = cotReceiverOutput.getChoice(i);
                flag[i * 2 + 1] = !cotReceiverOutput.getChoice(i);
                resByte[i * 2] = cotReceiverOutput.getRb(i);
                resByte[i * 2 + 1] = BytesUtils.clone(resByte[i * 2]);
            });
        } else {
            assert extendLen == cotReceiverOutput.getNum() + (eachNum / 2 - 1) * batchNum + (eachNum / 4 - 1) * batchNum;
            int logEachNum = LongUtils.ceilLog2(eachNum);
            int afterEachGroupOt = (eachNum - 1) * logEachNum;
            int beforeEachGroupOt = afterEachGroupOt - (eachNum / 2 - 1) - (eachNum / 4 - 1);
            IntStream intStream = parallel ? IntStream.range(0, batchNum).parallel() : IntStream.range(0, batchNum);
            if (isLeft) {
                // 相邻的两个PPRF，choice的最低位不同
                intStream.forEach(i -> {
                    int srcStartIndex = i * beforeEachGroupOt;
                    int destStartIndex = i * afterEachGroupOt;
                    for (int rowIndex = 0; rowIndex < eachNum - 1; rowIndex++) {
                        switch (rowIndex % 4) {
                            case 0:
                                // 判断 (0 == 2) == (1 == 3)
                                assert ((piArray[i][rowIndex] & 2) == (piArray[i][rowIndex + 2] & 2))
                                    == ((piArray[i][rowIndex + 1] & 2) == (piArray[i][rowIndex + 3] & 2));
                                assert (piArray[i][rowIndex] & 1) != (piArray[i][rowIndex + 1] & 1);
                                assert (piArray[i][rowIndex + 2] & 1) != (piArray[i][rowIndex + 3] & 1);
                            case 2:
                                System.arraycopy(cotReceiverOutput.getChoices(), srcStartIndex, flag, destStartIndex, logEachNum);
                                System.arraycopy(cotReceiverOutput.getRbArray(), srcStartIndex, resByte, destStartIndex, logEachNum);
                                srcStartIndex += logEachNum;
                                break;
                            case 1:
                                System.arraycopy(cotReceiverOutput.getChoices(), srcStartIndex, flag, destStartIndex, logEachNum - 2);
                                System.arraycopy(cotReceiverOutput.getRbArray(), srcStartIndex, resByte, destStartIndex, logEachNum - 2);
                                flag[destStartIndex - 2 + logEachNum] = cotReceiverOutput.getChoice(srcStartIndex - 2 + logEachNum) ^ flag[destStartIndex - 2];
                                resByte[destStartIndex - 2 + logEachNum] = BytesUtils.xor(cotReceiverOutput.getRb(srcStartIndex - 2 + logEachNum), resByte[destStartIndex - 2]);
                                flag[destStartIndex + logEachNum - 1] = !cotReceiverOutput.getChoice(srcStartIndex - 1);
                                resByte[destStartIndex + logEachNum - 1] = BytesUtils.clone(cotReceiverOutput.getRb(srcStartIndex - 1));
                                srcStartIndex += logEachNum - 1;
                                break;
                            case 3:
                                System.arraycopy(cotReceiverOutput.getChoices(), srcStartIndex, flag, destStartIndex, logEachNum - 2);
                                System.arraycopy(cotReceiverOutput.getRbArray(), srcStartIndex, resByte, destStartIndex, logEachNum - 2);
                                flag[destStartIndex - 2 + logEachNum] = cotReceiverOutput.getChoice(srcStartIndex - 2 - logEachNum) ^ flag[destStartIndex - 2];
                                resByte[destStartIndex - 2 + logEachNum] = BytesUtils.xor(cotReceiverOutput.getRb(srcStartIndex - 2 - logEachNum), resByte[destStartIndex - 2]);
                                flag[destStartIndex + logEachNum - 1] = !cotReceiverOutput.getChoice(srcStartIndex - 1);
                                resByte[destStartIndex - 1 + logEachNum] = BytesUtils.clone(cotReceiverOutput.getRb(srcStartIndex - 1));
                                srcStartIndex += logEachNum - 2;
                        }
                        destStartIndex += logEachNum;
                    }
                    assert srcStartIndex == (i + 1) * beforeEachGroupOt;
                    assert destStartIndex == (i + 1) * afterEachGroupOt;
                });
            } else {
                int andNum = 1 << (logEachNum - 2);
                int halfNum = eachNum / 2;
                int quarterNum = eachNum / 4;
                // 相隔为eachNum / 2的两个PPRF，choice的最高位不同
                intStream.forEach(i -> {
                    int srcStartIndex = i * beforeEachGroupOt;
                    byte[][] firstBitRes = new byte[halfNum][];
                    byte[][] secondBitRes = new byte[halfNum][];
                    boolean[] firstBitChoice = new boolean[halfNum];
                    boolean[] secondBitChoice = new boolean[halfNum];
                    boolean[] twoSwitchChoice = new boolean[quarterNum];
                    byte[][] twoSwitchXorRes = new byte[quarterNum][];
                    for (int rowIndex = 0; rowIndex < eachNum - 1; rowIndex++) {
                        int destStartIndex = i * afterEachGroupOt + rowIndex * logEachNum;
                        switch (rowIndex / quarterNum) {
                            case 0:
                                // 判断 (0 == 2) == (1 == 3)
                                assert ((piArray[i][rowIndex] & andNum) == (piArray[i][rowIndex + quarterNum] & andNum))
                                    == ((piArray[i][rowIndex + halfNum] & andNum) == (piArray[i][rowIndex + 3 * quarterNum] & andNum));
                                assert (piArray[i][rowIndex] >= halfNum) != (piArray[i][rowIndex + halfNum] >= halfNum);
                            case 1:
                                firstBitChoice[rowIndex] = !cotReceiverOutput.getChoice(srcStartIndex);
                                firstBitRes[rowIndex] = BytesUtils.clone(cotReceiverOutput.getRb(srcStartIndex));
                                secondBitChoice[rowIndex] = cotReceiverOutput.getChoice(srcStartIndex + 1);
                                secondBitRes[rowIndex] = BytesUtils.clone(cotReceiverOutput.getRb(srcStartIndex + 1));

                                System.arraycopy(cotReceiverOutput.getChoices(), srcStartIndex, flag, destStartIndex, logEachNum);
                                System.arraycopy(cotReceiverOutput.getRbArray(), srcStartIndex, resByte, destStartIndex, logEachNum);
                                srcStartIndex += logEachNum;
                                break;
                            case 2:
                                assert (piArray[i][rowIndex] >= halfNum) != (piArray[i][rowIndex - halfNum] >= halfNum);
                                twoSwitchXorRes[rowIndex - halfNum] = cotReceiverOutput.getRb(srcStartIndex);
                                twoSwitchChoice[rowIndex - halfNum] = cotReceiverOutput.getChoice(srcStartIndex);
                                srcStartIndex++;
                            case 3:
                                int halfSearchIndex = rowIndex - halfNum;
                                int quarterSearchIndex = rowIndex >= 3 * quarterNum ? rowIndex - 3 * quarterNum : rowIndex - halfNum;
                                flag[destStartIndex] = firstBitChoice[halfSearchIndex];
                                resByte[destStartIndex++] = firstBitRes[halfSearchIndex];
                                flag[destStartIndex] = secondBitChoice[halfSearchIndex] ^ twoSwitchChoice[quarterSearchIndex];
                                resByte[destStartIndex++] = BytesUtils.xor(secondBitRes[halfSearchIndex], twoSwitchXorRes[quarterSearchIndex]);

                                System.arraycopy(cotReceiverOutput.getChoices(), srcStartIndex, flag, destStartIndex, logEachNum - 2);
                                System.arraycopy(cotReceiverOutput.getRbArray(), srcStartIndex, resByte, destStartIndex, logEachNum - 2);
                                srcStartIndex += logEachNum - 2;
                                break;
                        }
                    }
                    assert srcStartIndex == (i + 1) * beforeEachGroupOt;
                });
            }
        }
        cotReceiverOutput = null;
        return bstSender.shareTranslate(piArray, byteLength, CotReceiverOutput.create(flag, resByte));
    }
}
