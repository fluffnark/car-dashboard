package com.fluffnark.motoringdashboard.car

import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CarManifestTest {
    @Test
    fun minimumCarApiLevelIsDeclaredOnApplication() {
        val application = RuntimeEnvironment.getApplication()
        val applicationInfo = application.packageManager.getApplicationInfo(
            application.packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
        )

        assertEquals(8, applicationInfo.metaData.getInt("androidx.car.app.minCarApiLevel"))
    }

    @Test
    fun appDeclaresPoiCategoryAndMapSurfacePermissions() {
        val application = RuntimeEnvironment.getApplication()
        assertEquals(PackageManager.PERMISSION_GRANTED,
            application.packageManager.checkPermission("androidx.car.app.MAP_TEMPLATES", application.packageName))
        assertEquals(PackageManager.PERMISSION_GRANTED,
            application.packageManager.checkPermission("androidx.car.app.ACCESS_SURFACE", application.packageName))
        val services = application.packageManager.queryIntentServices(
            android.content.Intent("androidx.car.app.CarAppService").setPackage(application.packageName),
            PackageManager.ResolveInfoFlags.of(PackageManager.GET_RESOLVED_FILTER.toLong()))
        assertTrue(services.any { it.filter?.hasCategory("androidx.car.app.category.POI") == true })
    }
}
