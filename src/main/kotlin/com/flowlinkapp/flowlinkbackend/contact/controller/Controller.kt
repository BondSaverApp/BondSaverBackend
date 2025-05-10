package com.flowlinkapp.flowlinkbackend.contact.controller

import com.flowlinkapp.flowlinkbackend.contact.model.Meeting
import com.flowlinkapp.flowlinkbackend.contact.service.ContactService
import com.flowlinkapp.flowlinkbackend.contact.service.SynchronizeInput
import com.flowlinkapp.flowlinkbackend.contact.service.SynchronizeOutput
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/contact")
class ContactController(
  val service: ContactService
) {
  @PostMapping("/synchronize")
  fun synchronize(@RequestBody input: SynchronizeInput): ResponseEntity<SynchronizeOutput> {
    val userId = ObjectId(SecurityContextHolder.getContext().authentication.name)

    val synchronizeOutput = service.synchronize(input, userId)
    return ResponseEntity.ok(synchronizeOutput)
  }

  @PostMapping("/generate-topics/{id}")
  fun generateTopics(@PathVariable("id") meetingId: String): ResponseEntity<Meeting> {
    val userId = ObjectId(SecurityContextHolder.getContext().authentication.name)

    val meeting = service.generateTopics(ObjectId(meetingId), userId)
    return ResponseEntity.ok(meeting)
  }
}