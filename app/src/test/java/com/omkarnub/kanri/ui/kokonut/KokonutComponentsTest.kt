package com.omkarnub.kanri.ui.kokonut

import androidx.compose.ui.geometry.Offset
import com.omkarnub.kanri.ui.common.kokonut.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStreamReader
import kotlin.math.abs

class KokonutComponentsTest {

    private fun loadResourceAsString(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: throw IllegalStateException("Resource not found: $name")
        return InputStreamReader(stream).use { it.readText() }
    }

    @Test
    fun testAvatarGoldenVectors30Seeds() {
        val jsonStr = loadResourceAsString("avatar_golden_vectors.json")
        val array = JSONArray(jsonStr)
        assertEquals(30, array.length())

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val seed = item.getString("seed")
            val expectedHash = item.getLong("hash")

            // 1. Hash assertion
            val actualHash = avatarHashString(seed)
            assertEquals("Hash mismatch for seed: $seed", expectedHash, actualHash)

            // 2. Face generation assertion
            val face = createAvatarFace(seed)
            val expectedFace = item.getJSONObject("face")
            val expectedShape = expectedFace.getJSONObject("shape")

            val s = face.shape
            assertEquals(expectedShape.getDouble("headW"), s.headW.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("headH"), s.headH.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("headSquare"), s.headSquare.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("chin"), s.chin.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("hairVolume"), s.hairVolume.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("hairFringe"), s.hairFringe.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("hairPart"), s.hairPart.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("hairLength"), s.hairLength.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("eyeGap"), s.eyeGap.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("eyeY"), s.eyeY.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("eyeRx"), s.eyeRx.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("eyeRy"), s.eyeRy.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("browTilt"), s.browTilt.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("browLift"), s.browLift.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("browWeight"), s.browWeight.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("mouthW"), s.mouthW.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("mouthCurve"), s.mouthCurve.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("mouthOpen"), s.mouthOpen.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("nose"), s.nose.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("glasses"), s.glasses.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("cheeks"), s.cheeks.toDouble(), 0.01)
            assertEquals(expectedShape.getDouble("shoulders"), s.shoulders.toDouble(), 0.01)

            // Colors assertion
            val expBg = expectedFace.getJSONArray("bg")
            assertEquals(expBg.getInt(0), face.bg.r)
            assertEquals(expBg.getInt(1), face.bg.g)
            assertEquals(expBg.getInt(2), face.bg.b)

            val expSkin = expectedFace.getJSONArray("skin")
            assertEquals(expSkin.getInt(0), face.skin.r)
            assertEquals(expSkin.getInt(1), face.skin.g)
            assertEquals(expSkin.getInt(2), face.skin.b)

            val expHair = expectedFace.getJSONArray("hair")
            assertEquals(expHair.getInt(0), face.hair.r)
            assertEquals(expHair.getInt(1), face.hair.g)
            assertEquals(expHair.getInt(2), face.hair.b)

            val expShirt = expectedFace.getJSONArray("shirt")
            assertEquals(expShirt.getInt(0), face.shirt.r)
            assertEquals(expShirt.getInt(1), face.shirt.g)
            assertEquals(expShirt.getInt(2), face.shirt.b)

            // Description assertion
            assertEquals(expectedFace.getString("description"), face.description)
        }
    }

    @Test
    fun testMathGoldenVectors() {
        val jsonStr = loadResourceAsString("math_golden_vectors.json")
        val root = JSONObject(jsonStr)

        // 1. Wrap tests
        val wrapArray = root.getJSONArray("wrap")
        for (i in 0 until wrapArray.length()) {
            val item = wrapArray.getJSONObject(i)
            val input = item.getDouble("input").toFloat()
            val expected = item.getDouble("output").toFloat()
            val actual = spendingRingWrap(input)
            assertEquals("wrap failed for $input", expected, actual, 0.001f)
        }

        // 2. Polar tests
        val polarArray = root.getJSONArray("polar")
        for (i in 0 until polarArray.length()) {
            val item = polarArray.getJSONObject(i)
            val deg = item.getDouble("deg").toFloat()
            val pt = item.getJSONArray("pt")
            val expectedX = pt.getDouble(0).toFloat()
            val expectedY = pt.getDouble(1).toFloat()
            val actual = spendingRingPolar(deg)
            assertEquals("polar X for $deg", expectedX, actual.first, 0.01f)
            assertEquals("polar Y for $deg", expectedY, actual.second, 0.01f)
        }

        // 3. Arc tests
        val arcArray = root.getJSONArray("arc")
        for (i in 0 until arcArray.length()) {
            val item = arcArray.getJSONObject(i)
            val from = item.getDouble("from").toFloat()
            val to = item.getDouble("to").toFloat()
            val expected = item.getString("result")
            val actual = spendingRingArc(from, to)
            assertEquals("arc failed for $from->$to", expected, actual)
        }

        // 4. Slide Band tests
        val bandArray = root.getJSONArray("band")
        for (i in 0 until bandArray.length()) {
            val item = bandArray.getJSONObject(i)
            val raw = item.getDouble("raw").toFloat()
            val max = item.getDouble("max").toFloat()
            val expected = item.getDouble("output").toFloat()
            val actual = slideBand(raw, max)
            assertEquals("band failed for $raw, max=$max", expected, actual, 0.01f)
        }

        // 5. Stick-slip tests
        val stickArray = root.getJSONArray("stickSlip")
        for (i in 0 until stickArray.length()) {
            val item = stickArray.getJSONObject(i)
            val pull = item.getDouble("pull").toFloat()
            val expected = item.getDouble("output").toFloat()
            val actual = tearStickSlip(pull)
            assertEquals("stickSlip failed for $pull", expected, actual, 0.001f)
        }

        // 6. Farthest corner tests
        val cornerArray = root.getJSONArray("farthestCorner")
        for (i in 0 until cornerArray.length()) {
            val item = cornerArray.getJSONObject(i)
            val at = item.getJSONObject("at")
            val ox = at.getDouble("x").toFloat()
            val oy = at.getDouble("y").toFloat()
            val w = item.getDouble("w").toFloat()
            val h = item.getDouble("h").toFloat()
            val expected = item.getDouble("output").toFloat()
            val actual = spoilerFarthestCorner(Offset(ox, oy), w, h)
            assertEquals("farthestCorner failed", expected, actual, 0.01f)
        }
    }

    @Test
    fun testAmountPadPureFunctions() {
        // Initial empty + "5"
        var res = amountPadProcessKey("", "5")
        assertTrue(res is AmountPadKeyResult.Success)
        var raw = (res as AmountPadKeyResult.Success).newRawInput
        assertEquals("5", raw)

        // Type "."
        res = amountPadProcessKey(raw, ".")
        assertTrue(res is AmountPadKeyResult.Success)
        raw = (res as AmountPadKeyResult.Success).newRawInput
        assertEquals("5.", raw)

        // Type second "." -> refused invalid
        val refusedDot = amountPadProcessKey(raw, ".")
        assertEquals(AmountPadKeyResult.RefusedInvalid, refusedDot)

        // Type two decimals "2", "5"
        res = amountPadProcessKey(raw, "2")
        assertTrue(res is AmountPadKeyResult.Success)
        raw = (res as AmountPadKeyResult.Success).newRawInput
        assertEquals("5.2", raw)

        res = amountPadProcessKey(raw, "5")
        assertTrue(res is AmountPadKeyResult.Success)
        raw = (res as AmountPadKeyResult.Success).newRawInput
        assertEquals("5.25", raw)

        // Third decimal -> refused invalid
        val refusedThird = amountPadProcessKey(raw, "9")
        assertEquals(AmountPadKeyResult.RefusedInvalid, refusedThird)

        // Backspace
        res = amountPadProcessKey(raw, "back")
        assertTrue(res is AmountPadKeyResult.Success)
        raw = (res as AmountPadKeyResult.Success).newRawInput
        assertEquals("5.2", raw)

        // To number
        val num = amountPadToNumber("5.25")
        assertEquals(5.25, num, 0.0001)

        // Money format
        val money = amountPadMoney(num, "₹")
        assertEquals("₹5.25", money)

        // Exceeding maxAmount caps
        val cappedRes = amountPadProcessKey("250000", "1", maxAmount = 250000.0)
        assertEquals(AmountPadKeyResult.RefusedCapped, cappedRes)
    }

    @Test
    fun testSlideToConfirmDecision() {
        // High progress commits
        assertTrue(slideShouldCommit(progress = 0.91f, velocity = 0f))
        assertTrue(slideShouldCommit(progress = 1.0f, velocity = -50f))

        // Flick over commit threshold
        assertTrue(slideShouldCommit(progress = 0.46f, velocity = 950f))

        // Below threshold does not commit
        assertFalse(slideShouldCommit(progress = 0.80f, velocity = 200f))
        assertFalse(slideShouldCommit(progress = 0.35f, velocity = 1500f))
    }
}

