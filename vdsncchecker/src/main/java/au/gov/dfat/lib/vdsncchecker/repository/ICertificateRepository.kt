package au.gov.dfat.lib.vdsncchecker.repository
import au.gov.dfat.lib.vdsncchecker.CertificateData
import java.security.cert.CertificateFactory

interface ICertificateRepository {
    fun findIssuer(certificate: ByteArray, certFactory: CertificateFactory): CertificateData?
    fun addCertificate(certificate: ByteArray, hash: String, clr: ByteArray)
    fun addCertificates(certificateData: Array<CertificateData>)
    fun removeCertificates(certificateData: Array<CertificateData>)
    fun removeCertificate(certificateData: CertificateData)
}