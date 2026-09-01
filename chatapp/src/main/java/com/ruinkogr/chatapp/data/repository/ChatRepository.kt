package com.ruinkogr.chatapp.data.repository

import com.ruinkogr.chatapp.R
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.data.local.MessageDao
import com.ruinkogr.chatapp.data.local.MessageEntity
import com.ruinkogr.chatapp.data.local.toDto
import com.ruinkogr.chatapp.data.local.toEntity
import com.ruinkogr.chatapp.data.remote.MessagesService
import com.ruinkogr.chatapp.data.remote.UsersService
import com.ruinkogr.chatapp.data.remote.dto.MessageDto
import com.ruinkogr.chatapp.data.remote.dto.SendMessageRequest
import com.ruinkogr.chatapp.data.remote.dto.UserDto
import com.ruinkogr.chatapp.data.storage.TokenStorage
import com.ruinkogr.chatapp.util.UiText
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
        emit(Resource.Error(UiText.StringResource(R.string.error_cache_or_network), e))
    }.flowOn(Dispatchers.IO)

    fun getMessagesWithCacheAlt(currentUserId: Int, chatWithUserId: Int): Flow<Resource<List<MessageDto>>> = flow {
        // Progress Bar
        emit(Resource.Loading)

        // 2. Refresh messages request. Try-catch is for do not interrupt DB request below.
        try {
            val networkResult = safeApiCall { messagesService.getMessages(chatWithUserId) }
            if (networkResult is Resource.Success) {
                val entities = networkResult.data.map { it.toEntity() }
                messageDao.insertMessages(entities)
                // Room will push new data into flow
            }
        } catch (e: Exception) {
            // Network or get messages error
        }

        // 3. return live flow. it will emit all new inserted data too
        messageDao.getChatHistoryFlow(currentUserId, chatWithUserId)
            .map { list -> Resource.Success(list.map { it.toDto() }) }
            .collect { emit(it) }

    }.catch { e ->
        emit(Resource.Error(UiText.StringResource(R.string.error_get_messages_critical), e))
    }.flowOn(Dispatchers.IO)

    suspend fun sendMessage(content: String, receiverId: Int): Resource<MessageDto> {
        val request = SendMessageRequest(content, receiverId)
        val response = safeApiCall { messagesService.sendMessage(request) }
        if (response is Resource.Success) {
            messageDao.insertMessages(listOf(response.data.toEntity()))
        }
        return response
    }

    suspend fun saveMessage(messageEntity: MessageEntity) {
        messageDao.insertMessages(listOf(messageEntity))
    }

    suspend fun checkIfMessageExists(id: Int) = messageDao.isMessageExists(id)

    fun getUsers(): Flow<Resource<List<UserDto>>> = flow {
        emit(Resource.Loading)
        val result = safeApiCall { usersService.getUsers() }
        emit(result)
    }.flowOn(Dispatchers.IO)
}