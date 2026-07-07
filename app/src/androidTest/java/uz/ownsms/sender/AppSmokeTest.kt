package uz.ownsms.sender

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @Test
    fun packageName_isCorrect() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals("uz.ownsms.sender", ctx.packageName)
    }
}
