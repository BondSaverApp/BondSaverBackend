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
  var topics: MutableList<Topic>,
  var contactIds: MutableList<ObjectId>,
  var ownerId: ObjectId,
) {
  fun updateServerTime() {
    this.updatedAtServer = System.currentTimeMillis()
  }
}