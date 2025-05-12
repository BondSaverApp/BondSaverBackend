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
  var name: String?,
)

@Document
class Email(
  var email: String? = null
)

@Document(collection = "contacts")
class Contact(
  @Id
  var id: ObjectId,
  var clientEditTimestamp: Long,
  var serverEditTimestamp: Long?,
  var deletionTimestamp: Long?,
  var name: String,
  var surname: String?,
  var patronymic: String?,
  var photoPath: String,
  var placeOfMeeting: PlaceOfMeeting?,
  var tags: List<String>?,
  var telephones: List<Telephone>?,
  var dates: List<Date>?,
  var socialNetworkNetworks: List<SocialNetwork>?,
  var professions: List<Profession>?,
  var emails: List<Email>?,
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

class ContactDto(
  var id: String,
  var clientEditTimestamp: Long,
  var serverEditTimestamp: Long?,
  var deletionTimestamp: Long?,
  var name: String,
  var surname: String?,
  var patronymic: String?,
  var photoPath: String,
  var placeOfMeeting: PlaceOfMeeting?,
  var appearance: String?,
  var contextOfMeeting: String?,
  var city: String?,
  var street: String?,
  var house: String?,
  var flat: String?,
  var notes: String?,
  var site: String?,
  var ownerId: String,
  var tags: List<String>?,
  var telephones: List<Telephone>?,
  var dates: List<Date>?,
  var socialNetworks: List<SocialNetwork>?,
  var professions: List<Profession>?,
  var emails: List<Email>?,
)

class EmailDto(
  var email: String? = null
)

fun Email.toDto(): EmailDto {
  return EmailDto(this@toDto.email)
}

fun EmailDto.toModel(): Email {
  return Email(this@toModel.email)
}

fun Contact.toDto(): ContactDto {
  return ContactDto(
    id = this.id.toHexString(),
    clientEditTimestamp = this.clientEditTimestamp,
    serverEditTimestamp = this.serverEditTimestamp,
    deletionTimestamp = this.deletionTimestamp, // Если нет, используем 0
    name = this.name,
    surname = this.surname,
    patronymic = this.patronymic,
    photoPath = this.photoPath,
    placeOfMeeting = this.placeOfMeeting,
    tags = this.tags ?: emptyList(),
    telephones = this.telephones ?: emptyList(),
    dates = this.dates ?: emptyList(),
    socialNetworks = this.socialNetworkNetworks ?: emptyList(),
    professions = this.professions ?: emptyList(),
    emails = this.emails ?: emptyList(),
    appearance = this.appearance,
    contextOfMeeting = this.contextOfMeeting,
    city = this.city,
    street = this.street,
    house = this.house,
    flat = this.flat,
    notes = this.notes,
    site = this.site,
    ownerId = this.ownerId.toHexString(),
  )
}

fun ContactDto.toModel(): Contact {
  return Contact(
    id = ObjectId(this.id),
    clientEditTimestamp = this.clientEditTimestamp,
    serverEditTimestamp = this.serverEditTimestamp,
    deletionTimestamp = this.deletionTimestamp, // Если нет, используем 0
    name = this.name,
    surname = this.surname,
    patronymic = this.patronymic,
    photoPath = this.photoPath,
    placeOfMeeting = this.placeOfMeeting,
    tags = this.tags ?: emptyList(),
    telephones = this.telephones ?: emptyList(),
    dates = this.dates ?: emptyList(),
    socialNetworkNetworks = this.socialNetworks ?: emptyList(),
    professions = this.professions ?: emptyList(),
    emails = this.emails ?: emptyList(),
    appearance = this.appearance,
    contextOfMeeting = this.contextOfMeeting,
    city = this.city,
    street = this.street,
    house = this.house,
    flat = this.flat,
    notes = this.notes,
    site = this.site,
    ownerId = ObjectId(this.ownerId),
  )
}
