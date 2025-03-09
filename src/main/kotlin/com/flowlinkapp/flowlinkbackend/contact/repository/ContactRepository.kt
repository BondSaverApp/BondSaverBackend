package com.flowlinkapp.flowlinkbackend.contact.repository

import com.flowlinkapp.flowlinkbackend.contact.model.Contact
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface ContactRepository: MongoRepository<Contact, ObjectId> {
  fun findByOwnerId(ownerId: ObjectId): List<Contact>
}