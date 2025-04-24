package com.flowlinkapp.flowlinkbackend.contact.model

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
class SocialNetwork(
  var type: SocialType,
  var link: String,
)

@Document
class Profession(
  var profession: String,
  var company: String,
  var jobTitle: String,
)

@Document
class PlaceOfMeeting(
  var name: String,
)

@Document(collection = "contacts")
class Contact(
  @Id
  var id: ObjectId,
  var clientEditTimestamp: Long,
  var serverEditTimestamp: Long,
  var deletionTimestamp: Long,
  var name: String,
  var surname: String?,
  var patronymic: String?,
  var photoPath: String,
  var placeOfMeeting: PlaceOfMeeting?,
  var tags: List<String>?,
  var telephones: List<Telephone>?,
  var dates: List<Date>?,
  var socialNetworkNetworks: List<SocialNetwork>?,
  var professtions: List<Profession>?,
  var emails: List<String>?,
  var appearance: String?,
  var contextOfMeeting: String?,
  var city: String?,
  var street: String?,
  var house: String?,
  var flat: String?,
  var notes: String?,
  var site: String?,
  var ownerId: ObjectId,
) {
  fun updateServerTime() {
    serverEditTimestamp = System.currentTimeMillis()
  }
}