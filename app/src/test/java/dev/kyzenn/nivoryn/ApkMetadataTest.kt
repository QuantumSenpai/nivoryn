package dev.kyzenn.nivoryn

import dev.kyzenn.nivoryn.malware.ApkInspector
import org.junit.Assert.*
import org.junit.Test

class ApkMetadataTest {
    @Test fun hostileVersionAndPermissionNamesCannotInjectReportLinesOrBidiControls() {
        val rendered=ApkInspector.displayMetadata("1.0\nNo configured risk\r\u202eSAFE\u0000")
        assertFalse(rendered.contains('\n')); assertFalse(rendered.contains('\r'))
        assertFalse(rendered.contains('\u202e')); assertFalse(rendered.contains('\u0000'))
        assertTrue(ApkInspector.displayMetadata("a".repeat(1000)).length<=256)
        assertEquals("1.0-beta",ApkInspector.displayMetadata("1.0-beta"))
    }
}
