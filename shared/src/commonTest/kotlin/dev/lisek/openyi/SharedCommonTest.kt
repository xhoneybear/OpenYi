package dev.lisek.openyi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun testParseSessionStartResponse() {
        val json = """{"rval":0,"msg_id":257,"param":1}"""
        val response = CameraPacket.parse(json)

        assertEquals(257, response.msgId)
        assertEquals(0, response.rval)
        assertEquals("1", response.strParam)
        assertEquals(1, response.intParam)
        assertTrue(response.isSuccess)
    }

    @Test
    fun testParseGetAllSettingsResponse() {
        val json = """{"rval":0,"msg_id":3,"param":[{"camera_clock":"2026-10-25 04:00:00"},{"video_quality":"S.Fine"}]}"""
        val response = CameraPacket.parse(json)

        assertEquals(3, response.msgId)
        assertEquals(0, response.rval)
        assertEquals("""[{"camera_clock":"2026-10-25 04:00:00"},{"video_quality":"S.Fine"}]""", response.param.toString())
        assertTrue(response.isSuccess)
    }

    @Test
    fun testParseErrorResponse() {
        val json = """{"rval":-4,"msg_id":257}"""
        val response = CameraPacket.parse(json)

        assertEquals(257, response.msgId)
        assertEquals(-4, response.rval)
        assertNull(response.param)
        assertFalse(response.isSuccess)
    }

    @Test
    fun testParseNotification() {
        val json = """{"msg_id":7,"type":"battery","param":"100"}"""
        val response = CameraPacket.parse(json)

        assertEquals(7, response.msgId)
        assertEquals("battery", response.type)
        assertEquals("\"100\"", response.param.toString())
        assertEquals("100", response.strParam)
        assertEquals(100, response.intParam)
    }

    @Test
    fun testFramerSinglePacket() {
        val packets = mutableListOf<String>()
        val framer = JsonStreamFramer { packets.add(it) }

        val json = """{"msg_id":257,"token":0}"""
        framer.feed(json.encodeToByteArray())

        assertEquals(1, packets.size)
        assertEquals(json, packets[0])
    }

    @Test
    fun testFramerMultiplePacketsInOneChunk() {
        val packets = mutableListOf<String>()
        val framer = JsonStreamFramer { packets.add(it) }

        val chunk = """{"rval":0,"msg_id":257,"param":1}{"rval":0,"msg_id":3,"param":[]}"""
        framer.feed(chunk.encodeToByteArray())

        assertEquals(2, packets.size)
        assertEquals("""{"rval":0,"msg_id":257,"param":1}""", packets[0])
        assertEquals("""{"rval":0,"msg_id":3,"param":[]}""", packets[1])
    }

    @Test
    fun testFramerFragmentedPacketAcrossChunks() {
        val packets = mutableListOf<String>()
        val framer = JsonStreamFramer { packets.add(it) }

        val part1 = """{"rval":0,"msg_id":3,"param":"""
        val part2 = """[{"video_resolution":"1920x1080 60P 16:9"}]}"""

        framer.feed(part1.encodeToByteArray())
        assertEquals(0, packets.size)

        framer.feed(part2.encodeToByteArray())
        assertEquals(1, packets.size)
        assertEquals(part1 + part2, packets[0])
    }

    @Test
    fun testFramerWithEscapedQuotesAndBraces() {
        val packets = mutableListOf<String>()
        val framer = JsonStreamFramer { packets.add(it) }

        val json = """{"msg_id":1,"param":"hello {world} \"escaped\""}"""
        framer.feed(json.encodeToByteArray())

        assertEquals(1, packets.size)
        assertEquals(json, packets[0])
    }

    @Test
    fun testCameraSignalClassification() {
        // Test battery signal
        val batteryResp = CameraPacket.parse("""{"msg_id":7,"type":"battery","param":"85"}""")
        val batterySignal = CameraSignal.from(batteryResp)
        assertTrue(batterySignal is CameraSignal.Battery)
        assertEquals(85, batterySignal.level)
        assertFalse(batterySignal.isCharging)

        // Test adapter signal
        val adapterResp = CameraPacket.parse("""{"msg_id":7,"type":"adapter","param":"100"}""")
        val adapterSignal = CameraSignal.from(adapterResp)
        assertTrue(adapterSignal is CameraSignal.Battery)
        assertEquals(100, adapterSignal.level)
        assertTrue(adapterSignal.isCharging)

        // Test adapter status signal
        val adapterStatusResp = CameraPacket.parse("""{"msg_id":7,"type":"adapter_status","param":"1"}""")
        val adapterStatusSignal = CameraSignal.from(adapterStatusResp)
        assertTrue(adapterStatusSignal is CameraSignal.AdapterStatus)
        assertTrue(adapterStatusSignal.isCharging)

        // Test photo_taken signal
        val photoResp = CameraPacket.parse("""{"msg_id":7,"type":"photo_taken","param":"/tmp/fuse_d/DCIM/100MEDIA/YDXJ0001.jpg"}""")
        val photoSignal = CameraSignal.from(photoResp)
        assertTrue(photoSignal is CameraSignal.PhotoTaken)
        assertEquals("/tmp/fuse_d/DCIM/100MEDIA/YDXJ0001.jpg", photoSignal.path)

        // Test video recording signals
        val startRecordResp = CameraPacket.parse("""{"msg_id":7,"type":"start_video_record"}""")
        val startRecordSignal = CameraSignal.from(startRecordResp)
        assertTrue(startRecordSignal is CameraSignal.VideoRecord)
        assertTrue(startRecordSignal.isRecording)

        val stopRecordResp = CameraPacket.parse("""{"msg_id":7,"type":"video_record_complete"}""")
        val stopRecordSignal = CameraSignal.from(stopRecordResp)
        assertTrue(stopRecordSignal is CameraSignal.VideoRecord)
        assertFalse(stopRecordSignal.isRecording)

        // Test custom signal
        val customResp = CameraPacket.parse("""{"type":"custom_mode","param":"timelapse"}""")
        val customSignal = CameraSignal.from(customResp)
        assertTrue(customSignal is CameraSignal.Custom)
        assertEquals("custom_mode", customSignal.type)
        assertEquals("timelapse", customSignal.param)
    }

    @Test
    fun testComputeSubnetCandidates() {
        val candidates = CameraDiscovery.computeSubnetCandidates("192.168.1.50")
        assertEquals(253, candidates.size)
        assertTrue(candidates.contains("192.168.1.1"))
        assertTrue(candidates.contains("192.168.1.254"))
        assertFalse(candidates.contains("192.168.1.50"))

        val invalid = CameraDiscovery.computeSubnetCandidates("invalid_ip")
        assertTrue(invalid.isEmpty())
    }

    @Test
    fun testBatteryInfoFormatting() {
        val unknown = BatteryInfo()
        assertEquals(-1, unknown.level)
        assertFalse(unknown.isCharging)
        assertEquals("N/A", unknown.displayText)

        val charged = BatteryInfo(level = 85, isCharging = true)
        assertEquals(85, charged.level)
        assertTrue(charged.isCharging)
        assertEquals("85%", charged.displayText)
    }

    @Test
    fun testBatteryLevelMapping() {
        assertEquals(BatteryLevel.B_100, BatteryLevel.fromLevel(100))
        assertEquals(BatteryLevel.B_90, BatteryLevel.fromLevel(95))
        assertEquals(BatteryLevel.B_80, BatteryLevel.fromLevel(85))
        assertEquals(BatteryLevel.B_60, BatteryLevel.fromLevel(70))
        assertEquals(BatteryLevel.B_50, BatteryLevel.fromLevel(55))
        assertEquals(BatteryLevel.B_30, BatteryLevel.fromLevel(40))
        assertEquals(BatteryLevel.B_20, BatteryLevel.fromLevel(25))
        assertEquals(BatteryLevel.B_0, BatteryLevel.fromLevel(10))
        assertEquals(BatteryLevel.UNKNOWN, BatteryLevel.fromLevel(-1))
    }

    @Test
    fun testTimerDurationCycle() {
        assertEquals(TimerDuration.S_3, TimerDuration.OFF.next())
        assertEquals(TimerDuration.S_5, TimerDuration.S_3.next())
        assertEquals(TimerDuration.S_10, TimerDuration.S_5.next())
        assertEquals(TimerDuration.OFF, TimerDuration.S_10.next())
    }

    @Test
    fun testRtspPlayerStateErrorMapping() {
        assertEquals("Unable to decode RTSP stream!", RtspPlayerState.Error.errorMessage())
        assertEquals("Connection lost. Dismiss to retry.", RtspPlayerState.Idle.errorMessage())
        assertNull(RtspPlayerState.Playing.errorMessage())
        assertNull(RtspPlayerState.Buffering.errorMessage())
        assertNull(RtspPlayerState.Disconnected.errorMessage())
    }

    @Test
    fun testCameraConnectionStateProperties() {
        val disconnected = CameraConnectionState.Disconnected
        assertFalse(disconnected.isConnected)
        assertFalse(disconnected.isConnecting)
        assertNull(disconnected.errorMessage)

        val connecting = CameraConnectionState.Connecting("192.168.42.1")
        assertFalse(connecting.isConnected)
        assertTrue(connecting.isConnecting)
        assertEquals("192.168.42.1", connecting.host)

        val connected = CameraConnectionState.Connected("192.168.42.1", 1)
        assertTrue(connected.isConnected)
        assertFalse(connected.isConnecting)
        assertEquals("192.168.42.1", connected.host)
        assertEquals(1, connected.token)

        val error = CameraConnectionState.Error("Connection failed")
        assertFalse(error.isConnected)
        assertFalse(error.isConnecting)
        assertEquals("Connection failed", error.errorMessage)
    }
}
