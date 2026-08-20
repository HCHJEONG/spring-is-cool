package com.hchjeong.springiscool.world

import kotlin.test.Test
import kotlin.test.assertEquals

class CommandParserTests {
    private val parser = CommandParser()

    @Test
    fun `parses command aliases`() {
        assertEquals(WorldCommand.Look, parser.parse("l"))
        assertEquals(WorldCommand.Help, parser.parse("?"))
        assertEquals(WorldCommand.Quit, parser.parse("exit"))
        assertEquals(WorldCommand.Log, parser.parse("remember"))
        assertEquals(WorldCommand.Status, parser.parse("stat"))
        assertEquals(WorldCommand.Ai("what is listening?"), parser.parse("ai what is listening?"))
        assertEquals(WorldCommand.Assign("clerk", "check line"), parser.parse("assign clerk check line"))
        assertEquals(WorldCommand.Assign("ai-clerk", "check line"), parser.parse("assign AI clerk check line"))
    }

    @Test
    fun `preserves unknown command text`() {
        assertEquals(WorldCommand.Unknown("open drawer"), parser.parse(" open drawer "))
    }
}
