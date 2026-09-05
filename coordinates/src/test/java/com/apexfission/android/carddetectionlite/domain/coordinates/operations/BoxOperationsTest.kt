package com.apexfission.android.carddetectionlite.domain.coordinates.operations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBoxPS
import org.junit.Assert.assertEquals
import org.junit.Test

class BoxOperationsTest {

    @Test
    fun testArrangeImageBox2PWhenUnarranged() {
        val unarrangedBox = ImageBox2P(x = 100U, y = 200U, x2 = 10U, y2 = 20U)
        val arrangedBox = arrangeImageBox2P(unarrangedBox)

        assertEquals(ImageBox2P(x = 10U, y = 20U, x2 = 100U, y2 = 200U), arrangedBox)
        assertEquals(arrangedBox, unarrangedBox.arrange())
    }

    @Test
    fun testArrangeImageBox2PWhenAlreadyArranged() {
        val alreadyArrangedBox = ImageBox2P(x = 10U, y = 20U, x2 = 100U, y2 = 200U)
        val arrangedBox = arrangeImageBox2P(alreadyArrangedBox)

        assertEquals(alreadyArrangedBox, arrangedBox)
        assertEquals(alreadyArrangedBox, alreadyArrangedBox.arrange())
    }

    @Test
    fun testImageBoxPStoImageBox2p() {
        val boxPS = ImageBoxPS(x = 10U, y = 20U, width = 100, height = 150)
        val expectedBox2P = ImageBox2P(x = 10U, y = 20U, x2 = 110U, y2 = 170U)

        assertEquals(expectedBox2P, imageBoxPStoImageBox2p(boxPS))
        assertEquals(expectedBox2P, boxPS.toImageBox2p())
    }
}
