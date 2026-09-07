package com.apexfission.android.carddetectionlite.domain.coordinates

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.SpaceRelationship
import com.apexfission.android.carddetectionlite.domain.coordinates.models.chain
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.translate
import org.junit.Assert.assertEquals
import org.junit.Test

class TransformationChainTest {

    @Test
    fun testBranchingChainTranslation() {
        val original = ImageSpace(width = 1000U, height = 1000U)
        val crop2 = ImageSpace(width = 400U, height = 400U, xOffset = 300U, yOffset = 300U)
        val scaled = ImageSpace(width = 2000U, height = 2000U, xScale = 2f, yScale = 2f)
        val cropped = ImageSpace(width = 800U, height = 800U, xOffset = 600U, yOffset = 600U)

        // crop2 -> Original -> scaled -> cropped
        val chain = crop2.chain(original, SpaceRelationship.Parent)
            .chain(scaled, SpaceRelationship.Child)
            .chain(cropped, SpaceRelationship.Child)

        val pointInCrop2 = ImagePoint(50, 50)
        val translated = pointInCrop2.translate(chain)
        assertEquals(ImagePoint(100, 100), translated)
    }
}
