package au.gov.dfat.lib.vdsncchecker

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.*


@Keep
@Serializable
data class VDS (
    /** Data. The actual data for the VDS, including version, person info, vaccination info, etc. */
    val data: VDSData,
    /** Signature. The cryptographic signature used to verify the authenticity of the data. */
    val sig: VDSSig,
    /** Original JSON. Not part of VDS spec, used by VDSReader internally. */
    var originalJson: String = ""
)


@Keep
@Serializable
@Parcelize
data class VDSData (
    /** Header. Includes type of data, version and issuing country. */
    val hdr: VDSHdr,
    /** Message. Includes person and vaccination info. */
    val msg: VDSMsg
) : Parcelable

@Keep
@Serializable
@Parcelize
data class VDSHdr (
    /** Version. Required. */
    val v: Long,
    /** Type of data. Can be either `icao.test` or `icao.vacc`. Other types possible in the future. Required. */
    val t: String,
    @SerialName("is")
    /** Issuing country. In 3 letter country code format (e.g. `AUS`). Required. */
    val hdrIs: String
) : Parcelable

@Keep
@Serializable
@Parcelize
data class VDSMsg (
    /** Unique vaccination certificate identifier. Required. */
    val uvci: String,
    /** Person identification info. Required. */
    val pid: VDSPID,
    /** Array of vaccination events. Required. */
    val ve: List<VDSVe>
) : Parcelable

@Keep
@Serializable
@Parcelize
data class VDSPID (
    /** Date of birth. In `yyyy-MM-dd` format. Required if `i` (travel document number) is not provided. */
    val dob: String? = null,
    /** Name. A double space separates first and last name (e.g .`JANE  CITIZEN`). May be truncated. Required. */
    val n: String,
    /** Sex. `M` for male, `F` for female or `X` for unspecified. */
    val sex: String? = null,
    /** Unique travel document number. */
    val i: String? = null,
    /** Additional identifier at discretion of issuer. */
    val ai: String? = null
) : Parcelable

@Keep
@Serializable
@Parcelize
data class VDSVe (
    /** Vaccine type/subtype. Required. */
    val des: String,
    /** Brand name. Required. */
    val nam: String,
    /** Disease targeted by vaccine. Required. */
    val dis: String,
    /** Array of vaccination details. Required. */
    val vd: List<VDSVd>
) : Parcelable

@Keep
@Serializable
@Parcelize
data class VDSVd (
    /** Date of vaccination. In `yyyy-MM-dd` format. Required. */
    val dvc: String,
    /** Dose sequence number. Required. */
    val seq: Long,
    /** Country of vaccination. In 3 letter country code format (e.g. `AUS`). Required. */
    val ctr: String,
    /** Administering center. Required. */
    val adm: String,
    /** Vaccine lot number. Required. */
    val lot: String,
    /** Date of next vaccination. In `yyyy-MM-dd` format. */
    val dvn: String? = null
) : Parcelable

@Keep
@Serializable
data class VDSSig (
    /** Crypto algorithm used for the signature. Can be either `ES256`, `ES384` or `ES512` (typically `ES256`). Required. */
    val alg: String,
    /** Certificate used for the signature. In Base64 URL encoding (not the same as normal Base64!). Required. */
    val cer: String,
    /** Signature value. In Base64 URL encoding (not the same as normal Base64!). Required. */
    val sigvl: String
)

@Keep
data class CertificateData(
    val hash: String,
    val certificate: ByteArray,
    val clr: ByteArray,
    var issuingCountry: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CertificateData

        if (hash != other.hash) return false
        if (!certificate.contentEquals(other.certificate)) return false
        if (!clr.contentEquals(other.clr)) return false
        if(issuingCountry != other.issuingCountry) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + certificate.contentHashCode()
        result = 31 * result + clr.contentHashCode()
        return result
    }
}


@Keep
data class CSCAData(
    val hash: String,
    val certificate: ByteArray,
    val clr: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CSCAData

        if (hash != other.hash) return false
        if (!certificate.contentEquals(other.certificate)) return false
        if (!clr.contentEquals(other.clr)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + certificate.contentHashCode()
        result = 31 * result + clr.contentHashCode()
        return result
    }
}
