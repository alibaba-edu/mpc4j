package edu.alibaba.mpc4j.common.tool.crypto.commit;

import org.bouncycastle.crypto.Commitment;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.util.Arrays;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * abstract random oracle commitment scheme based on JDK hash.
 *
 * @author Weiran Liu
 * @date 2023/3/17
 */
abstract class AbstractRoJdkCommit implements Commit {
    /**
     * the commitment scheme type
     */
    private final CommitFactory.CommitType type;
    /**
     * JDK message digest
     */
    private final MessageDigest digest;
    /**
     * byte length, this is the inner parameter in the hash function
     */
    private final int byteLength;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;

    AbstractRoJdkCommit(CommitFactory.CommitType type, String jdkHashName, int byteLength) {
        this(type, jdkHashName, byteLength, new SecureRandom());
    }

    AbstractRoJdkCommit(CommitFactory.CommitType type, String jdkHashName, int byteLength, SecureRandom secureRandom) {
        try {
            digest = MessageDigest.getInstance(jdkHashName);
            this.type = type;
            this.byteLength = byteLength;
            this.secureRandom = secureRandom;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossible if create an JDK hash instance with invalid algorithm name.");
        }
    }

    @Override
    public CommitFactory.CommitType getType() {
        return type;
    }

    @Override
    public Commitment commit(byte[] message) {
        if (message.length > byteLength / 2) {
            throw new DataLengthException("Message to be committed to too large (" + byteLength / 2 + "): " + message.length);
        }
        byte[] secret = new byte[byteLength - message.length];
        secureRandom.nextBytes(secret);
        byte[] commitmentBytes = calculateCommitment(secret, message);

        return new Commitment(secret, commitmentBytes);
    }

    @Override
    public boolean isRevealed(byte[] message, Commitment commitment) {
        // read secret and commitment
        byte[] secret = commitment.getSecret();
        byte[] commitmentBytes = commitment.getCommitment();
        // real the commitment
        if (message.length + secret.length != byteLength) {
            throw new DataLengthException("Message and witness secret lengths do not match.");
        }
        byte[] calcCommitmentBytes = calculateCommitment(secret, message);

        return Arrays.constantTimeAreEqual(commitmentBytes, calcCommitmentBytes);
    }

    @Override
    public int maxMessageByteLength() {
        return byteLength / 2;
    }

    private byte[] calculateCommitment(byte[] w, byte[] message) {
        digest.update(w, 0, w.length);
        digest.update(message, 0, message.length);
        digest.update((byte)((message.length >>> 8)));
        digest.update((byte)(message.length));
        return digest.digest();
    }
}
