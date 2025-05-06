package com.flowlinkapp.flowlinkbackend.contact.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
class Topic(
  var name: String,
  var description: String,
  var answer: String,
  var contactId: ObjectId?,
)

@Document(collection = "meetings")
class Meeting(
  @Id
  var id: ObjectId,
  var updatedAtClient: Long,
  var updatedAtServer: Long,
  var deletedAt: Long,
  var date: Long,
  var description: String,
  var topics: List<Topic>,
  var contactIds: List<ObjectId>,
  var ownerId: ObjectId,
) {
  fun updateServerTime() {
    this.updatedAtServer = System.currentTimeMillis()
  }
}

class MeetingDto(
  var id: String,
  var updatedAtClient: Long,
  var updatedAtServer: Long,
  var deletedAt: Long,
  var date: Long,
  var description: String,
  var topics: List<Topic>,
  var contactIds: List<String>,
  var ownerId: String,
) {
  fun updateServerTime() {
    this.updatedAtServer = System.currentTimeMillis()
  }
}

fun Meeting.toDto(): MeetingDto {
  return MeetingDto(
    id = this.id.toHexString(),
    updatedAtClient = this.updatedAtClient,
    updatedAtServer = this.updatedAtServer,
    deletedAt = this.deletedAt,
    date = this.date,
    description = this.description,
    topics = this.topics,
    contactIds = this.contactIds.map { objectId -> objectId.toHexString() },
    ownerId = this.ownerId.toHexString()
  )
}

fun MeetingDto.toModel(): Meeting {
  return Meeting(
    id = ObjectId(this.id),
    updatedAtClient = this.updatedAtClient,
    updatedAtServer = this.updatedAtServer,
    deletedAt = this.deletedAt,
    date = this.date,
    description = this.description,
    topics = this.topics,
    contactIds = this.contactIds.map { idHexString -> ObjectId(idHexString) },
    ownerId = ObjectId(this.ownerId)
  )
}
