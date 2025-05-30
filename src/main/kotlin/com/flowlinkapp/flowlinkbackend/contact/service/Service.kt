@file:OptIn(ExperimentalSerializationApi::class)

package com.flowlinkapp.flowlinkbackend.contact.service

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.flowlinkapp.flowlinkbackend.contact.model.Contact
import com.flowlinkapp.flowlinkbackend.contact.model.ContactDto
import com.flowlinkapp.flowlinkbackend.contact.model.Meeting
import com.flowlinkapp.flowlinkbackend.contact.model.MeetingDto
import com.flowlinkapp.flowlinkbackend.contact.model.Topic
import com.flowlinkapp.flowlinkbackend.contact.model.toDto
import com.flowlinkapp.flowlinkbackend.contact.model.toModel
import com.flowlinkapp.flowlinkbackend.contact.repository.ContactRepository
import com.flowlinkapp.flowlinkbackend.contact.repository.MeetingRepository
import com.flowlinkapp.flowlinkbackend.exceptions.NotFoundServerException
import com.flowlinkapp.flowlinkbackend.exceptions.UnauthorizedServerException
import io.minio.MinioClient
import io.minio.PutObjectArgs
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
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class SyncData(
  val id: ObjectId,
  val clientEditTimestamp: Long,
  val updatedAtServer: Long?,
)

data class SyncDataDto(
  val id: String,
  val clientEditTimestamp: Long,
  val updatedAtServer: Long?,
)

fun SyncData.toDto(): SyncDataDto = SyncDataDto(
  id = this.id.toHexString(),
  clientEditTimestamp = this.clientEditTimestamp,
  updatedAtServer = this.updatedAtServer,
)

fun SyncDataDto.toModel(): SyncData = SyncData(
  id = ObjectId(this.id),
  clientEditTimestamp = this.clientEditTimestamp,
  updatedAtServer = this.updatedAtServer,
)

data class SynchronizeInput(
  val contactUpdatesFromClient: List<ContactDto>,
  val contactUpdatesFromServer: List<SyncDataDto>,
  val meetingUpdatesFromClient: List<MeetingDto>,
  val meetingUpdatesFromServer: List<SyncDataDto>,
)

data class SynchronizeOutput(
  val contactsToUpdate: List<ContactDto>,
  val contactsUpdated: List<SyncDataDto>,
  val meetingsToUpdate: List<MeetingDto>,
  val meetingsUpdated: List<SyncDataDto>,
)

