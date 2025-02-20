package com.uiop07558.socnet.auth.repository

import com.uiop07558.socnet.auth.model.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository: MongoRepository<User, ObjectId> {
  fun findByPhone(phoneNumber: String): User?
  fun existsByPhone(phoneNumber: String): Boolean
}