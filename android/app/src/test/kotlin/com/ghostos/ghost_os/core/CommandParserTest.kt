package com.ghostos.ghost_os.core

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CommandParserTest {
    @Test fun naturalAlarmVariantsAndSpeechPunctuation() {
        listOf("set an alaram for 6pm", "can you set an alarm for six p. m.",
            "set alarm at 6 PM", "please create an alarm for six in the evening",
            "hey ghost set an alarm for six pm").forEach {
            assertEquals(it, GhostCommand.Alarm(18, 0), CommandParser.parse(it))
        }
        assertEquals(GhostCommand.Alarm(18, 30), CommandParser.parse("set an alarm for six thirty p m"))
    }
    @Test fun friendlyCommandsAndClarification() {
        assertEquals(GhostCommand.AlarmTimeNeeded, CommandParser.parse("set an alarm"))
        assertEquals(GhostCommand.OpenApp("YouTube"), CommandParser.parse("could you open the YouTube app for me"))
        assertTrue(CommandParser.parse("thank you") is GhostCommand.Chat)
        val missing = CommandParser.parse("do something unknown") as GhostCommand.Unknown
        assertTrue(missing.reason.contains("do something unknown"))
    }
    @Test fun naturalMessagePreservesPayload() {
        assertEquals(GhostCommand.Message("Harsh", "Hi!"), CommandParser.parse("send a message to Harsh on WhatsApp saying Hi!"))
        assertEquals(GhostCommand.Message("Harsh", "Hi!"), CommandParser.parse("send Hi! to Harsh on WhatsApp"))
    }
    @Test fun unlockAndScanCommands() {
        assertEquals(GhostCommand.Unlock, CommandParser.parse("hey ghost unlock my phone"))
        assertEquals(GhostCommand.ScanApps, CommandParser.parse("scan my apps"))
        assertEquals(GhostCommand.ScanApps, CommandParser.parse("list apps"))
    }
    @Test fun openApp() {
        assertEquals(GhostCommand.OpenApp("WhatsApp"), CommandParser.parse("Hey Ghost, please open WhatsApp"))
    }
    @Test fun preserveMessageContents() {
        assertEquals(GhostCommand.Message("Harsh", "Hi! Meet at 6 PM?"),
            CommandParser.parse("message Harsh on WhatsApp with Hi! Meet at 6 PM?"))
    }
    @Test fun neverInventMessageOrRecipient() {
        assertTrue(CommandParser.parse("message Harsh on whatsapp") is GhostCommand.Unknown)
        assertTrue(CommandParser.parse("message on whatsapp with hi") is GhostCommand.Unknown)
    }
    @Test fun ambiguousAndInvalidTimes() {
        assertTrue(CommandParser.parse("set an alarm for 6") is GhostCommand.Unknown)
        assertTrue(CommandParser.parse("set an alarm for 25:00") is GhostCommand.Unknown)
        assertTrue(CommandParser.parse("set an alarm for 6:75 am") is GhostCommand.Unknown)
        assertTrue(CommandParser.parse("set an alarm for 13 pm") is GhostCommand.Unknown)
        assertTrue(CommandParser.parse("set an alarm for 6 am tomorrow") is GhostCommand.Unknown)
    }
    @Test fun clockConversionsAndSpokenNumbers() {
        assertEquals(GhostCommand.Alarm(0, 0), CommandParser.parse("set an alarm for 12 am"))
        assertEquals(GhostCommand.Alarm(12, 0), CommandParser.parse("set an alarm for 12 pm"))
        assertEquals(GhostCommand.Alarm(18, 30), CommandParser.parse("set an alarm for 18:30"))
        assertEquals(GhostCommand.Alarm(6, 0), CommandParser.parse("set an alarm for six a m"))
        assertEquals(GhostCommand.Alarm(18, 35), CommandParser.parse("set an alarm for six thirty five pm"))
    }
    @Test fun cancellationAndUnknownRequests() {
        assertEquals(GhostCommand.Cancel, CommandParser.parse("stop listening"))
        assertTrue(CommandParser.parse("buy me a phone") is GhostCommand.Unknown)
    }
    @Test fun reminderParsesDateTimeAndRollsTimeWithoutDate() {
        val now = ZonedDateTime.of(2026, 10, 9, 10, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        val dated = ReminderParser.parse("remind me to call Harsh at 6 pm on 15 October 2026", now)!!
        assertEquals("call Harsh", dated.text)
        assertEquals(ZonedDateTime.of(2026, 10, 15, 18, 0, 0, 0, now.zone).toInstant().toEpochMilli(), dated.triggerAt)
        val rolled = ReminderParser.parse("remind me about medicine at 8 am", now)!!
        assertEquals(ZonedDateTime.of(2026, 10, 10, 8, 0, 0, 0, now.zone).toInstant().toEpochMilli(), rolled.triggerAt)
    }
    @Test fun alarmDismissalCommands() {
        assertEquals(GhostCommand.DismissAlarm, CommandParser.parse("turn off the alarm"))
        assertEquals(GhostCommand.DismissAlarm, CommandParser.parse("dismiss alarm"))
    }
    @Test fun reminderQueriesAndDeletionCommands() {
        val today = CommandParser.parse("what reminders are pending for today?") as GhostCommand.ListReminders
        assertNotNull(today.from); assertNotNull(today.until)
        assertTrue(today.until!! > today.from!!)
        val all = CommandParser.parse("list my reminders") as GhostCommand.ListReminders
        assertNull(all.from); assertNull(all.until)
        assertEquals(GhostCommand.DeleteReminder("call Harsh"), CommandParser.parse("delete reminder to call Harsh"))
        assertEquals(GhostCommand.DeleteReminder("6 pm"), CommandParser.parse("remove my reminder at 6 pm"))
    }
    @Test fun mediaCallsAndNotificationReplies() {
        assertEquals(GhostCommand.Media("pause"), CommandParser.parse("pause music"))
        assertEquals(GhostCommand.CallControl("answer"), CommandParser.parse("answer the call"))
        assertEquals(GhostCommand.Call("Harsh Kumar"), CommandParser.parse("call Harsh Kumar"))
        assertEquals(GhostCommand.NotificationReply("Harsh", "I will call you later"),
            CommandParser.parse("reply to Harsh with I will call you later"))
        assertEquals(GhostCommand.NotificationReply("", "sounds good"), CommandParser.parse("reply saying sounds good"))
    }
    @Test fun routinesAndDrivingMode() {
        assertEquals(GhostCommand.DrivingMode(true), CommandParser.parse("start driving mode"))
        assertEquals(GhostCommand.DrivingMode(false), CommandParser.parse("turn off driving mode"))
        assertEquals(GhostCommand.RunRoutine("commute"), CommandParser.parse("run my commute routine"))
        assertEquals(GhostCommand.DeleteRoutine("commute"), CommandParser.parse("delete commute routine"))
        assertEquals(GhostCommand.ListRoutines, CommandParser.parse("what are my routines"))
        assertEquals(
            GhostCommand.CreateRoutine("commute", listOf("start driving mode", "play music", "open Maps")),
            CommandParser.parse("create a routine called commute with start driving mode then play music and then open Maps")
        )
        assertTrue(RoutineStore.safeStep(GhostCommand.Media("play")))
        assertFalse(RoutineStore.safeStep(GhostCommand.Call("Harsh")))
        assertEquals(GhostCommand.TaskControl("retry"), CommandParser.parse("retry step"))
        assertEquals(GhostCommand.TaskControl("skip"), CommandParser.parse("skip that"))
        assertEquals(GhostCommand.TaskControl("continue"), CommandParser.parse("continue routine"))
    }
}
