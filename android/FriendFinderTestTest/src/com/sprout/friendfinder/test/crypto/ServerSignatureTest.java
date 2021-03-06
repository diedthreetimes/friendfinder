package com.sprout.friendfinder.test.crypto;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.openssl.PEMParser;

import android.util.Base64;

public class ServerSignatureTest extends TestCase {

  public void testDecode() {
    String encoded = "qpNq4BNsJ0cQ2rdwS9fAqlcy3xs=";

    String expectedHex = "aa936ae0136c274710dab7704bd7c0aa5732df1b";

    byte[] hex = Base64.decode(encoded.getBytes(), Base64.DEFAULT);


    byte[] array = new byte[hex.length + 1];
    ByteBuffer bbuf = ByteBuffer.wrap(array);
    bbuf.put((byte)0);
    bbuf.put(hex);

    assertEquals(expectedHex, new BigInteger(array).toString(16));
  }

  public void testCert() {


    Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

    String auth = "-----BEGIN PKCS7-----" + "\n"
        + "MII5cQYJKoZIhvcNAQcCoII5YjCCOV4CAQExCzAJBgUrDgMCGgUAMII3EgYJKoZI" + "\n"
        + "hvcNAQcBoII3AwSCNv95cEhxRENMV2xoN0oxZjJnMXdtR2pIdmF4SnM4b05mNVZr" + "\n"
        + "UUVyMTFNZWNHSXBjakFaSzFzUXd4RE03YWsKWlM2akloV3Z4TUxSUkdqdkpJNzlS" + "\n"
        + "bmpaRjZUQWc5Ym1weTlicDBUUWE3K3k0U3hmSm5rUWNPSGZXQytTCjVyMnd1MTBN" + "\n"
        + "WnFBeDJnKzd2WEs4MEdxVEJORFBFb1RIaHJIUWY4RWRrUTdCWi9jREVwMD0KIEk2" + "\n"
        + "Tk16M0VBc2lodlZjcDFzV09FQTEzUDNFNDFxcGhjVTZydXNzUFU1VStkeWozL3Aw" + "\n"
        + "U3QySk0yaDRsagpGdUEwNFJKYi82YmhtWEFVV3ltamtwNnFwZEdndityN3BDckpy" + "\n"
        + "RGw1NUY0YlB3djM3QUcwQUtFVU4rSHEKeUM3MjZ5QS9oaFBUdStqMitBOU05Um1R" + "\n"
        + "eEF5empHUjBwMHpROWNsSWpjWHRxRUc5MjhBPQogV3VtdkJOeHNGbWZWOXZvM0tV" + "\n"
        + "RW9BNndwVmNqODVYWVVRSzR0TFFoZ211VEttcERXTjR6ZWJUMUxkampkCkFycDB5" + "\n"
        + "c1VKRlJDcERlVFVLUVVLMU9PaGRjZ2pydENaQm9qckp3cVdISnRNSWRCYnRLVWtD" + "\n"
        + "b2ZLVTRCSQoxTHhXYXpmSy9ZWk1tVlF2bWllOW94ZDd6OXFlNlVjOFlxWmNVYzVq" + "\n"
        + "bXNvWE5iWDVlTlE9CiBrYUI3YWpwdjdCVzB5U0I4SXExcTJ0UkdyUzdoWDNrbHpF" + "\n"
        + "NWpqT0x1L2dSWFlVQjJ3ckVXWDNtMDlSK3QKVzJtMm1qcFByMkhrdUVUZEV1WTdH" + "\n"
        + "dzdOY3lscStXMkNIQW9vREs3SjlaeFhtRW5lVzErZjRCbURJc2svCmdaUk1jR000" + "\n"
        + "d1YyS2tYT3BFdTRkNytiQS9EMUMvR1JFSFdjdFZlU0tXLzFNcU9YeWpkND0KIE9S" + "\n"
        + "UTlvMjhDL05KZDdhc0hoUi9ZNERWWXZSaGtkclhWZlZldVgwWDgreEhnZkJYUUd2" + "\n"
        + "OS9aWlQxRUY1bwpJUzdBbzFyV0tGMERVZ0VBL0IrbFFaWjhvU3Q3VEJKbmkvQ1JI" + "\n"
        + "SXduUktDc01OSXZscElNbndWeDNod0QKUzM0ajM0blJma0JjMnk5ZG1OeUVvOGJF" + "\n"
        + "WW1LdHFBM1U2NzE2Ui9DMTdNdFk2UzRDTmk4PQogdkQxNUN4SXRLaTA5RWhnOFFG" + "\n"
        + "Y05FVzVVSVROelRRQ3JIT1VVTllaNDJoSmd0amZCZTVONW9Jc0FneGdsCjR6dnRR" + "\n"
        + "MHlVcVZHaGFqZEw3V01EOTNyeVJNWkRsbXNQV3F4NmV4UzBKOVVmeE51UTBKV1Ev" + "\n"
        + "NHF3cHFxSwpnZE9MSDJJaE1JTVYxWEk4eG1IbDFVYlBvYktHaWZPdmk0emZYckY5" + "\n"
        + "ZVc4bXN4Z2FTams9CiBsZWY2dWRLbTRPb01NU0tVa1JLenBjSXRzU3JsTVovNkRx" + "\n"
        + "cC91L002ZDZTTnhTbElhekVTMzVKcXBhY2QKdTc0eGllekFMeU9RWnlKbFJxL0VJ" + "\n"
        + "eThXRFVSTUp5VFl6dUQ3M1lBNEJJbFY1MmJ0Z1A4ZjkxTEdudFZYCnQxZFI5Zkcv" + "\n"
        + "UkZEVCtCSlVVSllvVi96dXhET1lHSGtkMUd6V1NoSi9NcjRZN2tWeVBNYz0KIGpp" + "\n"
        + "ZExZZThkT0dibGVDZ3pYbGd5TWdSUjVFNGtqRGRWY3NRMCtIRHVQRDlleEpqQXZV" + "\n"
        + "VVFtTkpkREtkNApWNE5udmwzT21mYzJ6S2dueUNvd3g1ckxRVzlwd1RidmpYVkhk" + "\n"
        + "Kzlqc3NKVG1YeUhGbDNtT01yNkswOFcKekMxcVFKeExjTWVFSWNGRFpQeTB0Wk5U" + "\n"
        + "MmJLSGxWY21YNlVXVVk4RS9GWFhVRm1TeDRFPQogVjBMbExNVTlCUDNqTDFMWnBr" + "\n"
        + "WUNWNnlyVEVDdml4cVhEWHdWVmRRZlVOemVmY2w2TEh1c1VMV09lSlBuClZES0ow" + "\n"
        + "V01FcWlZRVRlb0dBNkF6RXNuMXVnOHB3OGU5OHBRZXJKUFZkT1RWQTlxTHRMYUow" + "\n"
        + "Q1haaHJXTApMRDBHcENxVFlYemhxZFV6ZkUrSHJJUXRlZ1oyc1U3VXd0ZHZhdTdG" + "\n"
        + "enpFbmtWQlVzZDA9CiBSWkllc3pNc3JFVzZPaE5zbWVkclhSU1dPYjhmZ1pxT0R1" + "\n"
        + "M2NXaWxqbFVud2U2U0k5N3BYbUoxRUtxYmoKYVF5YUd5cXJBZjcxY2pDK1VaVEtn" + "\n"
        + "OFFvTXVCZVNQTWJZczdPV0RMYXNvV3ppTnBoY0FJTnhYYlpqQTFMCmNhNTlHamIv" + "\n"
        + "Qk9PcGpGZXBwNi9BT0NrQUpxTmZpcTI4QXVLQkgyTU4zZ3VCRlJSQS9yTT0KIHJK" + "\n"
        + "c043WVZmSkpLRFRoaG83ZE9kNWhRcVF2Zm5sQ1VTeUF1QmVRT2FZVHBYTHhrVjdl" + "\n"
        + "ZSt2enJuaTI5NwpaZnJsRzBzb2ZneWVMNTEzQ3ZGZ1FMRSs4UWpHeHU0bnBFQ0RC" + "\n"
        + "YWgyc2hJcUFlMG1QZ01vRFVMblRqNGIKR3gzcGk2UUVrNzIrL21FUlJjOENYeURt" + "\n"
        + "YnBtYTBpSjZtN1RRS2JGeWZjVzJxSHJIdmRRPQogcDBSWVpwb01NL3hHZUcrWm5s" + "\n"
        + "OGtza0l0U2p6cjE3UWI0aEppN0p6bE1ZVUlvWVZ2cms4N3RrOTJDNGk1Cnl3VUNC" + "\n"
        + "TmhQdlhBb0hIekYrRzR3RmZoc3pzY0ZaTU9ZckZ1WW1ndHlyOGhXY2Fmb1ZKaWZs" + "\n"
        + "WnlzUW5rKwpoYXF0M29rOGVQSHE2NTM3aE5HcWpUN05ueGhKc1lER0pvVXhCR3J5" + "\n"
        + "ZlhPb1dObzNxL2c9CiBQbVFmVCtvWnU3MTJ4blh2S2I2T3JkcytkMWlCbHFrRTQ0" + "\n"
        + "eGgwSGxkb1YvQk1TYzlPVm1WWHpqREVFVnUKNGtoUFZ5V1JMUE1YL0lvaExyTDBz" + "\n"
        + "Y2dLV2xpNmRWTlJBRWJSbjUxVEltcnhsODJBc3IzTFJibVRSM3F0CldUd1hGUExq" + "\n"
        + "Um9MMi9uTkVMK0lFaXhhTHNRYXlGUWRwa09QS01haHIwU0J5eXJWYmovWT0KIElT" + "\n"
        + "bnlVTHdWUzVyNi9BQTA4bTZidWQwM0Y0Z3h6STFHdkxUZkozZFYzdG5nTkpsaTdK" + "\n"
        + "UzFlYkMxT0VrTQpKcjZ4bGo0RWV5aUl6MkliT0xGQXRvYXpvQUJiYWFGSEJFdHlD" + "\n"
        + "SEM5YW40aFZkYmNxK1k0cFl1Q2FyV2cKVUVvc3JTVm1sY2lhWmpDdEFvU1BydC8r" + "\n"
        + "YnhIdTNzdVZibkFZVUlybWZtb0JNbkk3RDN3PQogL0ZVNjgwN2loVVdtYnE3VDVQ" + "\n"
        + "cWdlSGtYM1NDVWRFbXpOTUFsbXdVNnRPdGVkYzI4eWt4Uk4vSjF4djROCmdxaW9z" + "\n"
        + "ekUyR28wUitvb3hoV0w5bWRldWVSTWx2MGhSbk1ubmdTVTg1a0VWMk9lVXNkRGFJ" + "\n"
        + "V0JENnczawpDM09SUmhNQzB3NWZ4bEhoc3B4OUFROEpjU0kwRkhIUVFtQ0RmR1Nw" + "\n"
        + "NnBEL0tFbjR3UEE9CiBhV3lhaW9aSmhNNlZnV0h3RUVsaElLa1k0OTA4M2F2Yjlu" + "\n"
        + "L1VGdjlwYnB1VU4yeVlNclhwU2pZVG9tL1gKMXN3ZGk3d0h2Q3lObnZHcmhOczdX" + "\n"
        + "VTlJQkM4R3FGUlZtWm9MYVd5UGN5T0JpWEpwL0tyN2x2M0hGZ01UCmFtN3hUbmNj" + "\n"
        + "ck1FWlF3TmVHamdMRmdURUY0eTRVY3FzRDlsY0ZBM0VVbHZ4WXYxRVZIND0KIFVO" + "\n"
        + "TG56bW9Xa2YzL2tCSVdVTVNJcUs1Zmg1bzcxQTlYbUxGMmxUM2lRUlBVRGtLZkJB" + "\n"
        + "OEVRRlQ5aGVrSgpvYWsxZHQvTVpVTGx4YzRzakUzcWttSnM3dDgvd0h6VmJ4ZWVZ" + "\n"
        + "VnVaYnJIM21TRVRNM2NJQzN3R25qTDMKREwzSHdhNXlDbGNHSldVQndtaHg3REJt" + "\n"
        + "WkJTNDl3eTlpQ0xoN2hFT0x4Q2FUN2I5VDBJPQogdEpKUUlzRktLWE91YitUSGZp" + "\n"
        + "VGFOMXZZdFZvMjVkU1BXdVFvUkdDNjcrcEN5ekdEdDdPbmlYTlY0YUd2ClY1am1o" + "\n"
        + "dVF2UlQ3YlN3WVJQU1ZaNnhFb2NuRGN1UDZKOEpPZm8ra3VMamllMThYdjBLZ2xH" + "\n"
        + "YVZ4MkFzMwpmWjROR2tRdkVoMXZWOEZPT0hqelRUQytqdEVuM1A5U003SnNFYmpt" + "\n"
        + "V3hDakdQR0xhNjQ9CiBRWnBkQUw3T3I2RjUvYWFDS1ZKdzF5MStmMi8vMGgwUW9u" + "\n"
        + "blM5WVdiWkpISWZOMUF4UCtGVkhhSUo0VHIKamRxNW1pWDFxYkZuL3dFTjV4Z1B4" + "\n"
        + "Tk1OcGNDRGxldFZpZ1ZJQ1dhdG5TbzlKMXhBd2pHcWYvWWZ1dU1hCkhsQXBhRXg3" + "\n"
        + "SlJSWEMvWE5FbW5wZ3hFeUJMSGNhL1RpOEVuNU1kV01NY3BzUi90c0crQT0KIE1h" + "\n"
        + "d1RXVXNjc2gvRnlyb2FlY0p6dEtML0U4ditRR1hRNXZ1QnFSQnBKdXJyK3FJbTdS" + "\n"
        + "YlQ5RDdDVVBLOQplOGFxbW5aZ3Q0S3A3VmQwSzZrdG9yeFVWcm5WSFRlU2dKdmxv" + "\n"
        + "cnJadnlJUTBmYUplb25XRm9WRmpaWlMKYTJOaFV0WGRvSE9nRVRuOFZ6Q0hMaTdY" + "\n"
        + "bHp5VzEra1JGUDN4STdyV0NqUlUwYWh1TnlNPQogdll1YjdyNEE1aGU3TGxBa3hj" + "\n"
        + "dzVQREhQOE11RFFMdmlNa2dzLzFxZ21BaGxiVE5ZVEZuTkRQaWN4Zi9mCnp5eEF4" + "\n"
        + "aGQ4UHBzM2lxbVBXaTlodkd1SlAzc3Vwd1hXL1NUTWQ0S0NhbkphSzJwZkcwK296" + "\n"
        + "N2VZN2F1QgovY0NEY1lEbm9YdTdhVlZNeGVSbDhwYVFONHhRVURoeHJ1MDdmUitp" + "\n"
        + "ZUFvTEtDSEFJRmM9CiBwQUtZd2hDTGtkNW40TUJkL29LN3Qyc1B5WUc2WkErUWtH" + "\n"
        + "T2xIR3FxQnFtUEY1djhrcktrVjJIRGZFaUsKMjNFNG9OUW02QmphVDN1N3J6Rm5m" + "\n"
        + "eWxyUlc3WW5nWW4zbVhiL01TWWhaWFN2aFlqcUY0M2o0OFAremM1CnVxM0JKVXlk" + "\n"
        + "c2oyNS9VZGF6S0YzQ2NKWlU5cEJRaTJtdk5pSXpsSEJvaUxuYThiT2tkbz0KIG0x" + "\n"
        + "eTBnTEQwZUt2ZHREMDdIQlcxRHdOeGx2cDJ1M2JrZ2E3UnVBSXBKQVp2dVpFbCtl" + "\n"
        + "MU1mclRHc1VLTApmYzQxem1BMVlZSWFmVnZWVDJibEZqMlZUZTRTN3FYSzRSaCtn" + "\n"
        + "OHEzZDFzSXR6dnduMW9QWS9NcXR1WEQKL2k0RWs1S3FTSVJTeitrTUxkOVp3dkVL" + "\n"
        + "cG03VWcrUFp1VW41SU1qUnRDSllYdzkwQjdnPQogUzBqNklYZG5CVnZ0cnhJR0ov" + "\n"
        + "U0VPMTlrZTY3ZGRMdTJ6ektzUHRPNkRkQUQvWGlZRldsSUdzeDFFYkpXCm5VWHhG" + "\n"
        + "UFQzMkplOFJoQUVmRXZWdXM1OWxlVXRrSlVJWmFnVEs5eEtoZ3hZa1VsbXd4cEN0" + "\n"
        + "TFJZSUJnTwpyUnJuSTZYUUpPSllYZ3YwMnZPbmNISFpKTm50K3YrdUc1Z1BhVkVT" + "\n"
        + "RkhuWVZ3SjFFUUE9CiBRRktuUG5mQ2pPanErZXZ1NXZOZU9HdnJHQms3a1dJUHlW" + "\n"
        + "c0NXcWx0N3FyeTV4dGdxQ1V6VklZUGpHYmIKdzVEZ1EzSVlENnQ1cVVGTWN2RlB1" + "\n"
        + "cFRrbDJlbHdwQVVZbXFTWUc0VUdRWVpYY0phUGUvSndubVgrWC9UClNoTUZqa0tL" + "\n"
        + "VGQ2S0RUM1JRakNXUjdPOHJXU0ozU3Nzdk9JeWxrQ1FXekRVdTRTSitEND0KIHdm" + "\n"
        + "aHlwY3lYZDdraVl3eTlEZUorYVhkK2Voajl5Ukk0cnNJaUZSdTJBNTV2eDFlWnRI" + "\n"
        + "VEZ1bFV6QmptVgpEZkZOYkYwUGJwTjJGOWFRdEV2c0FLU2d2NVBDTVZ4TVdKbUFB" + "\n"
        + "eE9qUlIzU1pIRWErMFFDSmpnYW5IRHkKdjRnb1hyY3JUN2dNdHp5UXlwOVJGNmRn" + "\n"
        + "d2c4ZUdrWC92dDJuS2I2VXd1YXN4L0JidVV3PQogcWcveG5sTndPS2lKSERYOXR1" + "\n"
        + "QmJ2cUxVYkVQTmVoMnh3eXRCTXRpRUJreUVJeUxVdjBjSjJybDg5RjE3Ck1oKzRF" + "\n"
        + "YUU1M0p4ZGJFTEJuRjZZYTlwa1hLbkJPa1hjbDA1S1hpL1VMQkVxbG01NlBOWlNk" + "\n"
        + "dmZBd3g2dApvL3RrMU1ack1uZU9VTmxSZVVyMVZpcVBKZkpELzA1OW9wMkVoUmM4" + "\n"
        + "UXMwM1RvOU9Ccm89CiBSdTdLdW1KVVBOZWp5SW9VMmo4M3BCdTFxbWpoMVMweXZu" + "\n"
        + "N1d2NjRpME1qeWM2cTc5bFdLQUE2MzdTZXkKWU1oV3pvUnFQRG03NGVIY3crem9U" + "\n"
        + "dDArSlJBK09lMTY5VWNzRVFocG1WcVRYS1BUdms1UWdPeTRrK3lOCnNHaXRHbzBM" + "\n"
        + "T0lESGtjaVFSUGJDUkd5UlBGMGQ5emtzWnYxeFA2N0laL1ltM2VQT1RRWT0KIEtY" + "\n"
        + "Nk1GVTFlMFhqUTk2WXR3V3lBUU5tU1QvVTIrNVVVNWFaNWdLbU1BWjkwTVlpL1cx" + "\n"
        + "SENNUHNMMEhVYQozZEF1VzhlTWs4U1kxZkdLVmJuVThLQko5eVJpRmh3NzMxRXo0" + "\n"
        + "K3FBcU5mTzRjQXNWa2VWd3gzZFQyblgKOGw1K1pOaXF0Wk0rdlI2YVNzRTZ1cVpk" + "\n"
        + "VzRka1FtMHA5ZVBod0ZqdFhVQnBBb1JpNGNjPQogZDBObTZ2Q3hsYlMvWVNMQlk2" + "\n"
        + "UjlwaFExQ1h3SHE3MEFQeU56SnlsMWp2S1UxUEJSSXJuOFQ1dFNISWt4CkFJUFZY" + "\n"
        + "TGtXMGl4SFlvYi9PRTN2VXhTTkpmV2F1NFVJeFg0Q0pUdXV0T3ZPbVo4eXFnQ0pk" + "\n"
        + "UzBSRVkvZQpDNzhUUUJIUXhnR3Y0cXpmbFdhUWcyeUJuRzhscHBUK2NKU2owRzZ2" + "\n"
        + "Vnh6M3l4Um4wbm89CiBiVG9pcDl3NXZjdUJEZ1dkZTJIU1dScmZiSXJkUWF5TUFr" + "\n"
        + "eThEYkpmT0dWTW5iMnhURzAwb0laYnNweUsKRGNtb0ljRnZpSUlrUDk0L05rTjJI" + "\n"
        + "Skg3ZDdhNXA0eCtDZVhnWk11V3lhNXhwNEFKTWFCaXd6UEJsemdlClI0OWozUFFW" + "\n"
        + "ZTM2OXZ2YTZFOVRVUUhCMTJJdW5YeWlQTVRyVnNPQ0dPU3A1OW4rbS9NTT0KIFVx" + "\n"
        + "UTQwTFVaSUpGLzI2REdSdXRoaGtYUGtCS1JmVzBtbnVrRzlFY3N6WUJZbk54Zk5q" + "\n"
        + "WDZ1VUdydnFONQpnYnpnUlpuZ01TckdZNEtoQUVWeXRmWFNrbzFnRWc2blpXcHlu" + "\n"
        + "VDR3K005THJ4VzBQVXcwV295VmdicmsKZm1TcEtOWlo5VFYvb1FXZGkvYTVoNVph" + "\n"
        + "SEtsRkczSVUrN1VvWEFwd0Eyb0lUSlRldXI4PQogT0o2cXlmOEVsZE9pQmdsRW9D" + "\n"
        + "Skg4NUt1cVhMV1lORUQxUDZCZ3VZUzhSbFhyK1NmWDYwNDNRa1ZiSldjClBtK3BP" + "\n"
        + "aWVWNjhaMkdqMWxKTGcvWnpxU3NLZGFPSWthWWxpY0lzY3ZhWU1TZ2tPNXQ3Y0lI" + "\n"
        + "eHJ1bXE3UApXMElVUWp6UGhEYk0xUkk3eVVkNUh4TTBhcEpYT0FMME43am1JQVlz" + "\n"
        + "TytGVmcyWDh4M3M9CiBUTXp6ZDU0eS9aMDl6Vi9sTThkVW00WFRxMmpQM1RxRjFz" + "\n"
        + "d1FUS2FZMWhxeGlFQktBd0lqZHJ2RkZVeHcKOFFYV1NIVm1hSXhNbGFKVFNIWTc1" + "\n"
        + "TWk0eE1nOXp2MForN2pESHRVemwrdzdHOVN4WUJLOHhKNm9rZDdUCmFIOU50K0JL" + "\n"
        + "Y2R6VXJTNGhPRDdGd2lmNXVhZmNocXg1MXY4cVBOT3FXUmJ0Vks3dTRwbz0KIDJY" + "\n"
        + "YVFubDBxb1ZLRmtzendtZHNtVGFCUDJBdnNPM0xya0lGa01WWUp2Y2RmWEE4Sjl0" + "\n"
        + "OGp5QlEyZnRzOQorMWJrMGcxTzFuUTJpMHhkbC85SGhmSWtsT2lhOXJBSEszSXFz" + "\n"
        + "U3VzSnNIdzVaY1BWcVpndXlsWnJYM1cKZndsNVNraTZYRWJwMjEwUkNmc21xWHl5" + "\n"
        + "R2ZObGRzL2lTWit0NUtUOUZraEhFMGFYM0E4PQogSFdnNW1WMHRHSlRiRkxObThP" + "\n"
        + "bjhwRGFsNVB6ZWJmbmRSeTVod3dsQjd0aDRsZjdFR2hNckV1L0hrR1VKCmJ5U2Nm" + "\n"
        + "bHhFR3pCdUlyR1ZBT1FsdWZJSi9pTWM0c3J0U25QUkxNR3NUTGdTdnBRT1ZLR050" + "\n"
        + "ZURRSkM4YQpTN0F6MGhETGxRMXlnd0M1K1hpem9XYlVQVFNjb2MvcEo2U210SjU0" + "\n"
        + "NnBodHpCeGhtY1k9CiBMVzdLTmdFU1FkS1FBWnJwc2lVVkU2enZMOUdaMWM2c2hO" + "\n"
        + "L1NQTTVOa2FNSUZkZHRpRk9RZDJnVVIyRHcKdFFQVlh4VTU1dHBzUjJkeVdvZnBj" + "\n"
        + "V1Iyc2VRNEdnTndPMFpvL1JHNmR1cEpuQjM0MnJmWElSeUZaUzJWCi9OVlVUdXZB" + "\n"
        + "eEVYdzF1dFErQ215elk0bU14SFptdDdaaTFGd1NHRnU0eWhNYy9jcE9rWT0KIEk5" + "\n"
        + "dmhueE9BUFJRb0hURVVRTko4NnVNVzBEeVNJNHZSRVo3cno2bGFmZ1VncG53cDRh" + "\n"
        + "dHBKcGl5djJCOApERXpxV1BMWVZILzVmNkhrb1RoS2F2R0FobVJCem5ZamVlYWRo" + "\n"
        + "ZVgzVHdua3E5VVl0enFnVFNabldlU0kKMzJHcmZoUmFISGtaS3psNGxSTDJ3WEN4" + "\n"
        + "eDhqVDlwdDlPS2FSMXo3VFVlRVhRT3V0dWtjPQogTGlNQXk2UlNwMnJwWUgwSXl0" + "\n"
        + "dFYvaHRWSkJJblU3Yi81aldjc0FadkFQeDJPQzVWWjl4WU5EeG9WeTZ1Cmo2TGJz" + "\n"
        + "ekZOeE50c0RUQmMrK1ROWG5ENWlhWHVGUHpVODdLUGN4b3FoV3BQOWY2U1M2c1Zk" + "\n"
        + "dmdsRE1jKwpRaWZUVG95R0ZZOGp3RmhJVG1qR3FwdE1VNHlDOW1mWStiWDExcG54" + "\n"
        + "dUxnb1E1ekVROTA9CiBSOVNNYUdobG1Gc29UWlJCNUJpd1FHa1ZtRElNS2JOK2pH" + "\n"
        + "bWV5SmtGd2Zpd0lRV0pwS1hYV0Jaa2g1d1UKY0R2Umo3d0xiL0hwcENQTWVyTUR5" + "\n"
        + "VDllNnlVNkkzblViM0VCaHNxNXdiMzJwd3M4RG1nemZUbXFZYXFsCmllQk1sV2xs" + "\n"
        + "UUhKZFc5bXN1RVBRUkdkd25MbE1vN0duR2tlRXVoZmp0YmNKTC8rYjNmbz0KIE56" + "\n"
        + "UFJxNUV2ZjhQUUk3VDVDN1pkMWN6ai96NlFPOTRsSnZBNzRmUW5QNUJLbk9TVzFz" + "\n"
        + "WGdqSTNEaTArZwpVMlQrTUxRMEg1Y1pkRUZPaE5BU1FFdkFsQkJtWVJkcVQzOWIz" + "\n"
        + "Q0hDMFQxYlYrVC9pYyt4VEh0TVNDUTcKRXVSOENPMlhZT2VtTFM2RVdNcG9DV1VZ" + "\n"
        + "SHBxQm9jeitJUU9sUW45RE5EYU1oSFMwNmZjPQogd2M5V040dU9nMi9nQ25DamJF" + "\n"
        + "M0NjRFpaZ1VJdUFFZHViNlhDN0dwMHB1Q2FzWWhGb01nZkcwU3l6eTN0CkNqUDlq" + "\n"
        + "bnF0a1FEczNrTVFDWDVwaUM5Qk9oWHEzS29rZHpKZGExYWQ4OGI3ZWNzdGthZjRT" + "\n"
        + "Wkt0Rk8yUAp3VHcwWTlsRWlMUm5xWjFHM2ZvbTJGampJZkhzM0xyUk5OWlkxTlp3" + "\n"
        + "eXgxYVV0MFZuUk09CiBWSGFCUjhtcVdCcmVUS2U3OWJBNjZyOVRGRTFEalBGQ1hy" + "\n"
        + "aXQyZlN6aGplQ0MzanFjOHpFdGl0d2pkd28KL29SODFzU0pCbDUyMVNuNEhWNklt" + "\n"
        + "M1NabjBmckhMdEtuT3lpeCsvOXNndTM5aG5RUTNadlY4U3Q4QWNDCnY1WWN2SlNY" + "\n"
        + "TWY1WnVnQkNnYjhOcm5RaWZNc3F2c2tQRHc0ME81RXZBWjhhbG5keTNCMD0KIHRt" + "\n"
        + "blF0YXgxa2tYUVNYaTByK2cvVCtLTm5qSVBiNlF2cUNNOUUyYkxlTDVkTHFFZzFR" + "\n"
        + "b1NLYnhpTmxKawpHUHVsTlg1OXYwYnRHZStqY0t0YUswNEFKcXFEQVlYNXBraFFP" + "\n"
        + "aEVuZWwvbmJmOCtUMGtKMUZLdHU1SHUKMFora0pHMTZLY01JSlRzUThXZHRZYmdX" + "\n"
        + "N3lxbENUQlZWa3FlVXFHUWl6dlJ4aVJmSEZvPQogTzFiLzVYeFk4UGRPcW5XRnhu" + "\n"
        + "by9UQUNxNTViMkFKdTNBM2tvcGNnKzI5aXVneVlHb25mbGZ2Wi82Y0phCk5EeHZ1" + "\n"
        + "a0RiZGxFU0d0UzVQLzlsUnNvMFM3OXNWZUYvM0hscEN1elM1Y0tXKzJ5STNjSm9Q" + "\n"
        + "enF4NVo1MAo0WkoyVUYrTmFiU2oyVlVhZVY1dzZTSFZ1Wjc4NDFKemkrQ1FsTWJz" + "\n"
        + "QU9QSmE5TXovS3M9CiBxbFp6REo2cDVXTWZ2NWRyOVNTM1VVL05oL1IycHFESnVy" + "\n"
        + "R3dUbVRFSDRnenNNL29xL2VLcWxhcUhLRWcKajlqSXdkSHYvMzh3M0FUR0NvQjhK" + "\n"
        + "QTBMS1Q4cWVkZ1FlNGtGa1BCN2lBZ29WQnRHRnNNdG4zRmlra1ExCm9xbnVKR2RE" + "\n"
        + "VTJxYnFBdldJa2h3eHZyaCtJdm4rd0FWeWp6ZmlBTDMwYzhDVndSNjB0Yz0KIGYy" + "\n"
        + "TDkvaFVxQlYwekN4VE9hV0tlZzFweFFrTDRBbEVteWpEMHRQbHZtTGpXb011TnhM" + "\n"
        + "UXR1UjVoeS9GWQorcUN3bUM2OWpZanY4TjY1U1E5L3F0b0ZCWXdCWm1aRTdHMnIy" + "\n"
        + "akNDb1o3OFIvZXBvTG9GOHlSblJGbFAKdzVqWkVzSWZLYnRrdzBZV3JwWmtORGNW" + "\n"
        + "a2ptb3ZQUnpFUkliditlUHduanVvTmFhdUlrPQogaDg3ckhRRENIZEt6NUNSd3lr" + "\n"
        + "NHJKTTZmM2hMS3pLbHd6Q2lpTU9OQzhjUk45TGt0MUNwRlRJZmNEMVB3CndRZ1Y2" + "\n"
        + "bldCd3U0WXNiMjB1Qnk0NEtkL2t6M0JOazlBZktGaTllWUVia3dVd1J5RnhZc2FT" + "\n"
        + "OXFZZ2JreApnTDlOdDhrREJnOFdabFNLL0MvU2ZCL1pGWGRtN0IvbW8rOFVrVUtB" + "\n"
        + "ajdTdzFRYVhvaHc9CiAyL29XZGk3c080WlJ4aEpmQ0lWcWE2YmRXKzRZdzVkRjQ1" + "\n"
        + "QVdxa3ZET2FwNGhTd2hOd0dQeVVJMHIyRHMKTi92RDVQbHpuU2pDUWcrNkIyODRM" + "\n"
        + "TVdTYzRWdjRWTEdGTEJRdjFJR1lsdzRwUU1rYnlKdTdGN3NtVTYvClpkVXV6Sjlr" + "\n"
        + "ek5DdFQzNFlLNFdUdEc5MFdGeVdPWEcrekNvTWJzeTQvUGVycW1tdTFPQT0KIEth" + "\n"
        + "WTFZWWJ1QkIrRTJMWmxkQ3VqNDBZVE52anBpVGRWTXg2TWdTaDN0UXVtZzN5Zkkv" + "\n"
        + "LzdOSUdhZnFnagpvOU8wWEM1emNnRGJTSlhRU2R4THplMWNnZFAxa3RTNmF5WGto" + "\n"
        + "LzNXNURVUTBWSG9uM21mcmg5Z0dmWGMKR0RKNmJCcTJ1RG1md2FySUNhdmtMa01q" + "\n"
        + "RFNOd3BTOEtib0hGVVU5blVxdHN2aEdWZXpjPQogT04yWlluejlYRC9XR0dMdEZW" + "\n"
        + "c0lTS0RIRmo3NHJGYXI4Q3dtMWJrMTRBREIxUG4rSFNvbzRGV1ZadUl3CmhMSktx" + "\n"
        + "WkpHZStZbmozM3lsanNpMDQ0SW42T2ZUTXZzWHJZczFuK1p2QjZMbFFKTG9BUnhK" + "\n"
        + "YXNGenY5Qgo4aVdzWXRxZUlSemJmZFRtSFFncHZ6UURML3VFT09WNDBFaFlmN2NJ" + "\n"
        + "bFNiQjROSWx1T289CiBVekdRK3N1bTJGM2FGR2VBV2szR0twNWNTWVEvQVllTmNs" + "\n"
        + "UWNXdHI2dkxpWkJXUDlKQnVPd1YwYTFVeGMKa3huK2N4WmRTT1QvSk1PbW1Xb1M3" + "\n"
        + "OFhnOEtMU09jbFZRRHhJYSt5SVp3R09BZUxyS1d2TkVnbHZiVUpkCmRDbFhJWmlJ" + "\n"
        + "M29sSVhtVVdMTEZHNkJJNnltZzJqa01rL092dXUyU3lwcWlwZm1GNGVkQT0KIEli" + "\n"
        + "OGpRbDBxcU1NSnZlcko2bTQyZG5HOTVTWEFHRi9OY2hLakx0QkhEYjdQMHA1SmRj" + "\n"
        + "ZWExQ0tqd3MxWApxbUdSVDh4enpINWhrY3hvd2huU3A1Zm41UXkvWll5WW9XcXdu" + "\n"
        + "bmZ3OHVjMis0RmRPcjdZRTQ1RXBxaWkKUFdGUys1UDJSbG4xNmdLellJQS9nT2tY" + "\n"
        + "bUFtc0Noc1J5dU5JT2Yyc24reUFtM1BUVFVvPQogRXZZZG9FbWhycVN5TEJWVnJj" + "\n"
        + "cTFjSzU1c3pYV2g3VHRKRmdVUS9CZVU1TkkzcjJUekZBRkhCbUxBTnRkCkhveEQ3" + "\n"
        + "UUhJa2MyR29MSklXTWp3RWpmUVdvbzlOWnVPTkRwK1B0d21YOWUyQk5VRFg2cG92" + "\n"
        + "Y09LTUYwRwpvVUdLbTdXcklkaTI4Z0xXay81RVpFQkVPOVljeTFFTUxLZFZsb2Zw" + "\n"
        + "aWFMV3RJa1JneVE9CiBQeFhobzR2cU82czVkVUVaamd4ZTFhL0s1cHJnbUU2Nmpk" + "\n"
        + "VDZkVUEzakdDSFJlUnpad2RMYkgvYlZxTDAKOHNCNlloN1NRRTI4aCt6Z1lxalYx" + "\n"
        + "MkNrNzd0WDlialRGZ1U2dEtOV0hnQXhMUTlNVXdVWE0vUTNDL1JHCjB1QlNtVFZl" + "\n"
        + "L25yUnV0Z2dxUHhWUnRxOVQzZ2M5QStOUXhXdkpuaGI1WTNMamxnYXZyYz0KIFNV" + "\n"
        + "d09Dem5uZlFyWWNlYStzTXRmdUNpNnhUL21MS3NQQU53ZjdDMlRsZi9NS29jNDVl" + "\n"
        + "TS9NK01GSlZsbwpBUnI0dE5BZURzNEhIWnM2STd3T2MvRWpVT3FpazgvQklmWWhz" + "\n"
        + "dFo0VndwVVpjYmZCVTRNRHFWSFVLcXUKS0NaU0R5RWE1WlJ5bHZ4Z0lla3ZkRDNG" + "\n"
        + "TUMzZkdicE1oTFlsZ3c5TFdCSllzbGdEcjk4PQogb0F5NXBEMEg0dmk4MVphZ3RO" + "\n"
        + "eURDVkhucGFmaHhyc25lK0oxV1FmTGtXbWROYmlyL2VHMURSK0crUUFICjIzQlgv" + "\n"
        + "Tm5KaWVOS2FSQW84N3FmNUp4VmpwTHRCQ2pnU2NIUDZyYUU0VkZScmQ4TUFNY0xV" + "\n"
        + "UGx2R3FqMgptQUxXa3dpcGxtR0FqV0Q2NmdIbWhOd3R0Y05yNUVlMFpBZHhTeWdB" + "\n"
        + "R2svQ1ltbVRzNFU9CiBjNFNKY2lJOE5KTW9GOFlrMWxkTVlhaVlscTNHNDl6S0ha" + "\n"
        + "d2tDd1VYbWFyRUtTcHBMNFlRbUJpM3pTcjcKNFVmRFlvYXlGOEc4ejBkMk9XWGEw" + "\n"
        + "eUVteHBSa2hTRDRySjkveVNuV0h5NWpELzhTTWxMcVhDMGhaYkwrCnFLK01HTmdY" + "\n"
        + "UHZ2M0VGUFNHZUtBc1RiOFora1VqRC9kdnNDOUtJTkJrK3FSdTdJZVRGMD0KIDM5" + "\n"
        + "dHZOUDBCRzlRSjlHUWFYcklFbzdTK3Z5M08xaVBvT3M5MFlxTjBCdldLTXhRQS9a" + "\n"
        + "cDVtTmtYQzBRSgpmS1h3WHQzL2VwNFNVUENoSE1aOEJyUS9rVmZYUkVqM2ZKMXE2" + "\n"
        + "TmRtVitSaFpYVCtGYWFGU3cwZFF6eGkKV0JOUDFmcnlYYXRsTEF6RU9qM3RWbFlw" + "\n"
        + "b3NlNEZKMG9Da2RDNEV2VTBNOVdlTlNTTXdnPQogYUNPeWZnVHNEN2ZvV3ZEZ1Z0" + "\n"
        + "RGM0emNKUVorMlpWSGpJVzVwVGlwcnBWZWQ4c1FoQXNuL01uT1U5UmhNCkg1aUhX" + "\n"
        + "KzFFWGZsUSsxeGh4c2RWTTlsdkQ4a2NkcXd2Rm5POC81V01WV1pTcm5Wc056YVg4" + "\n"
        + "OGtVNXpFbgp2UThNY21nK1lrb29XUG9ZUkFkUk4xbHhZRy9TL0NWaGVpclIrWG13" + "\n"
        + "M3FYVFNaMU42U1U9CiBSS2xlcFhYNXQra0JQclFPYnVYa3hEREppdGhROUxFTjZ1" + "\n"
        + "dGsxdEttT0p1eG01VEIvNFRHVWgrU28wRUUKZVpjOTg4MjQzZTZPQU5pWXRSNksw" + "\n"
        + "U0xzdjBob2E1MzhTMGZtQ0V3REF3akk0c1BLVjRxUWtxK1QraUcyClR0NDhBK2NC" + "\n"
        + "Rkk4UUJGUkp3bVFlU2FhaE5raDFjY0g4NWhTTGZITlhCVjkxUUFlZEhPcz0KIE13" + "\n"
        + "YWh3S0ExVE83WUtoT21RYUE1cWxJaTJmYnlxZk9OSm5vb1RubWV2b0tQb3BjN1I2" + "\n"
        + "Z2ZhU0V2ajF0RApLQW5SeVEwQndzWGpmcFJWTjYwcGpaWWZUTHRKUGtmM0tqYVhB" + "\n"
        + "dldscVpPckFyRXFack1ySUh1UitkOFYKRHdYTjdjZGpNWDB3WUFVNmgwZ0V5SmNO" + "\n"
        + "dk03dmovR1pWdFdmVHI5N082U0F5UWFzM0UwPQogSkhVSkF4RVowdXRtOVdMN3M1" + "\n"
        + "OUlGZHFUS2hmTFpTcmRjNHNEdmZESFZxYmpmWU9ZM0ZFUFVwWEVQZXNKCkVzSDBF" + "\n"
        + "am81SXRreW5SRi82cm92YmhpNzlmZkRGUE5iVlE4a1NnNkZrSmZTc2pKRFZZZ254" + "\n"
        + "VkRSM2E2NApNekNuYUJrYjJLM2JUSHp5d1NYeE54SVhuNXJuY2F0Tk4xM09vVEpO" + "\n"
        + "SC9XMjVFYnk3ZVk9CiBXb1VSUEZiTjcza2JJR0JxSHh5Z2l5enJDcE9adG5yUWlS" + "\n"
        + "OFZPQjBmY0t0bnNJODNPK1NINlFLdXV2a1IKMUtmay9jbVIxd204ZVpUVVBwQk92" + "\n"
        + "OEtwWjdkU3paWUFnb2hROERMT05UYjBMUndaNGJXY20wZVlySWVQCitVN2k5bFg2" + "\n"
        + "djlXZ1RSSjVEUW8yVVpVTjJ6MTFZZ1RES2YxY2R5S25ZRmVhM0c1SHdWbz0KIFpK" + "\n"
        + "K2ZVSllnTitBSkZFZXprYVFxOStveEVOQ21UZklOR2Jta1kxalhCaWNJQUYyMmpW" + "\n"
        + "amZSNVhSalg5aApXK3VBRnNDd2hYTk1LdTY1QlBQeTY5QlNwNFdNeWh0YnR1R1kv" + "\n"
        + "MllSREoyaG9qb0xxeVprV0tSMFBYSGMKVnZ1SC9mSk9IckdHbTAwUnpNMGFZRlZl" + "\n"
        + "dnJSMHRFMDltaUhZOG1tUTlvOWRwaVlRVlRNPQogc3ZuVkFGc1M0ZlhQZzk4VG1Q" + "\n"
        + "TlRoSlRFRGYwNVFKdlU5Y0Z4OGI3ak00T3ArK2dPLzBxbmxvWVloKzNlCmhkek50" + "\n"
        + "eGJZMG1NNUVmTm0vS3dRZW9mZnFqZEw0OHRPUStHYzJQbzZ3ckRJdFhnQjc4Qnl6" + "\n"
        + "cWhxbXlqUQpWNjJ2Y0NMYkJlNEZOYXhSNDUxMmlTblNPYUQ1Q2lFNzljSzZJdkNi" + "\n"
        + "ME5JcmJaMW5UM2M9CiB6aFVvRHFJcTZkMHlmZjZWakp0Q21oQjB3c3kyajVBOU56" + "\n"
        + "d3B1N2tPWThOZFloNEFCYkZRc2lQcTBEVGMKMzRPU1ptRjZnbVByVmszakt3VldM" + "\n"
        + "dnZPK2x4dW5mc1BzQmdFL01HbHRCYis0a2VYMlBVSlQzamkzVnFEClpHdkV6OG9O" + "\n"
        + "RkNrcC9YdlFtMm5FYVlucHgzWWtGUVhJblRlMERZR2pZRFJ5T3pzOWpxMD0KIDNS" + "\n"
        + "NmpFNXBWRmVkYUhwN1hQUFY4U1RqUXRHaUdVMkZRNWJYSy9QNGxlT3pnMDQ4QlhB" + "\n"
        + "YW9LdExwYzdJbApCYmlkUE1DTnN6a2VEVnArMTFVdzJoWDBxWjlOdzVTM1djZjNt" + "\n"
        + "SS83eWh2SEhVVDdJZU1vRCtSYlNUMDgKL3lTQURFakl3YnVuM0dpVDNyTHpiQWc0" + "\n"
        + "Y0I2N1p0WS94Y1JvSyt3U252bWNpRjh6U2Y4PQogVXRrMkg5aGhNcXFwbEk1UzUx" + "\n"
        + "eExPVE1TSDNkT3QxRzduakVJN25wajQweFJsR291MGZOVERkc1d0TVJoCjJFRGJW" + "\n"
        + "aDJWYVJKZXcwQkxmMEQ3dzhURFlDK2lKbWNFdXArc1RMQ2lFNzBYMHd3emxVeTJR" + "\n"
        + "V2l2Y3RpOApFeXFMenlxVVNvRkk1SEQ5TDB3STBIU2JCVVp4c1NwUktEQzFoUjN0" + "\n"
        + "TEYxU2ZZeDgxY1E9CiBwUUU3c1MyZFREbzJkcjE0VFdMbVQ4dTBYdkxJckUremZv" + "\n"
        + "MDUxSmFIaGVHc1Y0ZUhpdy8wNzFvSmtEZHgKcjQrdnRaWkNacUd2L3NaS1hxUXJL" + "\n"
        + "MFMvcTM2MjF0c0hWL0FCODh0SDFsZWlpb05nQjdQZklSTUIwNFE1CmZMaVVxdFR2" + "\n"
        + "bm9nUnRDVW90U3JKejhrdWpXM2o4eGpsclJHT1BlUkNrV1ZyOEo0aWhwQT0KIEhy" + "\n"
        + "eGordmdNY25CZ3I4OExKMlBOZ3RreU9IODlLVEo4cHBXb0hvRk5RRmhSVjlpQURG" + "\n"
        + "bXVYZlFENnBveApWc0JZYW1ycTkyVjBYNm8rUmZnejV6OTg4WGg2a0ZVWDRyZU42" + "\n"
        + "bzZxZkZmRW10WkVZR3pPbFludlRxbTcKNkx3M2Rna2QxcnhtSWZjVUpWcmxCeXpM" + "\n"
        + "RnJQS2xZcGE2dW1jdVJ5S3p1NjVHeHEwbHZ3PQogZ1FGOG5qSFVGZ3piMlpIa0JI" + "\n"
        + "Wnc4YmNFYnZhN0VjSVhWdDdjaUJqWHlyQWpOaFFqMytEQWQrWG5KWUpFCmxJWXlt" + "\n"
        + "cHYyZDNoclBteXBxQy90NnBxQnJSYmszUFIrN2NSZ0Y5TGxsdzZXVXdiVFk0QUM3" + "\n"
        + "amJOcXd2LwpzeGhFbWtjVlNYQVNWYjFZL3hCRjlvQWJKZGJZa3NYRVVJN1NFS1Fu" + "\n"
        + "ajFSYnZyT3ZDUzQ9CiAxTUI1Wjc0UEF4MFBCV3NkRDJKTTVjSG1DZVQ1SzNlUFYy" + "\n"
        + "U3lpNFJWdCszOHZFMzFDWE8wc1ZyVUJHQngKNDhNUnJHUXUwTlNUOEJpY1JESFBU" + "\n"
        + "VVVOV0diNW5IOTVIeTFWT3ZadytoaWRFL29vRmlwK1J4d21GOFRoClpUajR3RnZG" + "\n"
        + "OXJSOXRwV05SVHVVd0s5U1hBN3JvTi9EbW9qMHRXSlFFWk9KbTV4V2psVT0KIEhh" + "\n"
        + "WXJTSklJcHFYMUh3RmwvYk5LVmt0UFkzaVNvRk5wQ1ptOHNXbVNPcjZDdmtXdjJh" + "\n"
        + "bnRBemwxQ1g5QQpOUVZIQkxUMVBKVU55ZmZtZUM1dnRBSWNpVmlCcE1haUVJanFx" + "\n"
        + "V2JpRmU3eERDOTlkOTJ3VXVHdGZ3MWsKRzdVT0NnTFJ3U0dmQmt6OU15TWQ0RmRY" + "\n"
        + "OWdlb3hPOERvVklGL213cnNHekVKb01PVXUwPQogVzc1VTJDa0dYeXVPSHJGMk9r" + "\n"
        + "YmVRejZZVlZsUkFxd1VHS2QzcHVsdkZvUlFwMVV2cFh6RW1lZWtZc0RJCnh5WnhH" + "\n"
        + "ZVQ5WWtRSzhXWEdiUEZKSDM0QkV2Um9sZUFyWVlneDg1V1VjZjRCUjJpNC9OMXRN" + "\n"
        + "TTVmdDJnTgp5ZGZBYkdkQWNjVW9sa3FCV3N3ank4QnZCNnZZRDZsbHN2SmJ4RXRM" + "\n"
        + "SE5WelBpSjZrWWc9CiBRY1d3bWxGd2lVNy9IM3lLZCt4M1JpM3k2WmNyOTZ0Nzhn" + "\n"
        + "TGNrRTNaLzRDcXZ2T0EvRUhqNkFjeDNGUU4KOUIwSmF3am1IMDRER2l6UTloMHRS" + "\n"
        + "WHkzdjh2bjJGbmU2M2NFR1dLY08rVko3emMrZWhtcm5oc241ZmFDCkw3ZG9tbCtv" + "\n"
        + "MmE3emMrcm9XWVA2V1JtSTJLYXBnSkFpemxJOXc2TU02WFJ5TEtyakhEdz0KIHRr" + "\n"
        + "NUVORkgrZk9aUFAwK1c1bkl0eEtGQUtVLzZJejVERDFqWXMvMTZlWGVTcTE3ckdY" + "\n"
        + "Z2cvUVhBMXRmMApqYXU3R0NDVTJTclBORTQ4a2Nxd0ZZTXR5czNPTG1qajg5Y3Jw" + "\n"
        + "MGZjVUwxb1pPaTVDNzIxMm5MUW1LcHgKQ2pIajNDbE1wOFA5Z2w5Um9ucGZoVmdY" + "\n"
        + "Y3FKQU14cUI0YkVvTDFlb0tWM2VPMUZtM09ZPQogWWJ5RVpDVW15VlFRSDJrMm9E" + "\n"
        + "NHlzNlNWdkwyeURTY2Y3Q09BaUVWaHhpQVZoVGZydCtweEZlMitHbVNICmxIeW5j" + "\n"
        + "U2Qzd21WbGxMdUJaMTREVFJSMlo5aUlKVktjVGxCZWlLTnBVS2JTTEhHcVJsTEwx" + "\n"
        + "ZFZYTXFlbQpsSmMxTm5RNDJZSUxBVGtKaU9Od25CK1p3TkowNzNFMjhwMElTa1cx" + "\n"
        + "Q1k2MVBjVTZwYnM9CiB6cjFnRTI4VGhlYkNvQmY1dm01OE9jdnkrcnR2cS9MTE5Z" + "\n"
        + "RTN4UW5Ba1VRcWRZZ0x2djFvT2ZGTitrRDkKK1dYSnBGbUU4a2lzRVl2MTRKbUNR" + "\n"
        + "UlR1U21vRDAvZE1WbjF3aDBkdkdROGFUNnExRXZqMm11ZVNkWTNCCk0xTGNGVHhx" + "\n"
        + "amJjOTNwbWYyU2RVUmUvbDQxMitINHZsNmNHM1NGVUV1Q0ErSlFsMXlSaz0KIGVM" + "\n"
        + "R3hvRGpmNnNiaGthVW5OZnhUcUN4NVpTZWhySjl4WFhoMEFMSHJuTENGaGZYTVQz" + "\n"
        + "ZkxlVC8yRmNwOQpHcGRXT0JtQmNIbk1LVzFrQ3NMZWt3ZytnWi9HOUduZ0tQRmVW" + "\n"
        + "WVV4enFSMDdnSzA0Zk9IZVZJNElKSjYKWHJNRGZiWDFGZkJJOXA0ZVI3aHpydzcz" + "\n"
        + "U0RodjdSY20xNmR4aTFhWUhQeWUvdnhBWGlZPQoxggI0MIICMAIBATCBrTCBnzEL" + "\n"
        + "MAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExDzANBgNVBAcTBklydmlu" + "\n"
        + "ZTEhMB8GA1UEChMYVW5pdmVyc2l0eSBvZiBDYWxpZm9ybmlhMQ8wDQYDVQQLEwZT" + "\n"
        + "cHJvdXQxFzAVBgNVBAMTDlNsaW5rZWUgU2VydmVyMR0wGwYJKoZIhvcNAQkBFg5m" + "\n"
        + "YWJlcnNAdWNpLmVkdQIJAMhRVpI9xcvqMAkGBSsOAwIaBQCgXTAYBgkqhkiG9w0B" + "\n"
        + "CQMxCwYJKoZIhvcNAQcBMBwGCSqGSIb3DQEJBTEPFw0xNDA1MTMwNDU4NTBaMCMG" + "\n"
        + "CSqGSIb3DQEJBDEWBBQ2tafj+lZo9MZ6TNURWna7TOv6WzANBgkqhkiG9w0BAQEF" + "\n"
        + "AASCAQCxE65+hpePiqUznmsCIZkaqc7r0GLvLd49oW/QxiHTbu8yJWmKlUCe6ltV" + "\n"
        + "Sb+htGquvDYXTlsvhk7+d5uSTTnc15FS/awtCAkEaOy1WUFFuUBBaDfki7m4x2nf" + "\n"
        + "FYlm7SEW+rViDBEZJts1g6Xo6plyrxIXqMlh134YsZG3usMIW+9kKXlBw6c3tfHU" + "\n"
        + "fWpgFI9kHQKOpYBgr/yTBNdALXIoCjjtD+mY2h7xyVHCKaPVElGypiv2/RzF7m/9" + "\n"
        + "1hjNaR+S3MoE6ueyAU+sLmDBhSpAJycSMhaOM8eCRE0gHWcBuIQfgGoYkhcFfg+O" + "\n"
        + "cvFXv9Im1DKjXwbg91n4272YBmWk" + "\n"
        + "-----END PKCS7-----";



    try {
      @SuppressWarnings("resource")
      ContentInfo ci = (ContentInfo) new PEMParser(new StringReader(auth)).readObject();

      CMSSignedData cms = new CMSSignedData(ci);

      ByteArrayOutputStream out = new ByteArrayOutputStream();

      cms.getSignedContent().write(out);

      byte [] bytes = out.toByteArray();

      // the data should be a string that is a series of base64 encoded bigintegers separated by spaces

      String base64 = new String(bytes);

      List<BigInteger> ret = new ArrayList<BigInteger>();

      for( String bn : base64.split(" ") ) {
        byte[] hex = Base64.decode(bn.getBytes(), Base64.DEFAULT);


        byte[] array = new byte[hex.length + 1];
        ByteBuffer bbuf = ByteBuffer.wrap(array);
        bbuf.put((byte)0);
        bbuf.put(hex);

        BigInteger in = new BigInteger(array);
        ret.add( in );

        System.out.println(in.toString(16));
      }

      assertEquals(80, ret.size());


    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
      assertTrue(false);
    }
  }
}
