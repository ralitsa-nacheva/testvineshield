package com.rncoding.testvineshield.core.domain.datamodels

/** Map renderers may expose repeated-world longitudes. Normalize only that representation. */
data class VineyardMapPoint private constructor(val latitude: Double,val longitude: Double) {
    companion object {
        fun from(latitude: Double,longitude: Double): VineyardMapPoint? {
            if(!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0) return null
            val wrapped=longitude % 360.0
            val normalized=when { wrapped < -180.0 -> wrapped+360.0; wrapped>=180.0 -> wrapped-360.0; else -> wrapped }
            return VineyardMapPoint(latitude,if(normalized==0.0) 0.0 else normalized)
        }
    }
}
