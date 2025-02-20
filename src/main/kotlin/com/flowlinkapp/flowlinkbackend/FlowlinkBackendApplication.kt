package com.flowlinkapp.flowlinkbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FlowlinkBackendApplication

fun main(args: Array<String>) {
	runApplication<FlowlinkBackendApplication>(*args)
}
