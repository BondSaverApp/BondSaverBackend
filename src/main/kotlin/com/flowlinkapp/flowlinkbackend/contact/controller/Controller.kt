package com.flowlinkapp.flowlinkbackend.contact.controller

import com.flowlinkapp.flowlinkbackend.contact.model.Meeting
import com.flowlinkapp.flowlinkbackend.contact.model.MeetingDto
import com.flowlinkapp.flowlinkbackend.contact.service.ContactService
import com.flowlinkapp.flowlinkbackend.contact.service.SynchronizeInput
import com.flowlinkapp.flowlinkbackend.contact.service.SynchronizeOutput
import com.flowlinkapp.flowlinkbackend.contact.service.UploadedObjects
import com.flowlinkapp.flowlinkbackend.exceptions.BadRequestServerException
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

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
  fun generateTopics(@PathVariable("id") meetingId: String): ResponseEntity<MeetingDto> {
    val userId = ObjectId(SecurityContextHolder.getContext().authentication.name)

    val meeting = service.generateTopics(ObjectId(meetingId), userId)
    return ResponseEntity.ok(meeting)
  }

  @PostMapping("/upload")
  fun uploadMultipleFiles(@RequestParam("files") files: List<MultipartFile>): ResponseEntity<UploadedObjects> {
    if (files.isEmpty()) {
      throw BadRequestServerException("No files uploaded")
    }

    val uploaded = service.uploadFiles(files)
    return ResponseEntity.ok(uploaded)
  }
}