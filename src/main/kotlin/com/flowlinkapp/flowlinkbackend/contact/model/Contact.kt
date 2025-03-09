package com.flowlinkapp.flowlinkbackend.contact.model

import kotlinx.datetime.Instant
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
class Telephone(
  var type: String,
  var number: String,
)

@Document
class Date(
  var type: String,
  var date: Long,
)

enum class SocialType(var type: String) {
  Telegram("Telegram"),
  VK("VK"),
}

@Document
class Social(
  var type: SocialType,
  var link: String,
)

@Document
class Occupation(
  var profession: String,
  var company: String,
  var jobTitle: String,
)

@Document(collection = "contacts")
class Contact(
  @Id
  var id: ObjectId,
  var updatedAtClient: Long,
  var updatedAtServer: Long,
  var deletedAt: Long,
  var profilePic: String,
  var firstName: String,
  var lastName: String,
  var middleName: String,
  var appearance: String,
  var meetContext: String,
  var city: String,
  var street: String,
  var house: String,
  var flat: String,
  var notes: String,
  var site: String,
  var ownerId: ObjectId,
  var meetPlaces: List<String>,
  var tags: List<String>,
  var emails: List<String>,
  var telephones: List<Telephone>,
  var dates: List<Date>,
  var social: List<Social>,
  var occupations: List<Occupation>,
) {
  fun updateServerTime() {
    updatedAtServer = System.currentTimeMillis()
  }
}