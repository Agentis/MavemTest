package au.gov.dfat.lib.vdsncchecker

import au.gov.dfat.lib.vdsncchecker.repository.ICertificateRepository

interface IVDSAuthenticator {
    fun verifyVDS(vds: VDS, cscaCertData: ByteArray, cscaCertSHA256Hash: String, crlData: Collection<ByteArray>): Boolean
    fun verifyVDS(vds: VDS, certificateRepository: ICertificateRepository): Boolean
}