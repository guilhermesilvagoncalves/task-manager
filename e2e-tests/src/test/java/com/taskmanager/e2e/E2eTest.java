package com.taskmanager.e2e;

import io.karatelabs.junit6.Karate;

class E2eTest {

    @Karate.Test
    Karate all() {
        return Karate.run("classpath:auth", "classpath:tasks");
    }
}
