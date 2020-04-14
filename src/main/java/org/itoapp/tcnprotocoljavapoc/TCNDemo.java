package org.itoapp.tcnprotocoljavapoc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.bouncycastle.util.Arrays;

public class TCNDemo {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SHA256 = "SHA-256";
    private static final byte[] H_TCK = "H_TCK".getBytes(); // Pin charset?
    private static final byte[] H_TCN = "H_TCN".getBytes(); // Pin charset?

    byte[] rak = new byte[32];
    byte[] rvk = new byte[32];

    List<byte[]> tcks = new LinkedList<>();

    public TCNDemo() throws NoSuchAlgorithmException {
        Ed25519.precompute();
        RANDOM.nextBytes(rak);

        genRVKandTck0();
    }

    void genRVKandTck0() throws NoSuchAlgorithmException {
        Ed25519.generatePublicKey(rak, 0, rvk, 0);
        MessageDigest h_tck0 = getSHA256();
        h_tck0.update(H_TCK);
        h_tck0.update(rak); // why do we use this ??? 
        tcks.add(h_tck0.digest());
    }

    private void operateRatchet() {
        MessageDigest h_tckj = getSHA256();
        h_tckj.update(H_TCK);
        h_tckj.update(rvk);
        h_tckj.update(getLastTck());
        tcks.add(h_tckj.digest());
    }

    public synchronized byte[] getNewTCN() {
        operateRatchet();
        MessageDigest h_tcnj = getSHA256();
        h_tcnj.update(H_TCN);
        ByteBuffer length = ByteBuffer.allocate(2);
        length.order(ByteOrder.LITTLE_ENDIAN);
        length.putShort((short) (tcks.size() - 1));
        h_tcnj.update(length.array());
        h_tcnj.update(getLastTck());
        return Arrays.copyOfRange(h_tcnj.digest(), 0, 16);
    }

    public synchronized byte[] generateReport(int daysBefore) {
        int end = tcks.size() - 1;
        int start = tcks.size() - daysBefore - 1;
        if (tcks.size() <= 1) { // have we got more than only tck_0?
            throw new RuntimeException("no Keys to report about");
        }
        if (start < 1) { // give em everything we've got (except tck_0)
            start = 1;
        }
        byte[] memo = createMemo();
        final int totalPayloadbytes = 32 + 32 + 4 + memo.length;

        ByteBuffer payload = ByteBuffer.allocate(totalPayloadbytes);
        payload.put(rvk);
        payload.put(tcks.get(start));

        ByteBuffer beginAndEnd = ByteBuffer.allocate(4);
        beginAndEnd.order(ByteOrder.LITTLE_ENDIAN);
        beginAndEnd.putShort((short) (start + 1));
        beginAndEnd.putShort((short) (end + 1));
        payload.put(beginAndEnd.array());

        payload.put(memo);
        byte[] sig = new byte[Ed25519.SIGNATURE_SIZE];
        Ed25519.sign(rak, 0, payload.array(), 0, totalPayloadbytes, sig, 0);
        ByteBuffer ret = ByteBuffer.allocate(totalPayloadbytes + Ed25519.SIGNATURE_SIZE);
        ret.put(payload.array());
        ret.put(sig);
//        System.out.println("net.kiwo.dummyproject.TCNDemo.generateReport() " + Ed25519.verify(sig, 0, rvk, 0, payload.array(), 0, payload.array().length));
        return ret.array();
    }

    private byte[] createMemo() {
        byte[] symptomData = "symptom data".getBytes();
        ByteBuffer memo = ByteBuffer.allocate(2 + symptomData.length);
        memo.order(ByteOrder.LITTLE_ENDIAN);
        memo.put((byte) 0); // 0x0: CoEpi symptom report v1;
        memo.put((byte) symptomData.length);
        memo.put(symptomData);
        return memo.array();
    }

    private byte[] getLastTck() {
        return tcks.get(tcks.size() - 1);
    }

    private MessageDigest getSHA256() {
        try {
            return MessageDigest.getInstance(SHA256);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}
