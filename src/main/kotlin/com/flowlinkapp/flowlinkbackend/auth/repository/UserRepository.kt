package com.flowlinkapp.flowlinkbackend.auth.repository

import com.flowlinkapp.flowlinkbackend.auth.model.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository: MongoRepository<User, ObjectId> {
  fun findByEmail(email: String): User?
  fun existsByEmail(email: String): Boolean
}