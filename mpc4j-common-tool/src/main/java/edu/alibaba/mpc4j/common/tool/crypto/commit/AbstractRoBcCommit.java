package edu.alibaba.mpc4j.common.tool.crypto.commit;

import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory.CommitType;
import org.bouncycastle.crypto.Commitment;
import org.bouncycastle.crypto.Committer;
import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.commitments.GeneralHashCommitter;

import java.security.SecureRandom;

/**
 * abstract random oracle commitment scheme based on Bouncy Castle hash.
 *
 * @author Weiran Liu
 * @date 2023/3/17
 */
abstract class AbstractRoBcCommit implements Commit {
    /**
     * the commitment scheme type
     */
    private final CommitType type;
    /**
     * the maximal message byte length for the commitment
     */
    private final int maxMessageByteLength;
    /**
     * the commitment scheme in Bouncy Castle
     */
    private final Committer committer;

    AbstractRoBcCommit(CommitType type, ExtendedDigest extendedDigest) {
        this(type, extendedDigest, new SecureRandom());
    }

    AbstractRoBcCommit(CommitType type, ExtendedDigest extendedDigest, SecureRandom secureRandom) {
        this.type = type;
        // we use GeneralHashCommitter instead of HashCommitter.
        // The difference is that GeneralHashCommitter includes the length of the message in the hash calculation.
        committer = new GeneralHashCommitter(extendedDigest, secureRandom);
        maxMessageByteLength = extendedDigest.getByteLength() / 2;
    }

    @Override
    public CommitType getType() {
        return type;
    }

    @Override
    public Commitment commit(byte[] message) {
        return committer.commit(message);
    }

    @Override
    public boolean isRevealed(byte[] message, Commitment commitment) {
        return committer.isRevealed(commitment, message);
    }

    @Override
    public int maxMessageByteLength() {
        return maxMessageByteLength;
    }
}
