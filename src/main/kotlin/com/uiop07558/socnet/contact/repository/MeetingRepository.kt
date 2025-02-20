package com.uiop07558.socnet.contact.repository

import com.uiop07558.socnet.model.Meeting
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface MeetingRepository: MongoRepository<Meeting, ObjectId> {
}