package au.gov.dfat.lib.vdsncchecker

import java.lang.Exception

// separated for testing
class UVCIChecker{

    companion object {
        fun checkRange(value: String): UVCIRange{
            if(value.length < 4){
                return UVCIRange.invalid
            }

            var testLimit = 998999
            var specimenLimit = 999999

            //get the number string, dont include the first two alpha chars, or the last check digit
            var numberStr =  value.substring(2,value.length - 1)

            //get the check digit, the last digit
            var checkDigit = value.last().toString()

            //validate the check digit
            if(!validateCheckDigit(value.substring(0, value.length - 1), checkDigit) ){
                return UVCIRange.invalid
            }

            //check its numeric
            if (!isNumericAllowedCharacter(numberStr)) {
                return UVCIRange.invalid
            }

            //convert to number
            //was guard in SWIFT
            var number = try{
                numberStr.toInt()

            }catch(exception: Exception){
                throw VDSVerifyException(VDSVerifyError.UVCI_PARSE_ERROR)
            }


            //now check the range
            if(number <= testLimit)
            {
                return UVCIRange.test
            }

            if(number <= specimenLimit)
            {
                return UVCIRange.specimen
            }

            return UVCIRange.production
        }
    }

}

enum class UVCIRange{
    invalid,
    test,
    specimen,
    production
}

fun VDS.UVCIRange(){
    UVCIChecker.checkRange(data.msg.uvci)
}

private val set = "abcdefghijklmnopqrstuvwxyz"

private fun isNumericAllowedCharacter(value: String): Boolean {

    set.forEach { c ->
        if(value.indexOf(c, 0, true) > -1){
            return false;
        }
    }
    return true
}

private fun mapCharacter(character: String): UInt{
    var char = character.lowercase()
    if(set.indexOf(char) > -1){
        return (set.indexOf(char) + 10).toUInt()
    }
    var number = char.toIntOrNull()
    if(number != null && number is Int){
        return number.toUInt()
    }
    return 0u
}

/**
data validation function

:param: data The data that needs to be validated
:param: check The checksum string for the validation

:returns: Returns true if the data was valid
 */
private fun validateCheckDigit(data: String, check: String): Boolean {
    // The check digit calculation is as follows: each position is assigned a value; for the digits 0 to 9 this is
    // the value of the digits, for the letters A to Z this is 10 to 35, for the filler < this is 0. The value of
    // each position is then multiplied by its weight; the weight of the first position is 7, of the second it is 3,
    // and of the third it is 1, and after that the weights repeat 7, 3, 1, etcetera. All values are added together
    // and the remainder of the final value divided by 10 is the check digit.

    var i: Int = 1
    var dc: Int = 0
    var w: Array<Int> = arrayOf(7, 3, 1)
    var b0: UInt = 0u // "0"
    var b9: UInt = 9u // "9".toByteArray(Charsets.UTF_8).first().toUInt()
    var bA: UInt = 10u // "A".toByteArray(Charsets.UTF_8).first().toUInt()
    var bZ: UInt = 35u // "Z".toByteArray(Charsets.UTF_8).first().toUInt()
    var bK: UInt = 0u // "<".toByteArray(Charsets.UTF_8).first().toUInt()

    data.toCharArray().forEach { cha ->
        var d = 0
        var c: UInt = mapCharacter(cha.toString())


        if(c in b0..b9){
            d = (c - b0).toInt()
        } else if(c in bA..bZ){
            d = ((10u + c) - bA).toInt()
        } else if( c != bK ){
            return false
        }
        dc += d * w[(i-1)%3]
        //increment
        i += 1
    }


    try {

        if( (dc%10) != check.toInt() ){
            return false
        }
    }catch(exception: Exception) {
        return false
    }

    return true
}
