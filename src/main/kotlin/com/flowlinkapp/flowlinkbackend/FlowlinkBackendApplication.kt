package com.flowlinkapp.flowlinkbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
class FlowlinkBackendApplication

fun main(args: Array<String>) {
	runApplication<FlowlinkBackendApplication>(*args)
}
