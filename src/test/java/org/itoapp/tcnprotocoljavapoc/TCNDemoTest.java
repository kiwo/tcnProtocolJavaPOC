/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.itoapp.tcnprotocoljavapoc;

import java.math.BigInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author kiwo
 */
public class TCNDemoTest {

    public TCNDemoTest() {
    }

    /**
     * Test Generate Report
     */
    @Test
    public void testGenerateReport() throws Exception {
        String rak = "577cfdae21fee71579211ab02c418ee0948bacab613cf69d0a4a5ae5a1557dbb";
        TCNDemo demo = new TCNDemo();
        demo.rak = hex2Byte(rak);
        demo.tcks.clear();
        demo.genRVKandTck0();

        IntStream.range(0, 9).forEach(i
                -> System.out.println(byte2Hex(demo.getNewTCN()))
        );

        assertEquals("fd8deb9d91a13e144ca5b0ce14e289532e040fe0bf922c6e3dadb1e4e2333c78df535b90ac99bec8be3a8add45ce77897b1e7cb1906b5cff1097d3cb142fd9d002000a00000c73796d70746f6d206461746131078ec5367b67a8c793b740626d81ba904789363137b5a313419c0f50b180d8226ecc984bf073ff89cbd9c88fea06bda1f0f368b0e7e88bbe68f15574482904", byte2Hex(demo.generateReport(8)));
    }

    private byte[] hex2Byte(String in) {
        return new BigInteger(in, 16).toByteArray();
    }

    public static String byte2Hex(byte[] in) {
        String s = "";
        for (byte b : in) {
            String st = String.format("%02X", b).toLowerCase();
            s += st;
        }
        return s;
    }
}
