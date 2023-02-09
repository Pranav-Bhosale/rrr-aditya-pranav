package com.esop.schema

import com.esop.PlatformFeeLessThanZeroException
import java.math.BigInteger
import com.esop.PLATFORM_COMISSION
import java.lang.Math.round
import kotlin.math.roundToLong

class PlatformFee {

    companion object {

        var totalPlatformFee: BigInteger = BigInteger("0")

        fun addPlatformFee(fee: Long) {
            if (fee < 0)
                throw PlatformFeeLessThanZeroException()
            totalPlatformFee += fee.toBigInteger()
        }

        fun getPlatformFee(): BigInteger {
            return totalPlatformFee
        }

        fun calculatePlatformFee(esopType:String, orderExecutionPrice: Long): Long {
            if(esopType == "NON_PERFORMANCE")
                return (orderExecutionPrice * PLATFORM_COMISSION).roundToLong()
            return 0L
        }

    }


}