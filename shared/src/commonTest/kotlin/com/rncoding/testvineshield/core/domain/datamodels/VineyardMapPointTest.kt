package com.rncoding.testvineshield.core.domain.datamodels
import kotlin.test.*
class VineyardMapPointTest {
    @Test fun normalPointKeepsLatitudeLongitudeOrder() {
        val p=assertNotNull(VineyardMapPoint.from(42.7,23.3)); assertEquals(42.7,p.latitude); assertEquals(23.3,p.longitude,0.000000001)
    }
    @Test fun repeatedWorldLongitudesNormalizeAcrossAntimeridian() {
        assertEquals(-180.0,VineyardMapPoint.from(10.0,180.0)?.longitude)
        assertEquals(-179.0,VineyardMapPoint.from(10.0,181.0)?.longitude)
        assertEquals(179.0,VineyardMapPoint.from(10.0,-181.0)?.longitude)
        assertEquals(23.0,VineyardMapPoint.from(10.0,743.0)?.longitude)
    }
    @Test fun nonfiniteAndInvalidLatitudesAreRejected() {
        for(v in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)) {
            assertNull(VineyardMapPoint.from(v,0.0)); assertNull(VineyardMapPoint.from(0.0,v))
        }
        assertNull(VineyardMapPoint.from(90.01,0.0)); assertNull(VineyardMapPoint.from(-90.01,0.0))
    }
    @Test fun zeroAndSouthernWesternCoordinatesAreNotUnknown() {
        assertEquals(0.0,VineyardMapPoint.from(0.0,0.0)?.latitude)
        val p=assertNotNull(VineyardMapPoint.from(-33.9,-70.7)); assertEquals(-33.9,p.latitude); assertEquals(-70.7,p.longitude,0.000000001)
    }
}
