package com.flowlinkapp.flowlinkbackend.contact.repository

import com.flowlinkapp.flowlinkbackend.contact.model.Meeting
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface MeetingRepository: MongoRepository<Meeting, ObjectId> {
  fun findByOwnerId(ownerId: ObjectId): List<Meeting>
}