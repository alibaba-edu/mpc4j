package edu.alibaba.mpc4j.s3pc.abb3.context.msg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.AbbCoreParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * verification message synchronizer for 3PC
 *
 * @author Feng Han
 * @date 2024/01/26
 */
public class VerificationMsg {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationMsg.class);
    List<AbbCoreParty> coreParties;

    public VerificationMsg() {
        coreParties = new LinkedList<>();
    }

    public void addParty(AbbCoreParty abbCoreParty) {
        if (!coreParties.contains(abbCoreParty)) {
            coreParties.add(abbCoreParty);
        }
    }

    public void checkUnverified() throws MpcAbortException {
        for (AbbCoreParty party : coreParties) {
            // the current party should not be in the verification process
            LOGGER.info("start verification process for: " + party.getClass().getName());
            assert !party.getDuringVerificationFlag();
            party.setDuringVerificationFlag(true);
            party.verifyMul();
            party.setDuringVerificationFlag(false);
        }
    }
}
