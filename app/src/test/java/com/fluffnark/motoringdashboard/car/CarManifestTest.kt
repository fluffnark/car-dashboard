package com.fluffnark.motoringdashboard.car

import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
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
}
