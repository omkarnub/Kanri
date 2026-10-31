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

            // 3. SVG Path assertion
            val expectedPaths = item.getJSONObject("paths")
            val cx = 50.0
            val cy = 50.0

            val headW = s.headW.toExactDouble()
            val headH = s.headH.toExactDouble()
            val hairVolume = s.hairVolume.toExactDouble()
            val hairLength = s.hairLength.toExactDouble()
            val headSquare = s.headSquare.toExactDouble()
            val chin = s.chin.toExactDouble()

            val generatedLongHair = avatarBlobPath(
                cx = cx,
                cy = cy - headH * 0.15,
                w = headW + 1.0 + hairVolume * 2.5,
                top = headH * 0.85 + hairVolume * 4.0,
                bottom = headH * 0.15 + hairLength * (headH + 14.0),
                square = headSquare * 0.6
            )
            assertEquals(expectedPaths.getString("longHair"), generatedLongHair)

            val generatedHeadBlob = avatarBlobPath(
                cx = cx,
                cy = cy,
                w = headW,
                top = headH,
                bottom = headH,
                square = headSquare,
                chin = chin
            )
            assertEquals(expectedPaths.getString("headBlob"), generatedHeadBlob)

            val generatedFrontHair = avatarHairFrontPath(s, 50f, 50f)
            assertEquals(expectedPaths.getString("frontHair"), generatedFrontHair)

            val generatedMouth = avatarMouthPath(s, 50.0, 50.0 + headH * 0.5)
            assertEquals(expectedPaths.getString("mouth"), generatedMouth)
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
        var idCounter = 1
        val make: (String) -> AmountChar = { ch -> AmountChar(idCounter++, ch) }

        // Initial empty + "5"
        var chars = amountPadTypeKey(emptyList(), "5", make)!!
        assertEquals(1, chars.size)
        assertEquals("5", chars[0].char)

        // Type "."
        chars = amountPadTypeKey(chars, ".", make)!!
        assertEquals(2, chars.size)
        assertEquals(".", chars[1].char)

        // Type second "." -> refused
        val refusedDot = amountPadTypeKey(chars, ".", make)
        assertNull(refusedDot)

        // Type two decimals "2", "5"
        chars = amountPadTypeKey(chars, "2", make)!!
        chars = amountPadTypeKey(chars, "5", make)!!
        assertEquals(4, chars.size)

        // Third decimal -> refused
        val refusedThird = amountPadTypeKey(chars, "9", make)
        assertNull(refusedThird)

        // To number
        val num = amountPadToNumber(chars)
        assertEquals(5.25, num, 0.0001)

        // Money format
        val money = amountPadMoney(num)
        assertEquals("₹5.25", money)

        // Whole digits grouping
        var wholeChars = emptyList<AmountChar>()
        "1000000".forEach { wholeChars = amountPadTypeKey(wholeChars, it.toString(), make)!! }
        val layout = amountPadLayout(wholeChars)
        val fullStr = layout.joinToString("") { it.char }
        assertEquals("1,000,000", fullStr)
    }

    @Test
    fun testSlideToConfirmDecision() {
        // High progress commits
        assertTrue(slideShouldCommit(progress = 0.91f, velocity = 0f))
        assertTrue(slideShouldCommit(progress = 1.0f, velocity = -50f))

        // Flick over commit threshold
        assertTrue(slideShouldCommit(progress = 0.46f, velocity = 950f))

        // Below threshold does not commit
        assertFalse(slideShouldCommit(progress = 0.85f, velocity = 200f))
        assertFalse(slideShouldCommit(progress = 0.40f, velocity = 1500f))
    }
}

