package com.uiop07558.socnet.contact.model

import kotlinx.datetime.Instant
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
class Telephone(
  private val type: String,
  private val number: String,
)

@Document
class Date(
  private val type: String,
  private val number: Instant,
)

enum class SocialType(val type: String) {
  Telegram("Telegram"),
  VK("VK"),
}

@Document
class Social(
  private val type: SocialType,
  private val link: String,
)

@Document
class Occupation(
  private val profession: String,
  private val company: String,
  private val jobTitle: String,
)

@Document(collection = "contacts")
class Contact(
  @Id
  private val id: ObjectId,
  private val profilePic: String,
  private val firstName: String,
  private val lastName: String,
  private val middleName: String,
  private val appearance: String,
  private val meetContext: String,
  private val city: String,
  private val street: String,
  private val house: String,
  private val flat: String,
  private val notes: String,
  private val site: String,
  private val ownerId: ObjectId,
  private val meetPlaces: List<String>,
  private val tags: List<String>,
  private val emails: List<String>,
  private val telephones: List<Telephone>,
  private val dates: List<Date>,
  private val social: List<Social>,
  private val occupations: List<Occupation>,
) {}