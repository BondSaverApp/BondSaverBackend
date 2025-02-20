package com.uiop07558.socnet.contact.repository

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface ContactRepository: MongoRepository<ContactRepository, ObjectId> {
}