package com.flowlinkapp.flowlinkbackend.contact.service

import com.flowlinkapp.flowlinkbackend.contact.model.Contact
import com.flowlinkapp.flowlinkbackend.contact.model.ContactDto
import com.flowlinkapp.flowlinkbackend.contact.model.Meeting
import com.flowlinkapp.flowlinkbackend.contact.model.MeetingDto
import com.flowlinkapp.flowlinkbackend.contact.model.toContact
import com.flowlinkapp.flowlinkbackend.contact.model.toDto
import com.flowlinkapp.flowlinkbackend.contact.model.toMeeting
import com.flowlinkapp.flowlinkbackend.contact.repository.ContactRepository
import com.flowlinkapp.flowlinkbackend.contact.repository.MeetingRepository
import com.flowlinkapp.flowlinkbackend.exceptions.UnauthorizedServerException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class SyncData(
  val id: String,
  val updatedAtClient: Long,
  val updatedAtServer: Long,
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
    val contacts = mutableMapOf<String, ContactDto>()
    val contactsNotMentioned = mutableSetOf<String>()
    for (contact in contactsList) {
      contacts.put(contact.id.toHexString(), contact.toDto())
      contactsNotMentioned.add(contact.id.toHexString())
    }

    val contactsToUpdate = mutableListOf<ContactDto>()
    val contactsUpdated = mutableListOf<SyncData>()
    for (entry in input.contactUpdatesFromServer) {
      val contact = contacts[entry.id]
      if (contact == null) continue
      contactsNotMentioned.remove(entry.id)
      contactsToUpdate.add(contact)
    }

    for (clientContact in input.contactUpdatesFromClient) {
      val client = clientContact.toContact()
      if (client.ownerId != userId) {
        throw UnauthorizedServerException("Client sent contact not owned by client's user")
      }
      val serverContact = contacts[clientContact.id]
      contactsNotMentioned.remove(clientContact.id)
      if (serverContact == null || serverContact.clientEditTimestamp < clientContact.clientEditTimestamp) {
        clientContact.updateServerTime()
        client.updateServerTime()
        contactRepository.save(client)
        contactsUpdated.add(SyncData(clientContact.id, clientContact.clientEditTimestamp, clientContact.serverEditTimestamp))
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
    val meetings = mutableMapOf<String, MeetingDto>()
    val meetingsNotMentioned = mutableSetOf<String>()
    for (contact in meetingsList) {
      val meeting = contact.toDto()
      meetings.put(meeting.id, meeting)
      meetingsNotMentioned.add(meeting.id)
    }

    val meetingsToUpdate = mutableListOf<MeetingDto>()
    val meetingsUpdated = mutableListOf<SyncData>()
    for (entry in input.contactUpdatesFromServer) {
      val meeting = meetings[entry.id]
      if (meeting == null) continue
      meetingsNotMentioned.remove(entry.id)
      meetingsToUpdate.add(meeting)
    }

    for (clientMeeting in input.meetingUpdatesFromClient) {
      if (!clientMeeting.ownerId.equals(userId)) {
        throw UnauthorizedServerException("Client sent meeting not owned by client's user")
      }
      val serverMeeting = meetings[clientMeeting.id]
      meetingsNotMentioned.remove(clientMeeting.id)
      if (serverMeeting == null || serverMeeting.updatedAtClient < clientMeeting.updatedAtClient) {
        clientMeeting.updateServerTime()
        meetingRepository.save(clientMeeting.toMeeting())
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
}