package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.AbstractThreePartyOpfPto;

import java.security.SecureRandom;

/**
 * the abstract soprp party
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public abstract class AbstractSoprpParty extends AbstractThreePartyOpfPto implements SoprpParty {
    /**
     * 所需的key
     */
    protected TripletZ2Vector key;
    /**
     * 随机状态
     */
    protected final SecureRandom randomGen;
    /**
     * current data to be filled into the circuit
     */
    protected TripletZ2Vector[] state;

    protected AbstractSoprpParty(PtoDesc ptoDesc, Abb3Party abb3Party, SoprpConfig config) {
        super(ptoDesc, abb3Party, config);
        this.randomGen = new SecureRandom();
    }

    protected void checkEncInput(TripletZ2Vector[] plainText, int blockSize) throws MpcAbortException {
        int numOfInput = plainText[0].getNum();
        int originLength = plainText.length;
        // 如果输入不足长，则在前面补充share为0的wire至足够长度
        state = new TripletZ2Vector[blockSize];
        if (originLength <= blockSize) {
            for (int i = 0; i < blockSize - originLength; i++) {
                state[i] = z2cParty.createShareZeros(numOfInput);
            }
            System.arraycopy(plainText, 0, state, blockSize - originLength, originLength);
        } else {
            throw new MpcAbortException("the bit length of prp input is too long: " + plainText.length + ">" + blockSize);
        }
    }

    protected void checkDecInput(TripletZ2Vector[] ciphertext, int blockSize){
        MathPreconditions.checkEqual("ciphertext.length", "blockSize", ciphertext.length, blockSize);
        state = new TripletZ2Vector[blockSize];
        System.arraycopy(ciphertext, 0, state, 0, blockSize);
    }
}
