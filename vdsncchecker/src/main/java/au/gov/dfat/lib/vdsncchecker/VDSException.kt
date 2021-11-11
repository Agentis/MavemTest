package au.gov.dfat.lib.vdsncchecker

enum class VDSDecodeError {
    JSON_DECODING_ERROR
}

enum class VDSVerifyError {
    CREATE_CERTIFICATE_FACTORY_ERROR,
    PARSE_BSC_CERT_ERROR,
    PARSE_SIGNATURE_ERROR,
    PARSE_JSON_ERROR,
    LOAD_BSC_CERT_ERROR,
    LOAD_CSCA_CERT_ERROR,
    VERIFY_CSCA_CERT_HASH_ERROR,
    LOAD_CRL_ERROR,
    VERIFY_CRL_ERROR,
    VERIFY_BSC_CERT_NOT_IN_CRL_ERROR,
    VERIFY_BSC_CERT_AKI_MATCHES_CSCA_CERT_AKI_ERROR,
    VERIFY_BSC_CERT_PATH_INCLUDES_CSCA_CERT_ERROR,
    VERIFY_SIGNATURE_ERROR,
    UVCI_PARSE_ERROR

}

class VDSDecodeException(val error: VDSDecodeError): Exception() {

}

class VDSVerifyException(val error: VDSVerifyError): Exception() {

}