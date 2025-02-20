package com.flowlinkapp.flowlinkbackend.auth.repository

import com.flowlinkapp.flowlinkbackend.auth.model.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository: MongoRepository<User, ObjectId> {
  fun findByPhone(phoneNumber: String): User?
  fun existsByPhone(phoneNumber: String): Boolean
}