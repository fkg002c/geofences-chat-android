package com.ruinkogr.chatapp.ui.search

import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class SearchRepository @Inject constructor() {
    private val namesDatabase = listOf(
        "Александр", "Алексей", "Анастасия", "Анна", "Артем",
        "Борис", "Валентина", "Валерий", "Виктория", "Владимир",
        "Дмитрий", "Даниил", "Евгения", "Егор", "Екатерина",
        "Иван", "Игорь", "Ирина", "Илья", "Константин",
        "Максим", "Михаил", "Мария", "Николай", "Ольга"
    )

    suspend fun performSearch(query: String): List<String> {
        val randomDelay = Random.nextLong(10, 201)
        if (randomDelay in 150..200) throw IllegalStateException("too long operation")
        delay(randomDelay.milliseconds)
        if (query.isBlank()) {
            return emptyList()
        }
        return namesDatabase.filter { name ->
            name.contains(query, ignoreCase = true)
        }
    }
}
