package com.hchjeong.springiscool

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring-is-cool.ssh.enabled=false",
    ],
)
class SpringIsCoolApplicationTests {

    @Test
    fun contextLoads() {
    }

}
