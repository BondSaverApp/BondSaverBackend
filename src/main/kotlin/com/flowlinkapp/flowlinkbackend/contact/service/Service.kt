package com.flowlinkapp.flowlinkbackend.contact.service

import com.flowlinkapp.flowlinkbackend.contact.model.Contact
import com.flowlinkapp.flowlinkbackend.contact.model.ContactDto
import com.flowlinkapp.flowlinkbackend.contact.model.Meeting
import com.flowlinkapp.flowlinkbackend.contact.model.MeetingDto
import com.flowlinkapp.flowlinkbackend.contact.model.toDto
import com.flowlinkapp.flowlinkbackend.contact.model.toModel
import com.flowlinkapp.flowlinkbackend.contact.repository.ContactRepository
import com.flowlinkapp.flowlinkbackend.contact.repository.MeetingRepository
import com.flowlinkapp.flowlinkbackend.exceptions.UnauthorizedServerException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class SyncData(
  val id: String,
  val updatedAtClient: Long,
  val updatedAtServer: Long?,
)

data class SynchronizeInput(
  val contactUpdatesFromClient: List<ContactDto>,
  val contactUpdatesFromServer: List<SyncData>,
  val meetingUpdatesFromClient: List<MeetingDto>,
  val meetingUpdatesFromServer: List<SyncData>,
)

data class SynchronizeOutput(
  val contactsToUpdate: List<ContactDto>,
  val contactsUpdated: List<SyncData>,
  val meetingsToUpdate: List<MeetingDto>,
  val meetingsUpdated: List<SyncData>,
)

@Service
class ContactService(
  val contactRepository: ContactRepository,
  val meetingRepository: MeetingRepository,
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
      val contact = contacts[ObjectId(entry.id)]
      if (contact == null) continue
      contactsNotMentioned.remove(ObjectId(entry.id))
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
        clientContact.updateServerTime(clientContact.clientEditTimestamp)
        contactRepository.save(clientContact)
        contactsUpdated.add(SyncData(clientContact.id.toHexString(), clientContact.clientEditTimestamp, clientContact.serverEditTimestamp))
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

    return SynchronizeOutput(contactsToUpdate.map { it.toDto() }, contactsUpdated, emptyList(), emptyList())
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
      val meeting = meetings[ObjectId(entry.id)]
      if (meeting == null) continue
      meetingsNotMentioned.remove(ObjectId(entry.id))
      meetingsToUpdate.add(meeting)
    }

    for (clientMeeting in input.meetingUpdatesFromClient.map { it.toModel() }) {
      if (clientMeeting.ownerId != userId) {
        throw UnauthorizedServerException("Client sent meeting not owned by client's user")
      }
      val serverMeeting = meetings[clientMeeting.id]
      meetingsNotMentioned.remove(clientMeeting.id)
      if (serverMeeting != null && serverMeeting.updatedAtClient == clientMeeting.updatedAtClient) {
        continue
      }
      if (serverMeeting == null || serverMeeting.updatedAtClient < clientMeeting.updatedAtClient) {
        clientMeeting.updateServerTime(clientMeeting.updatedAtClient)
        meetingRepository.save(clientMeeting)
        meetingsUpdated.add(SyncData(clientMeeting.id.toHexString(), clientMeeting.updatedAtClient, clientMeeting.updatedAtServer))
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

    return SynchronizeOutput(emptyList(), emptyList(), meetingsToUpdate.map { it.toDto() }, meetingsUpdated)
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
}