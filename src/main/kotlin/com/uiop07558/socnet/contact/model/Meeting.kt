package com.uiop07558.socnet.contact.model

import kotlinx.datetime.Instant
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
class Topic(
  private val name: String,
  private val description: String,
)

@Document(collection = "meetings")
class Meeting(
  @Id
  private val id: ObjectId,
  private val date: Instant,
  private val description: String,
  private val topics: List<Topic>,
  private val contactId: ObjectId,
)