package com.example.aira.commands

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommandRegistryTest {

    private lateinit var registry: CommandRegistry

    @Before
    fun setUp() {
        registry = CommandRegistry()
    }

    @Test
    fun testFlashlightCommands() {
        val onResult = registry.parse("turn on flashlight")
        assertTrue(onResult is CommandIntent.SetFlashlight && onResult.enable)

        val torchOn = registry.parse("torch on")
        assertTrue(torchOn is CommandIntent.SetFlashlight && torchOn.enable)

        val offResult = registry.parse("turn off flashlight")
        assertTrue(offResult is CommandIntent.SetFlashlight && !offResult.enable)

        val torchOff = registry.parse("torch off")
        assertTrue(torchOff is CommandIntent.SetFlashlight && !torchOff.enable)
    }

    @Test
    fun testBatteryCommands() {
        val battery1 = registry.parse("what is the battery level")
        assertTrue(battery1 is CommandIntent.CheckBattery)

        val battery2 = registry.parse("check battery")
        assertTrue(battery2 is CommandIntent.CheckBattery)

        val battery3 = registry.parse("battery percentage")
        assertTrue(battery3 is CommandIntent.CheckBattery)
    }

    @Test
    fun testAppLaunchCommands() {
        val yt = registry.parse("open youtube")
        assertTrue(yt is CommandIntent.OpenApp && yt.targetAppName == "youtube")

        val wa = registry.parse("open whatsapp")
        assertTrue(wa is CommandIntent.OpenApp && wa.targetAppName == "whatsapp")

        val chrome = registry.parse("launch chrome")
        assertTrue(chrome is CommandIntent.OpenApp && chrome.targetAppName == "chrome")

        val camera = registry.parse("start camera")
        assertTrue(camera is CommandIntent.OpenApp && camera.targetAppName == "camera")
    }

    @Test
    fun testSettingsCommands() {
        val wifi = registry.parse("open wifi settings")
        assertTrue(wifi is CommandIntent.OpenWifiSettings)

        val bt = registry.parse("open bluetooth")
        assertTrue(bt is CommandIntent.OpenBluetoothSettings)

        val cellular = registry.parse("open mobile network")
        assertTrue(cellular is CommandIntent.OpenMobileNetworkSettings)

        val loc = registry.parse("open location settings")
        assertTrue(loc is CommandIntent.OpenLocationSettings)
    }

    @Test
    fun testVolumeCommands() {
        val volUp = registry.parse("volume up")
        assertTrue(volUp is CommandIntent.ChangeVolume && volUp.action == CommandIntent.VolumeAction.UP)

        val volDown = registry.parse("decrease volume")
        assertTrue(volDown is CommandIntent.ChangeVolume && volDown.action == CommandIntent.VolumeAction.DOWN)

        val mute = registry.parse("mute media")
        assertTrue(mute is CommandIntent.ChangeVolume && mute.action == CommandIntent.VolumeAction.MUTE)
    }

    @Test
    fun testTimeAndDateCommands() {
        val time = registry.parse("what time is it")
        assertTrue(time is CommandIntent.TellTime)

        val date = registry.parse("today's date")
        assertTrue(date is CommandIntent.TellDate)
    }

    @Test
    fun testStopListeningCommand() {
        val stop = registry.parse("stop listening")
        assertTrue(stop is CommandIntent.StopListening)

        val sleep = registry.parse("go to sleep")
        assertTrue(sleep is CommandIntent.StopListening)
    }

    @Test
    fun testWebsiteCommand() {
        val web1 = registry.parse("open website google.com")
        assertTrue(web1 is CommandIntent.OpenWebsite && web1.url == "google.com")

        val web2 = registry.parse("open wikipedia.org")
        assertTrue(web2 is CommandIntent.OpenWebsite && web2.url == "wikipedia.org")
    }

    @Test
    fun testDialerAndCallCommands() {
        val dialer = registry.parse("open dialer")
        assertTrue(dialer is CommandIntent.OpenDialer && dialer.phoneNumber == null)

        val phone = registry.parse("open phone")
        assertTrue(phone is CommandIntent.OpenDialer && phone.phoneNumber == null)

        val dialNumber = registry.parse("dial 9876543210")
        assertTrue(dialNumber is CommandIntent.OpenDialer && dialNumber.phoneNumber == "9876543210")

        val callNumber = registry.parse("call +1 234 567 8900")
        assertTrue(callNumber is CommandIntent.OpenDialer && callNumber.phoneNumber == "+12345678900")

        val callMom = registry.parse("call mom")
        assertTrue(callMom is CommandIntent.CallContact && callMom.contactName == "mom")

        val callDad = registry.parse("call to dad")
        assertTrue(callDad is CommandIntent.CallContact && callDad.contactName == "dad")

        val dialAlice = registry.parse("dial alice")
        assertTrue(dialAlice is CommandIntent.CallContact && dialAlice.contactName == "alice")

        val makeCall = registry.parse("make a call to john doe")
        assertTrue(makeCall is CommandIntent.CallContact && makeCall.contactName == "john doe")

        val phoneSister = registry.parse("phone sister")
        assertTrue(phoneSister is CommandIntent.CallContact && phoneSister.contactName == "sister")
    }

    @Test
    fun testUnknownCommand() {
        val unknown = registry.parse("bake me a cake please")
        assertTrue(unknown is CommandIntent.Unknown)
        assertEquals("bake me a cake please", (unknown as CommandIntent.Unknown).rawQuery)
    }
}
