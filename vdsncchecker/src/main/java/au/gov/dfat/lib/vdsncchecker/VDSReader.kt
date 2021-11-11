/**
 * Copyright (c) 2021 Speedwell Pty Ltd.
 */

package au.gov.dfat.lib.vdsncchecker

import kotlinx.serialization.*
import kotlinx.serialization.json.*



class VDSReader {

    /**
     * Decodes VDS data from a VDS JSON string.
     *
     * @param jsonString The VDS JSON string
     * @return A 'VDS' if decoding is successful, otherwise throws an exception.
     * @throws VDSDecodeException if decoding fails.
     */
    @ExperimentalSerializationApi
    fun decodeVDSFromJsonString(jsonString: String): VDS {
        try {
            return Json.decodeFromString(VDS.serializer(), jsonString).apply {
                originalJson = jsonString
            }
        } catch (e: Exception) {
            throw VDSDecodeException(VDSDecodeError.JSON_DECODING_ERROR)

        }
    }

}
