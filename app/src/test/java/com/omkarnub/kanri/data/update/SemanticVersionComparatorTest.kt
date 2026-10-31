package com.omkarnub.kanri.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticVersionComparatorTest {

    @Test
    fun testStandardSemanticVersionComparison() {
        assertTrue(SemanticVersionComparator.isNewer("1.0.2", "1.0.3"))
        assertTrue(SemanticVersionComparator.isNewer("1.0.2", "1.1.0"))
        assertTrue(SemanticVersionComparator.isNewer("1.0.2", "2.0.0"))
        assertFalse(SemanticVersionComparator.isNewer("1.0.2", "1.0.2"))
        assertFalse(SemanticVersionComparator.isNewer("1.0.2", "1.0.1"))
        assertFalse(SemanticVersionComparator.isNewer("1.0.2", "0.9.9"))
    }

    @Test
    fun testWithVPrefix() {
        assertTrue(SemanticVersionComparator.isNewer("1.0.2", "v1.0.3"))
        assertTrue(SemanticVersionComparator.isNewer("v1.0.2", "v1.0.3"))
        assertFalse(SemanticVersionComparator.isNewer("v1.0.2", "v1.0.2"))
        assertFalse(SemanticVersionComparator.isNewer("v1.0.3", "v1.0.2"))
    }

    @Test
    fun testUnequalSegments() {
        assertTrue(SemanticVersionComparator.isNewer("1.0", "1.0.1"))
        assertTrue(SemanticVersionComparator.isNewer("1.0.2", "1.0.2.1"))
        assertFalse(SemanticVersionComparator.isNewer("1.0.2.1", "1.0.2"))
        assertFalse(SemanticVersionComparator.isNewer("1.0.0", "1.0"))
    }

    @Test
    fun testWithPrereleaseSuffix() {
        assertTrue(SemanticVersionComparator.isNewer("1.0.2", "1.0.3-beta"))
        assertTrue(SemanticVersionComparator.isNewer("1.0.2", "v1.0.3-rc1"))
        assertFalse(SemanticVersionComparator.isNewer("1.0.2", "1.0.2-patch"))
    }

    @Test
    fun testCleanVersion() {
        assertEquals("1.0.2", SemanticVersionComparator.cleanVersion("v1.0.2"))
        assertEquals("1.0.2", SemanticVersionComparator.cleanVersion("V1.0.2"))
        assertEquals("1.0.3", SemanticVersionComparator.cleanVersion("v1.0.3-beta1"))
    }
}
