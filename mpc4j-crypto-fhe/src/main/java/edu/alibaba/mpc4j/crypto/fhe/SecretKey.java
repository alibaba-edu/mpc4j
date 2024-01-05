package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class to store a secret key.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/secretkey.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/14
 */
public class SecretKey implements SealCloneable {
    /**
     * secret key
     */
    private final Plaintext sk;

    /**
     * Creates an empty secret key.
     */
    public SecretKey() {
        sk = new Plaintext();
    }

    /**
     * Creates a new SecretKey by copying an old one.
     *
     * @param copy the SecretKey to copy from.
     */
    public SecretKey(SecretKey copy) {
        sk = new Plaintext();
        sk.copyFrom(copy.sk);
    }

    /**
     * Returns a reference to the underlying polynomial.
     *
     * @return a reference to the underlying polynomial.
     */
    public Plaintext data() {
        return sk;
    }

    /**
     * Returns parms_id. See EncryptionParameters for more information about parms_id.
     *
     * @return parms_id.
     */
    public ParmsId parmsId() {
        return sk.parmsId();
    }

    /**
     * Sets parms_id to the given one.
     *
     * @param parmsId the given parms_id.
     */
    public void setParmsId(ParmsId parmsId) {
        sk.setParmsId(parmsId);
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        sk.saveMembers(outputStream);
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        sk.loadMembers(context, inputStream, version);
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        int inSize = sk.unsafeLoad(context, inputStream);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("SecretKey data is invalid");
        }
        return inSize;
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        sk.unsafeLoad(context, in);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("SecretKey data is invalid");
        }
    }
}