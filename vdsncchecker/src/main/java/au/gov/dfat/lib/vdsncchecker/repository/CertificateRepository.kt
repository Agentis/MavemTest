package au.gov.dfat.lib.vdsncchecker.repository
import au.gov.dfat.lib.vdsncchecker.CertificateData
import au.gov.dfat.lib.vdsncchecker.VDSVerifyError
import au.gov.dfat.lib.vdsncchecker.VDSVerifyException
import java.security.cert.CertificateFactory
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.cert.X509CertificateHolder
import java.lang.Exception
import java.security.cert.CertificateException

open class CertificateRepository : ICertificateRepository {
    private var certs: MutableList<CertificateData> = mutableListOf()
    override fun addCertificates(certificateData: Array<CertificateData>){
        certs.addAll(certificateData)
    }

    override fun removeCertificates(certificateData: Array<CertificateData>){
        certs.removeAll(certificateData)
    }

    override fun removeCertificate(certificateData: CertificateData){
        certs.remove(certificateData)
    }

    private fun findCN(certificate: ByteArray): String?{
        try{
            var holder = X509CertificateHolder(certificate)
            // get country entry, should always be first
            var rdn = holder.subject.rdNs[0]
            return IETFUtils.valueToString(rdn.first.value)
        }catch(exception: Exception){

        }

        return null

    }

    override fun addCertificate(certificate: ByteArray, hash: String, clr: ByteArray){
        //var cert = certFactory.generateCertificate(certificate.inputStream()) as X509Certificate
        var name = findCN(certificate)
        certs.plus(CertificateData(hash, certificate, clr, name))
    }

    // Find issuing CA cert

    override fun findIssuer(certificate: ByteArray, certFactory: CertificateFactory): CertificateData?{

        var ca = matchCertificateWithCa(certificate, certFactory)
        if(ca != null) {
            return ca
        }

        return null

    }

    // try to match CA cert with issued cert
    private fun matchCertificateWithCa(certificateData: ByteArray, certFactory: CertificateFactory): CertificateData?{

        try{
            var name = findCN(certificateData)
            var certificate = certFactory.generateCertificate(certificateData.inputStream())
            var certs = getCertificates(name)
            certs.iterator().forEach {

                var csca = certFactory.generateCertificate(it.certificate.inputStream())
                certificate.verify(csca.publicKey)
                return it
            }
        }catch(cEx: CertificateException){
            // error parsing certificate byte arrays
            // catch this outside
            throw cEx
        }

        return null
    }

    // get any certificates passed into the repository
    // if issuing country exists, filter by it
    protected open fun getCertificates(issuingCountry: String? = null): Array<CertificateData>{

        if(issuingCountry != null){
            return certs.filter { i -> i.issuingCountry == issuingCountry }.toTypedArray()
        }

        return certs.toTypedArray()
    }

}

