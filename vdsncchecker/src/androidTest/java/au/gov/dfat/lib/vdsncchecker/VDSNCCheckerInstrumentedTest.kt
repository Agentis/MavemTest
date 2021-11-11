package au.gov.dfat.lib.vdsncchecker

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class VDSNCCheckerInstrumentedTest {

    private val vdsReader = VDSReader()
    private var authenticator: IVDSAuthenticator = VDSAuthenticator()

    // SHA256 hash of our CSCA certificate data
    private val cscaCertSHA256Hash = "35623a89f6b40cb4888e05147f7d16d27f7bf754aad563c37fef6e1a0e7ff5cf"

    // CSCA certificate
    private val cscaCertData: ByteArray = Base64.decode("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", 0)

    // CRL without serial number
    private val crlWithoutSerialNumberData: ByteArray = Base64.decode("MIIC3zCByAIBATANBgkqhkiG9w0BAQsFADBlMQswCQYDVQQGEwJBVTEMMAoGA1UECgwDR09WMQ0wCwYDVQQLDARERkFUMQwwCgYDVQQLDANBUE8xKzApBgNVBAMMIlBhc3Nwb3J0IENvdW50cnkgU2lnbmluZyBBdXRob3JpdHkXDTIxMDgxOTAzNTY1NVoXDTIxMTExNzAzNTY1NVqgLzAtMAoGA1UdFAQDAgFAMB8GA1UdIwQYMBaAFDYXwef1Z5VxLjd1cI5VgzGG6TgOMA0GCSqGSIb3DQEBCwUAA4ICAQDFCv8xG3oM0enVAP/7W4IDrbbyq8yZLPX4kbWqfAnaveLgMjHu+eo9aLDELjDHK859W4cguBEDmTZ9ewJ1eiJouvveLETrUnIn4dHEo+AKVuClvkYFPI8iexZl+kX1GzQpLBQLTTUA4PAskLF+YsDBMZ4eE66A2U5q17yFVsHFgwXKj0rKXbLsD+IvJkZKEDea5wJMj0wgaeF+y32t3rQy5Bi/0hMe95YXonlW33mxLOMQ6fbzEJeibwJ393QTvw4Y9Ohy6kMjaEVlxVuyVmtJaquW4ITI3idQTsQlZBI75BBH/nVAru38hgFEb6gmnMz4d4tUG/9PSvVhyHEHDd8g/OJGEkDnFDsNPh0e62Jkj9b0cE5bcWHDXmWtj33BOXA2jxuVE5aOzQRtkroiiarnwXrndZgh0y5WndpGNzsxkne+hXC09tvY0pnJ/DixP8A2vv33vzWd0JjEVTG1VuJJUIDvmXepMuKMsEwCfMrDgVd89uepLke61w/atA++MhB30/sU/8T+vrqAy/GUQGHRpdy8oQ1LS4yFbeWBxzlEpodq55SJisxPsup97+8QHjjJx+F3ELk6GkCv/M6ppplbmvkfW2rG2ll4O0VAtLlvykHhNEyzzwBjZA/IWmd/xwaBaHboq72LKGc1Hhr7VYRBfBodiPf1S6UAX4gbuDcmmg==", 0)

    // CRL with serial number
    // Note: This does not contain a serial number currently, as we have not been able to get a valid CRL for this test case
    // TODO: Replace with a CRL that contains a serial number
    private val crlWithSerialNumberData: ByteArray = Base64.decode("MIIC3zCByAIBATANBgkqhkiG9w0BAQsFADBlMQswCQYDVQQGEwJBVTEMMAoGA1UECgwDR09WMQ0wCwYDVQQLDARERkFUMQwwCgYDVQQLDANBUE8xKzApBgNVBAMMIlBhc3Nwb3J0IENvdW50cnkgU2lnbmluZyBBdXRob3JpdHkXDTIxMDgxOTAzNTY1NVoXDTIxMTExNzAzNTY1NVqgLzAtMAoGA1UdFAQDAgFAMB8GA1UdIwQYMBaAFDYXwef1Z5VxLjd1cI5VgzGG6TgOMA0GCSqGSIb3DQEBCwUAA4ICAQDFCv8xG3oM0enVAP/7W4IDrbbyq8yZLPX4kbWqfAnaveLgMjHu+eo9aLDELjDHK859W4cguBEDmTZ9ewJ1eiJouvveLETrUnIn4dHEo+AKVuClvkYFPI8iexZl+kX1GzQpLBQLTTUA4PAskLF+YsDBMZ4eE66A2U5q17yFVsHFgwXKj0rKXbLsD+IvJkZKEDea5wJMj0wgaeF+y32t3rQy5Bi/0hMe95YXonlW33mxLOMQ6fbzEJeibwJ393QTvw4Y9Ohy6kMjaEVlxVuyVmtJaquW4ITI3idQTsQlZBI75BBH/nVAru38hgFEb6gmnMz4d4tUG/9PSvVhyHEHDd8g/OJGEkDnFDsNPh0e62Jkj9b0cE5bcWHDXmWtj33BOXA2jxuVE5aOzQRtkroiiarnwXrndZgh0y5WndpGNzsxkne+hXC09tvY0pnJ/DixP8A2vv33vzWd0JjEVTG1VuJJUIDvmXepMuKMsEwCfMrDgVd89uepLke61w/atA++MhB30/sU/8T+vrqAy/GUQGHRpdy8oQ1LS4yFbeWBxzlEpodq55SJisxPsup97+8QHjjJx+F3ELk6GkCv/M6ppplbmvkfW2rG2ll4O0VAtLlvykHhNEyzzwBjZA/IWmd/xwaBaHboq72LKGc1Hhr7VYRBfBodiPf1S6UAX4gbuDcmmg==", 0)

    // Valid VDS JSON
    private val validVDSJson = """
{
    "data": {
        "hdr": {
            "is": "AUS",
            "t": "icao.vacc",
            "v": 1
        },
        "msg": {
            "pid": {
                "dob": "2004-03-20",
                "i": "PA0940385",
                "n": "CITIZEN  JOHN",
                "sex": "M"
            },
            "uvci": "VB0009990025",
            "ve": [{
                "des": "XM68M6",
                "dis": "RA01.0",
                "nam": "AstraZeneca Vaxzevria",
                "vd": [{
                    "adm": "Community Nurse",
                    "ctr": "AUS",
                    "dvc": "2021-04-02",
                    "lot": "317711P",
                    "seq": 1
                }]
            }, {
                "des": "XM68M6",
                "dis": "RA01.0",
                "nam": "Moderna Spikevax",
                "vd": [{
                    "adm": "Primary Health",
                    "ctr": "AUS",
                    "dvc": "2021-10-03",
                    "lot": "Not recorded",
                    "seq": 2
                }]
            }]
        }
    },
    "sig": {
        "alg": "ES256",
        "cer": "MIIDhDCCAWygAwIBAgICGK0wDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCQVUxDDAKBgNVBAoMA0dPVjENMAsGA1UECwwEREZBVDEMMAoGA1UECwwDQVBPMSswKQYDVQQDDCJQYXNzcG9ydCBDb3VudHJ5IFNpZ25pbmcgQXV0aG9yaXR5MB4XDTIxMDgzMTE0MDAwMFoXDTMxMDkzMDEzNTk1OVowHDELMAkGA1UEBhMCQVUxDTALBgNVBAMTBERGQVQwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARSVpOyHuLjm01TB1iLBr3SrUp2GkQlM-mPqubbW3mjs0DTeRKrfVTSkkZNgOGj_DB_fo3p8qGy8UVgT4DQRVhIo1IwUDAWBgdngQgBAQYCBAswCQIBADEEEwJOVjAVBgNVHSUBAf8ECzAJBgdngQgBAQ4CMB8GA1UdIwQYMBaAFDYXwef1Z5VxLjd1cI5VgzGG6TgOMA0GCSqGSIb3DQEBCwUAA4ICAQCh_Qc5i6-vewGqinR9EdUpsl0P4jqg0pdx7hyOtPgYOwbTOegJyZOjyWZyuLlxGYuvCHqbrnATMedoIoUJzt8GxHA-4v5TUN2yEbRFXev8ur_0Y3uF4WXFr93Zl0LV78PBNZwXKfZEC6oTN_eVgtR37GdnYsWno0SuhR4fJo8JC_blivas8BJt78Hg8VhvWSK3uT0T58eYQjQhbsXV-BxJ2kSspdvkUF6-arLHh6DVS3ATPAGIm6fEvF4AxnLq5OSHOC3zZR0SR9XntYxEwjo_bW8O0Se8qa5mIBpXmvlwh0Ij6sqVwEskvkM30GmQGfZh5VjFujN2AZnwpjOjK0R-JvR3u6jsBJqVMgm75HgezOzayNiaqzhitrgg5KpO3gK_j3C-Doj5iPAm7I_63GyjUi8ZnqVUZ37UxM19uX2SvhTTQ70nL-zHNfHOyBXJgzMi4Zkor2uagHPz-W1XvNVwGEfFAu-nEyIOKBndHwnvSomL54yBv83X2yAQsoYggU18LNXMHUonTJ_ug7FU0LEX3qA1TeARJ4WBFNjysrBXQepVLowcbtvrhLFjocHjmCp3z17xUoKGI6daajCbvedXgeeSWSD5CuMAXpdN3Yml7VdW7PCK4DD0E_raw6d_wKNGSYAh0TBpNLxnunquai-gFIjgf4iRoys5F35KwmvpZw==",
        "sigvl": "70r84nJGP4hnkx3fZkHfhRG2AaGrC1wKgkzbLWkBZpiQ0DJUQ_x9WJGTqMAAjWGfUDcoAtsmKlg_HrDhNChjKg=="
    }
}
"""

    // Invalid VDS JSON - identical to the valid JSON, except the name was modified (JOHN to JOHNX) to make the signature invalid
    private val invalidVDSJson = """
{
    "data": {
        "hdr": {
            "is": "AUS",
            "t": "icao.vacc",
            "v": 1
        },
        "msg": {
            "pid": {
                "dob": "2004-03-20",
                "i": "PA0940385",
                "n": "CITIZEN  JOHNX",
                "sex": "M"
            },
            "uvci": "VB0009990025",
            "ve": [{
                "des": "XM68M6",
                "dis": "RA01.0",
                "nam": "AstraZeneca Vaxzevria",
                "vd": [{
                    "adm": "Community Nurse",
                    "ctr": "AUS",
                    "dvc": "2021-04-02",
                    "lot": "317711P",
                    "seq": 1
                }]
            }, {
                "des": "XM68M6",
                "dis": "RA01.0",
                "nam": "Moderna Spikevax",
                "vd": [{
                    "adm": "Primary Health",
                    "ctr": "AUS",
                    "dvc": "2021-10-03",
                    "lot": "Not recorded",
                    "seq": 2
                }]
            }]
        }
    },
    "sig": {
        "alg": "ES256",
        "cer": "MIIDhDCCAWygAwIBAgICGK0wDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCQVUxDDAKBgNVBAoMA0dPVjENMAsGA1UECwwEREZBVDEMMAoGA1UECwwDQVBPMSswKQYDVQQDDCJQYXNzcG9ydCBDb3VudHJ5IFNpZ25pbmcgQXV0aG9yaXR5MB4XDTIxMDgzMTE0MDAwMFoXDTMxMDkzMDEzNTk1OVowHDELMAkGA1UEBhMCQVUxDTALBgNVBAMTBERGQVQwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARSVpOyHuLjm01TB1iLBr3SrUp2GkQlM-mPqubbW3mjs0DTeRKrfVTSkkZNgOGj_DB_fo3p8qGy8UVgT4DQRVhIo1IwUDAWBgdngQgBAQYCBAswCQIBADEEEwJOVjAVBgNVHSUBAf8ECzAJBgdngQgBAQ4CMB8GA1UdIwQYMBaAFDYXwef1Z5VxLjd1cI5VgzGG6TgOMA0GCSqGSIb3DQEBCwUAA4ICAQCh_Qc5i6-vewGqinR9EdUpsl0P4jqg0pdx7hyOtPgYOwbTOegJyZOjyWZyuLlxGYuvCHqbrnATMedoIoUJzt8GxHA-4v5TUN2yEbRFXev8ur_0Y3uF4WXFr93Zl0LV78PBNZwXKfZEC6oTN_eVgtR37GdnYsWno0SuhR4fJo8JC_blivas8BJt78Hg8VhvWSK3uT0T58eYQjQhbsXV-BxJ2kSspdvkUF6-arLHh6DVS3ATPAGIm6fEvF4AxnLq5OSHOC3zZR0SR9XntYxEwjo_bW8O0Se8qa5mIBpXmvlwh0Ij6sqVwEskvkM30GmQGfZh5VjFujN2AZnwpjOjK0R-JvR3u6jsBJqVMgm75HgezOzayNiaqzhitrgg5KpO3gK_j3C-Doj5iPAm7I_63GyjUi8ZnqVUZ37UxM19uX2SvhTTQ70nL-zHNfHOyBXJgzMi4Zkor2uagHPz-W1XvNVwGEfFAu-nEyIOKBndHwnvSomL54yBv83X2yAQsoYggU18LNXMHUonTJ_ug7FU0LEX3qA1TeARJ4WBFNjysrBXQepVLowcbtvrhLFjocHjmCp3z17xUoKGI6daajCbvedXgeeSWSD5CuMAXpdN3Yml7VdW7PCK4DD0E_raw6d_wKNGSYAh0TBpNLxnunquai-gFIjgf4iRoys5F35KwmvpZw==",
        "sigvl": "70r84nJGP4hnkx3fZkHfhRG2AaGrC1wKgkzbLWkBZpiQ0DJUQ_x9WJGTqMAAjWGfUDcoAtsmKlg_HrDhNChjKg=="
    }
}
"""

    // Corrupt/non-VDS JSON
    private val nonVDSJson = """
{
    "isThisAVDS": false
}
"""

    @Test
    fun testValidVDS() {
        // Decode VDS
        val vdsReader = VDSReader()
        val vds = try { vdsReader.decodeVDSFromJsonString(validVDSJson) } catch (_: Exception) { null }
        assertNotNull(vds)

        val crlList = ArrayList<ByteArray>()
        crlList.add(crlWithoutSerialNumberData)

        // Verify VDS
        val isVdsValid = try { authenticator.verifyVDS(vds as VDS, cscaCertData, cscaCertSHA256Hash, crlList) } catch (_: Exception) { null }
        assertNotNull(isVdsValid)
        assertEquals(isVdsValid, true)
    }

    @Test
    fun testInvalidVDS() {
        // Decode VDS
        val vds = try { vdsReader.decodeVDSFromJsonString(invalidVDSJson) } catch (_: Exception) { null }
        assertNotNull(vds)

        val crlList = ArrayList<ByteArray>()
        crlList.add(crlWithoutSerialNumberData)

        // Verify VDS
        try {
            authenticator.verifyVDS(vds as VDS, cscaCertData, cscaCertSHA256Hash, crlList)
            fail()
        } catch (e: VDSVerifyException) {
            assertEquals(e.error, VDSVerifyError.VERIFY_SIGNATURE_ERROR)
        }
    }

    @Test
    fun testNonVDS() {
        // Decode VDS

        try {
            vdsReader.decodeVDSFromJsonString(nonVDSJson)
            fail()
        } catch (e: VDSDecodeException) {
            assertEquals(e.error, VDSDecodeError.JSON_DECODING_ERROR)
        }
    }

    @Test
    fun testRevokedVDSWithOfflineCRL() {
        // Note: This test will fail due to the CRL not containing a serial number

        // Decode VDS
        val vds = try { vdsReader.decodeVDSFromJsonString(validVDSJson) } catch (_: Exception) { null }
        assertNotNull(vds)

        val crlList = ArrayList<ByteArray>()
        crlList.add(crlWithSerialNumberData)

        // Verify VDS
        try {
            authenticator.verifyVDS(vds as VDS, cscaCertData, cscaCertSHA256Hash, crlList)
            fail()
        } catch (e: VDSVerifyException) {
            assertEquals(e.error, VDSVerifyError.VERIFY_BSC_CERT_NOT_IN_CRL_ERROR)
        }
    }

    @Test
    fun checkUVCISpecimen(){
        try{
            var vds = UVCIChecker.checkRange("VB0009990025")
            assert(vds == UVCIRange.specimen)
        }catch(exception: java.lang.Exception){
            fail()
        }

    }

    @Test
    fun checkUVCISpecimen2(){
        try{
            var vds = UVCIChecker.checkRange("VB0009990012")
            assert(vds == UVCIRange.specimen)
        }catch(exception: java.lang.Exception){
            fail()
        }

    }

    @Test
    fun checkUVCITest(){
        try{
            var vds = UVCIChecker.checkRange("VB0000003498")
            assert(vds == UVCIRange.test)
        }catch(exception: java.lang.Exception){
            fail()
        }

    }

    @Test
    fun setupCrlManager(){
        val data = ArrayList<CRLStoreWrapper>()

        CRLManager.setContext(InstrumentationRegistry.getInstrumentation().context)

        data.add(CRLStoreWrapper(URL("https", "pkddownload1.icao.int", "/CRLs/AUS.crl"), ByteArray(0)))
        CRLManager.setupCRLs(data.toTypedArray())

        assertEquals(data.size, CRLManager.crls.size)

        val countDownLatch = CountDownLatch(data.size)

        if(CRLManager.isUpdateOverdue())
        {
            CRLManager.updateCRLData(){
                println("All $it downloads complete")

                CRLManager.crls.forEach {
                    println("${it.url} : ${it.data?.size}")
                    countDownLatch.countDown()
                }
            }

            countDownLatch.await()
        }

        assert(!CRLManager.isUpdateOverdue())
    }

    @Test
    fun startAutoUpdate(){
        // data setup
        val data = ArrayList<CRLStoreWrapper>()

        CRLManager.setContext(InstrumentationRegistry.getInstrumentation().context)

        data.add(CRLStoreWrapper(URL("https", "pkddownload1.icao.int", "/CRLs/AUS.crl"), ByteArray(0)))
        CRLManager.setupCRLs(data.toTypedArray())

        val countDownLatch = CountDownLatch(1)

        // subscribe to the event which gets raised when the auto update finishes
        CRLManagerUpdateEvent.subscribe<Int>()!!.subscribe(){
            assertEquals(CRLManager.crls.size, it)
            countDownLatch.countDown()
        }

        CRLManager.startAutoUpdatingCRLData(1)

        countDownLatch.await(5, TimeUnit.SECONDS) // 5 seconds should be enough for the 1 second wait to elapse and do the work
    }




}