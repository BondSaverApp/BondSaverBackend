package com.flowlinkapp.flowlinkbackend.contact.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
class Topic(
  var name: String,
  var description: String,
  var isGenerated: Boolean,
  var answer: String?,
  var contactId: ObjectId?,


) {
  override fun toString(): String {
    return "Topic(name='$name', description='$description', isGenerated=$isGenerated, answer=$answer, contactId=$contactId)"
  }
}

class TopicDto(
  var name: String,
  var description: String,
  var isGenerated: Boolean,
  var answer: String?,
  var contactId: String?,
) {
  override fun toString(): String {
    return "TopicDto(name='$name', description='$description', isGenerated=$isGenerated, answer=$answer, contactId=$contactId)"
  }
}

@Document(collection = "meetings")
class Meeting(
  @Id
  var id: ObjectId,
  var clientEditTimestamp: Long,
  var serverEditTimestamp: Long?,
  var deletionTimestamp: Long?,
  var date: Long,
  var description: String,
  var topics: MutableList<Topic>,
  var contactIds: MutableList<ObjectId>,
  var ownerId: ObjectId,
) {
  fun updateServerTime() {
    this.serverEditTimestamp = System.currentTimeMillis()
  }

  override fun toString(): String {
    return "Meeting(id=$id, clientEditTimestamp=$clientEditTimestamp, serverEditTimestamp=$serverEditTimestamp, deletionTimestamp=$deletionTimestamp, date=$date, description='$description', topics=$topics, contactIds=$contactIds, ownerId=$ownerId)"
  }


}

class MeetingDto(
  var id: String,
  var clientEditTimestamp: Long,
  var serverEditTimestamp: Long?,
  var deletionTimestamp: Long?,
  var date: Long,
  var description: String,
  var topics: List<TopicDto>,
  var contactsIds: List<String>,
  var ownerId: String,
) {
  override fun toString(): String {
    return "MeetingDto(id='$id', clientEditTimestamp=$clientEditTimestamp, serverEditTimestamp=$serverEditTimestamp, deletionTimestamp=$deletionTimestamp, date=$date, description='$description', topics=$topics, contactsIds=$contactsIds, ownerId='$ownerId')"
  }
}

fun Meeting.toDto(): MeetingDto {
  return MeetingDto(
    id = this.id.toHexString(),
    clientEditTimestamp = this.clientEditTimestamp,
    serverEditTimestamp = this.serverEditTimestamp,
    deletionTimestamp = this.deletionTimestamp,
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
  isGenerated = this@toDto.isGenerated,
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
    isGenerated = this@toModel.isGenerated,
    answer = this@toModel.answer,
    contactId = contactId,
  )
}

fun MeetingDto.toModel(): Meeting {
  return Meeting(
    id = ObjectId(this.id),
    clientEditTimestamp = this.clientEditTimestamp,
    serverEditTimestamp = this.serverEditTimestamp,
    deletionTimestamp = this.deletionTimestamp,
    date = this.date,
    description = this.description,
    topics = this.topics.map { it.toModel() }.toMutableList(),
    contactIds = this.contactsIds.map { idHexString -> ObjectId(idHexString) }.toMutableList(),
    ownerId = ObjectId(this.ownerId)
  )
}
