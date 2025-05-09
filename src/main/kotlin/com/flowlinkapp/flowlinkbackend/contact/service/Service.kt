@file:OptIn(ExperimentalSerializationApi::class)

package com.flowlinkapp.flowlinkbackend.contact.service

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.flowlinkapp.flowlinkbackend.contact.model.Contact
import com.flowlinkapp.flowlinkbackend.contact.model.Meeting
import com.flowlinkapp.flowlinkbackend.contact.repository.ContactRepository
import com.flowlinkapp.flowlinkbackend.contact.repository.MeetingRepository
import com.flowlinkapp.flowlinkbackend.exceptions.NotFoundServerException
import com.flowlinkapp.flowlinkbackend.exceptions.UnauthorizedServerException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class SyncData(
  val id: ObjectId,
  val updatedAtClient: Long,
  val updatedAtServer: Long,
)

data class SynchronizeInput(
  val contactUpdatesFromClient: List<Contact>,
  val contactUpdatesFromServer: List<SyncData>,
  val meetingUpdatesFromClient: List<Meeting>,
  val meetingUpdatesFromServer: List<SyncData>,
)

data class SynchronizeOutput(
  val contactsToUpdate: List<Contact>,
  val contactsUpdated: List<SyncData>,
  val meetingsToUpdate: List<Meeting>,
  val meetingsUpdated: List<SyncData>,
)

@Serializable
data class PreviousTopic(
  @EncodeDefault
  val contactId: String?,
  @EncodeDefault
  val contactName: String?,
  val question: String,
  val answer: String,
)

