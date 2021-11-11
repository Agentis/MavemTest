package au.gov.dfat.lib.vdsncchecker

enum class VaultError {
    GET_STORE_FILE,
    LOAD_KEYSTORE,
    GET_ENTRY,
    SET_ENTRY,
    STORE_ENTRY
}

class VaultException(val error: VaultError): Exception() {

}