data class UploadedObjects(
  val uploaded: List<String>
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

@Serializable
data class TopicGenerationInput(
  val previousConversationTopics: List<PreviousTopic>
)

@Serializable
data class GeneratedTopic(
  @EncodeDefault
  val contactId: String?,
  @EncodeDefault
  val contactName: String?,
  val name: String,
  val description: String,
)

@Serializable
data class TopicGenerationOutput(
  val newConversationTopics: List<GeneratedTopic>
)

@Service
class ContactService(
  val contactRepository: ContactRepository,
  val meetingRepository: MeetingRepository,
  val minioClient: MinioClient,
  @Value("\${s3.bucket}")
  val bucketName: String,

  @Value("\${llm-provider.token}")
  val aiToken: String,
  @Value("\${llm-provider.host}")
  val aiHost: String,
) {
  @Transactional
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
    for (entry in input.contactUpdatesFromServer.map { it.toModel() }) {
      val contact = contacts[entry.id]
      if (contact == null) continue
      contactsNotMentioned.remove(entry.id)
      contactsToUpdate.add(contact)
    }

    for (clientContact in input.contactUpdatesFromClient.map { it.toModel() }) {
      if (clientContact.ownerId != userId) {
        throw UnauthorizedServerException("Client sent contact not owned by client's user")
      }
      val serverContact = contacts[clientContact.id]
      contactsNotMentioned.remove(clientContact.id)
      if (serverContact != null && serverContact.clientEditTimestamp == clientContact.clientEditTimestamp) {
        continue
      }
      if (serverContact == null || serverContact.clientEditTimestamp < clientContact.clientEditTimestamp) {
        clientContact.updateServerTime()
        contactRepository.save(clientContact)
        contactsUpdated.add(SyncData(clientContact.id, clientContact.clientEditTimestamp,
          clientContact.serverEditTimestamp))
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

    return SynchronizeOutput(contactsToUpdate.map { it.toDto() },
      contactsUpdated.map { it.toDto() }, emptyList(), emptyList())
  }

  @Transactional
  private fun synchronizeMeetings(input: SynchronizeInput, userId: ObjectId): SynchronizeOutput {
    println("Synchronized input: $input")
    val meetingsList = meetingRepository.findByOwnerId(userId)
    val meetings = mutableMapOf<ObjectId, Meeting>()
    val meetingsNotMentioned = mutableSetOf<ObjectId>()
    for (contact in meetingsList) {
      meetings.put(contact.id, contact)
      meetingsNotMentioned.add(contact.id)
    }

    val meetingsToUpdate = mutableListOf<Meeting>()
    val meetingsUpdated = mutableListOf<SyncData>()
    for (entry in input.contactUpdatesFromServer.map { it.toModel() }) {
      val meeting = meetings[entry.id]
      if (meeting == null) continue
      meetingsNotMentioned.remove(entry.id)
      meetingsToUpdate.add(meeting)
    }

    println("Original list of meetings from client: ${input.meetingUpdatesFromClient}, mapped: ${input.meetingUpdatesFromClient.map { it.toModel() }}")
    for (clientMeeting in input.meetingUpdatesFromClient.map { it.toModel() }) {
      if (clientMeeting.ownerId != userId) {
        throw UnauthorizedServerException("Client sent meeting not owned by client's user")
      }
      val serverMeeting = meetings[clientMeeting.id]
      meetingsNotMentioned.remove(clientMeeting.id)
      if (serverMeeting != null && serverMeeting.clientEditTimestamp == clientMeeting.clientEditTimestamp) {
        continue
      }
      if (serverMeeting == null || serverMeeting.clientEditTimestamp < clientMeeting.clientEditTimestamp) {
        clientMeeting.updateServerTime()
        println("save client meeting $clientMeeting")
        meetingRepository.save(clientMeeting)
        meetingsUpdated.add(SyncData(clientMeeting.id, clientMeeting.clientEditTimestamp,
          clientMeeting.serverEditTimestamp))
      } else {
        meetingsToUpdate.add(serverMeeting)
      }
    }

    for (id in meetingsNotMentioned) {
      val meeting = meetings[id]
      if (meeting == null) continue
      meetingsToUpdate.add(meeting)
    }

    return SynchronizeOutput(emptyList(), emptyList(),
      meetingsToUpdate.map { it.toDto() }, meetingsUpdated.map { it.toDto() })
  }

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

  private fun getGeneratedTopics(inputTopics: TopicGenerationInput): TopicGenerationOutput {
    val inputPrompt = Json.encodeToString(inputTopics)
//    println("Input topics are: $inputTopics")
//    println("Input prompt is: $inputPrompt")

    val chatRequest = ChatCompletionRequest(
      model = ModelId("meta-llama/llama-3.3-8b-instruct:free"),
      messages = listOf(
        ChatMessage(
          role = ChatRole.System,
          content = """
            Task: You are assistant that helps users of social networking management application to come up with new topics for conversations on next meeting appointed with one or more of their contacts. For each request you will have examples of previous conversations with contacts in appointed meeting. Sometimes, user can make mistake and add topic for wrong contact, in this case contactName field and name in answer will be different. You must ignore those topics. Also, user can use different forms of the same name for contact, you must correctly handle such cases.    

            Language: You need to understand and answer in fluent Russian.    

            Format: Examples of conversations will be encoded in JSON format. One document will contain one or more contact objects which contains contact's id, name and one or more conversation examples. You must emit valid RAW JSON compliant with given schema which contains suggested conversation topics for each given example.  
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

    println("Chat request is $chatRequest")

    println("OpenAi builder started")
    val client = OpenAI(
      token = this.aiToken,
      host = OpenAIHost(this.aiHost)
    )
    println("OpenAi builder finished, client is: $client")

    val completion = runBlocking {
      val response = client.chatCompletion(chatRequest)
      println("Raw response: ${response.choices[0].message.content}")
      response
    }
    println("completion finished")

    val markdownContent = completion.choices[0].message.content
    val cleanContent = markdownContent
      ?.replace("```json\n", "")
      ?.replace("\n```", "")
      ?.trim()

    println("Raw content: $markdownContent")
    println("Clean content: $cleanContent")
    val toReturn = Json.decodeFromString<TopicGenerationOutput>(
      cleanContent ?: ""
    )
    return toReturn
  }

  @Transactional
  fun generateTopics(meetingId: ObjectId, userId: ObjectId): MeetingDto {
    val curMeeting: Meeting? = meetingRepository.findById(meetingId).orElse(null)
    if (curMeeting == null) {
      throw NotFoundServerException("Meeting not found")
    }
//    println("Current meeting non-null")
    if (curMeeting.ownerId != userId) {
      throw UnauthorizedServerException("User is unauthorized to access this meeting")
    }
    val meetings = meetingRepository.findByOwnerIdOrderByClientEditTimestampDesc(userId)
    val meetingByContact = mutableMapOf<ObjectId, MutableList<Meeting>>()
//    println("Found ${meetings.size} meetings associated with this user")
    for (meeting in meetings) {
      println("for meeting $meeting")
      for (contactId in curMeeting.contactIds) {
        println("for contactId $contactId")
        val contactSize = meetingByContact[contactId]?.size
        if (contactSize != null && contactSize >= 2) {
          continue
        }
        println("Contact size with this contact is ok")
        if (meeting.contactIds.contains(contactId)) {
          println("this meeting contains contact with id $contactId")
          if (meetingByContact.containsKey(contactId)) {
            meetingByContact[contactId]?.add(meeting)
          } else {
            meetingByContact.put(contactId, mutableListOf(meeting))
          }
        }
      }
    }
    println("Meetings with this contact found: $meetingByContact, size is ${meetingByContact.size}")

    val contacts: MutableList<Contact> = contactRepository.findAllById(curMeeting.contactIds)
//    println("How much contact found in db: ${curMeeting.contactIds.size}/${contacts.size}")
    val contactsById: MutableMap<ObjectId, Contact> = mutableMapOf()
    for (contact in contacts) {
      contactsById[contact.id] = contact
    }

    val previousTopics = mutableListOf<PreviousTopic>()
    for (contact in contacts) {
      println("For contact $contact")
      val meetings = meetingByContact[contact.id] ?: continue
      println("Associated meetings are $meetings")
      for (meeting in meetings) {
        println("For meeting $meeting")
        for (topic in meeting.topics) {
          println("For topic $topic")
          val contactId: ObjectId? = if (topic.contactId != null) {
            topic.contactId
          } else if (meeting.contactIds.size == 1) {
            meeting.contactIds[0]
          } else null
          println("this topic is related to contact with id $contactId (null if not determined)")

          val contact = contactsById[contactId]

          println("add to prev topics list this: contactId is ${contactId?.toString()}, " +
                  "contactName is ${contact?.name} ${contact?.surname}, question is ${topic.name}, answer is ${topic.answer}")
          previousTopics.add(PreviousTopic(
            contactId?.toString(),
            "${contact?.name} ${contact?.surname}",
            "${topic.name}, ${topic.description}",
            topic.answer ?: ""
          ))
        }
      }
    }

    println("prev topics: ${previousTopics}")
    val generationTopics = TopicGenerationInput(previousTopics)
    val generatedTopics = getGeneratedTopics(generationTopics)

    for (topic in generatedTopics.newConversationTopics) {
      curMeeting.topics.add(Topic(
        name = topic.name,
        description = topic.description,
        answer = null,
        contactId = ObjectId(topic.contactId),
        isGenerated = true
      ))
    }

    meetingRepository.save(curMeeting)
    return curMeeting.toDto()
  }

  fun uploadFiles(files: List<MultipartFile>): UploadedObjects {
    return UploadedObjects(
     uploaded = files.map { file ->
       val objectName = UUID.randomUUID().toString() + "-" + file.originalFilename
       minioClient.putObject(
         PutObjectArgs.builder()
           .bucket(bucketName)
           .`object`(objectName)
           .stream(file.inputStream, file.size, -1)
           .contentType(file.contentType)
           .build()
       )
       objectName
     }
    )
  }
}