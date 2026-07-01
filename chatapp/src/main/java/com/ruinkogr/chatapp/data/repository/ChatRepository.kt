package com.ruinkogr.chatapp.data.repository

import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.data.local.MessageDao
import com.ruinkogr.chatapp.data.local.toDto
import com.ruinkogr.chatapp.data.local.toEntity
import com.ruinkogr.chatapp.data.remote.MessagesService
import com.ruinkogr.chatapp.data.remote.UsersService
import com.ruinkogr.chatapp.data.remote.dto.MessageDto
import com.ruinkogr.chatapp.data.remote.dto.SendMessageRequest
import com.ruinkogr.chatapp.data.remote.dto.UserDto
import com.ruinkogr.chatapp.data.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messagesService: MessagesService,
    private val usersService: UsersService,
    private val messageDao: MessageDao,
    private val tokenStorage: TokenStorage //TODO get own id
) : BaseRepository() {

    fun getMessagesWithCache(currentUserId: Int, chatWithUserId: Int): Flow<Resource<List<MessageDto>>> = flow {
        emit(Resource.Loading)

        val cachedData = messageDao.getChatHistoryFlow(currentUserId, chatWithUserId).first()
        if (cachedData.isNotEmpty()) {
            emit(Resource.Success(cachedData.map { it.toDto() }))
        }

        val networkResult = safeApiCall { messagesService.getMessages(chatWithUserId) }

        if (networkResult is Resource.Success) {
            val entities = networkResult.data.map { it.toEntity() }
            messageDao.insertMessages(entities)
        }

        // Live flow from DB
        messageDao.getChatHistoryFlow(currentUserId, chatWithUserId)
            .map { list -> Resource.Success(list.map { it.toDto() }) }
            .collect { emit(it) }

    }.catch { e ->
        emit(Resource.Error("Cache or Network error", e))
    }.flowOn(Dispatchers.IO)

    suspend fun sendMessage(content: String, receiverId: Int): Resource<List<MessageDto>> {
        val request = SendMessageRequest(content, receiverId)
        val result = safeApiCall { messagesService.sendMessage(request) }
        if (result is Resource.Success) {
            messageDao.insertMessages(result.data.map { it.toEntity() })
        }
        return result
    }


    fun getUsers(): Flow<Resource<List<UserDto>>> = flow {
        emit(Resource.Loading)
        val result = safeApiCall { usersService.getUsers() }
        emit(result)
    }.flowOn(Dispatchers.IO)
}