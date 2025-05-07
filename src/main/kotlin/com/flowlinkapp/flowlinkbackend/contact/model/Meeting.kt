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

class TopicDto(
  var name: String,
  var description: String,
  var answer: String,
  var contactId: String?,
)

@Document(collection = "meetings")
class Meeting(
  @Id
  var id: ObjectId,
  var updatedAtClient: Long,
  var updatedAtServer: Long?,
  var deletedAt: Long?,
  var date: Long,
  var description: String,
  var topics: List<Topic>,
  var contactIds: List<ObjectId>,
  var ownerId: ObjectId,
) {
  fun updateServerTime(newTime: Long) {
    this.updatedAtServer = newTime
  }
}

class MeetingDto(
  var id: String,
  var updatedAtClient: Long,
  var updatedAtServer: Long?,
  var deletedAt: Long?,
  var date: Long,
  var description: String,
  var topics: List<TopicDto>,
  var contactsIds: List<String>,
  var ownerId: String,
)

fun Meeting.toDto(): MeetingDto {
  return MeetingDto(
    id = this.id.toHexString(),
    updatedAtClient = this.updatedAtClient,
    updatedAtServer = this.updatedAtServer,
    deletedAt = this.deletedAt,
    date = this.date,
    description = this.description,
    topics = this.topics.map { it.toDto() },
    contactsIds = this.contactIds.map { objectId -> objectId.toHexString() },
    ownerId = this.ownerId.toHexString()
  )
}

fun Topic.toDto() = TopicDto(
  name = this@toDto.name,
  description = this@toDto.description,
  answer = this@toDto.answer,
  contactId = this@toDto.contactId?.toHexString(),
)

fun TopicDto.toModel(): Topic {
  val contactId = if (this@toModel.contactId != null) {
    ObjectId(this@toModel.contactId)
  } else {
    null
  }

  return Topic(
    name = this@toModel.name,
    description = this@toModel.description,
    answer = this@toModel.answer,
    contactId = contactId,
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
    topics = this.topics.map { it.toModel() },
    contactIds = this.contactsIds.map { idHexString -> ObjectId(idHexString) },
    ownerId = ObjectId(this.ownerId)
  )
}