@Service
class ContactService(
  val contactRepository: ContactRepository,
  val meetingRepository: MeetingRepository,

  @Value("\${llm-provider.token}")
  val aiToken: String,
  @Value("\${llm-provider.host}")
  val aiHost: String,
) {
  private fun synchronizeContacts(input: SynchronizeInput, userId: ObjectId): SynchronizeOutput {
    val contactsList = contactRepository.findByOwnerId(userId)
    val contacts = mutableMapOf<ObjectId, Contact>()
    val contactsNotMentioned = mutableSetOf<ObjectId>()
    for (contact in contactsList) {
      contacts.put(contact.id, contact)
      contactsNotMentioned.add(contact.id)
    }

    val contactsToUpdate = mutableListOf<Contact>()
    val contactsUpdated = mutableListOf<SyncData>()
    for (entry in input.contactUpdatesFromServer) {
      val contact = contacts[entry.id]
      if (contact == null) continue
      contactsNotMentioned.remove(entry.id)
      contactsToUpdate.add(contact)
    }

    for (clientContact in input.contactUpdatesFromClient) {
      if (clientContact.ownerId != userId) {
        throw UnauthorizedServerException("Client sent contact not owned by client's user")
      }
      val serverContact = contacts[clientContact.id]
      contactsNotMentioned.remove(clientContact.id)
      if (serverContact == null || serverContact.updatedAtClient < clientContact.updatedAtClient) {
        clientContact.updateServerTime()
        contactRepository.save(clientContact)
        contactsUpdated.add(SyncData(clientContact.id, clientContact.updatedAtClient, clientContact.updatedAtServer))
      }
      else {
        contactsToUpdate.add(serverContact)
      }
    }

    for (id in contactsNotMentioned) {
      val contact = contacts[id]
      if (contact == null) continue
      contactsToUpdate.add(contact)
    }

    return SynchronizeOutput(contactsToUpdate, contactsUpdated, emptyList(), emptyList())
  }

  private fun synchronizeMeetings(input: SynchronizeInput, userId: ObjectId): SynchronizeOutput {
    val meetingsList = meetingRepository.findByOwnerId(userId)
    val meetings = mutableMapOf<ObjectId, Meeting>()
    val meetingsNotMentioned = mutableSetOf<ObjectId>()
    for (contact in meetingsList) {
      meetings.put(contact.id, contact)
      meetingsNotMentioned.add(contact.id)
    }

    val meetingsToUpdate = mutableListOf<Meeting>()
    val meetingsUpdated = mutableListOf<SyncData>()
    for (entry in input.contactUpdatesFromServer) {
      val meeting = meetings[entry.id]
      if (meeting == null) continue
      meetingsNotMentioned.remove(entry.id)
      meetingsToUpdate.add(meeting)
    }

    for (clientMeeting in input.meetingUpdatesFromClient) {
      if (clientMeeting.ownerId != userId) {
        throw UnauthorizedServerException("Client sent meeting not owned by client's user")
      }
      val serverMeeting = meetings[clientMeeting.id]
      meetingsNotMentioned.remove(clientMeeting.id)
      if (serverMeeting == null || serverMeeting.updatedAtClient < clientMeeting.updatedAtClient) {
        clientMeeting.updateServerTime()
        meetingRepository.save(clientMeeting)
        meetingsUpdated.add(SyncData(clientMeeting.id, clientMeeting.updatedAtClient, clientMeeting.updatedAtServer))
      }
      else {
        meetingsToUpdate.add(serverMeeting)
      }
    }

    for (id in meetingsNotMentioned) {
      val meeting = meetings[id]
      if (meeting == null) continue
      meetingsToUpdate.add(meeting)
    }

    return SynchronizeOutput(emptyList(), emptyList(), meetingsToUpdate, meetingsUpdated)
  }

  @Transactional
  fun synchronize(input: SynchronizeInput, userId: ObjectId): SynchronizeOutput {
    val syncContacts = synchronizeContacts(input, userId)
    val syncMeetings = synchronizeMeetings(input, userId)
    return SynchronizeOutput(
      syncContacts.contactsToUpdate,
      syncContacts.contactsUpdated,
      syncMeetings.meetingsToUpdate,
      syncMeetings.meetingsUpdated,
    )
  }

  private fun getGeneratedTopics(inputTopics: List<PreviousTopic>): String {
    val inputPrompt = Json.encodeToString(inputTopics)

    val chatRequest = ChatCompletionRequest(
      model = ModelId("deepseek/deepseek-chat-v3-0324:free"),
      messages = listOf(
        ChatMessage(
          role = ChatRole.System,
          content = """
            Task: You are assistant that helps users of social networking management application to come up with new topics for conversations on next meeting appointed with one or more of their contacts. For each request you will have examples of previous conversations with contacts in appointed meeting. Sometimes, user can make mistake and add topic for wrong contact, in this case contactName field and name in answer will be different. You must ignore those topics. Also, user can use different forms of the same name for contact, you must correctly handle such cases.    

            Language: You need to understand and answer in fluent Russian.    

            Format: Examples of conversations will be encoded in JSON format. One document will contain one or more contact objects which contains contact's id, name and one or more conversation examples. You must emit valid JSON compliant with given schema which contains suggested conversation topics for each given example.  
          """.trimIndent()
        ),
        ChatMessage(
          role = ChatRole.User,
          content = """
            {
                "previousConversationTopics": [
                    {
                        "contactId": "67fb5c1a24a887afe02d2785",
                        "contactName": "Александр",
                        "question": "Как успехи на стажировке джава разработчиком?",
                        "answer": "Александр: Достаточно неплохо на самом деле, на следующей неделе дадут новый проект. Он ещё не знает какой, но обещают что-то побольше, более долгосрочное"
                    },
                    {
                        "contactId": "67fb5c1a24a887afe02d2785",
                        "contactName": "Александр",
                        "question": "Есть идея по проекту, бот для записи встреч",
                        "answer": "Саша: Идея интересная, проверю спрос на запись конференций и вернусь с информацией"
                    },
                    {
                        "contactId": "d2a15c1a24a887afe02d90b3",
                        "contactName": "Иван",
                        "question": "Есть ли у тебя на данный момент какие-нибудь проекты",
                        "answer": "Да, сейчас продумываем идею для нового продукта"
                    },
                    {
                        "contactId": null,
                        "contactName": null,
                        "question": "Ты упоминала, что планируешь участвовать в хакатоне",
                        "answer": "Мария: Да, регистрируемся на команду, тема будет связана с медициной"
                    },
                    {
                        "contactId": null,
                        "contactName": null,
                        "question": "У кого что нового?",
                        "answer": "Женя получил повышение, Руслан съездил в отпуск"
                    }
                ]
            }
          """.trimIndent()
        ),
        ChatMessage(
          role = ChatRole.Assistant,
          content = """
            {
                "newConversationTopics": [
                    {
                        "contactId": "67fb5c1a24a887afe02d2785",
                        "contactName": "Александр",
                        "name": "Проект на стажировке",
                        "description": "Какой новый проект тебе дали на стажировке? Насколько он интересный для тебя?"
                    },
                    {
                        "contactId": "67fb5c1a24a887afe02d2785",
                        "contactName": "Александр",
                        "name": "Исследование спроса на бота",
                        "description": "Рассказать о результатах проверки спроса на бота для записи конференций и обсудить дальнейшие шаги по проекту."
                    },
                    {
                        "contactId": "d2a15c1a24a887afe02d90b3",
                        "contactName": "Иван",
                        "name": "Идея нового продукта",
                        "description": "Какую идею вы сейчас рассматриваете для нового продукта? Есть ли уже концепт или прототип?"
                    },
                    {
                        "contactId": null,
                        "contactName": "Мария",
                        "name": "Хакатон по медицине",
                        "description": "Удалось ли зарегистрироваться на хакатон? Какую идею вы собираетесь реализовывать?"
                    },
                    {
                        "contactId": null,
                        "contactName": null,
                        "name": "Новое в жизни",
                        "description": "Спросить у кого что нового?"
                    }
                ]
            }
          """.trimIndent(),
        ),
        ChatMessage(
          role = ChatRole.User,
          content = inputPrompt
        )
      )
    )

    val client = OpenAI(
      token = this.aiToken,
      host = OpenAIHost(this.aiHost)
    )

    val completion = runBlocking { client.chatCompletion(chatRequest) }
    return completion.choices[0].message.messageContent.toString()
  }

  @Transactional
  fun generateTopics(meetingId: ObjectId, userId: ObjectId): String {
    val curMeeting: Meeting? = meetingRepository.findById(meetingId).orElse(null)
    if (curMeeting == null) {
      throw NotFoundServerException("Meeting not found")
    }
    if (curMeeting.ownerId != userId) {
      throw UnauthorizedServerException("User is unauthorized to access this meeting")
    }
    val meetings = meetingRepository.findByOwnerIdOrderByUpdatedOnClientDesc(userId)
    val meetingByContact = mutableMapOf<ObjectId, MutableList<Meeting>>()
    for (meeting in meetings) {
      for (contactId in curMeeting.contactIds) {
        val contactSize = meetingByContact[contactId]?.size
        if (contactSize != null && contactSize >= 2) {
          continue
        }
        if (meeting.contactIds.contains(contactId)) {
          meetingByContact[contactId]?.add(meeting)
        }
      }
    }

    val contacts: MutableList<Contact> = contactRepository.findAllById(curMeeting.contactIds)
    val contactsById: MutableMap<ObjectId, Contact> = mutableMapOf()
    for (contact in contacts) {
      contactsById[contact.id] = contact
    }

    val generationTopics = mutableListOf<PreviousTopic>()
    for (contact in contacts) {
      val meetings = meetingByContact[contact.id] ?: continue
      for (meeting in meetings) {
        for (topic in meeting.topics) {
          val contactId: ObjectId? = if (topic.contactId != null) {
            topic.contactId
          } else if (meeting.contactIds.size == 1) {
            meeting.contactIds[0]
          } else null

          val contact = contactsById[contactId]

          generationTopics.add(PreviousTopic(
            contactId?.toString(),
            "${contact?.firstName} ${contact?.lastName}",
            topic.name,
            topic.description
          ))
        }
      }
    }

    return getGeneratedTopics(generationTopics)
  }
}