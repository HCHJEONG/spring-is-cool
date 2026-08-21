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
        assertEquals(WorldCommand.Log(), parser.parse("remember"))
        assertEquals(WorldCommand.Log(100), parser.parse("log for 100"))
        assertEquals(WorldCommand.Log(25), parser.parse("history 25"))
        assertEquals(WorldCommand.Status, parser.parse("stat"))
        assertEquals(WorldCommand.Ai("what is listening?"), parser.parse("ai what is listening?"))
        assertEquals(WorldCommand.Assign("clerk", "check line"), parser.parse("assign clerk check line"))
        assertEquals(WorldCommand.Assign("ai-clerk", "check line"), parser.parse("assign AI clerk check line"))
        assertEquals(WorldCommand.Assign("clerk", "file signal evidence"), parser.parse("assign clerk file signal evidence"))
        assertEquals(WorldCommand.Assign("cleark", "describe office"), parser.parse("assign cleark describe office"))
    }

    @Test
    fun `preserves unknown command text`() {
        assertEquals(WorldCommand.Unknown("open drawer"), parser.parse(" open drawer "))
    }
}
