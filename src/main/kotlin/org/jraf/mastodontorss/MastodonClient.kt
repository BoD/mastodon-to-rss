/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2021 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.mastodontorss

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private var LOGGER = LoggerFactory.getLogger(MastodonClient::class.java)

class MastodonClient(
  private val server: String,
  private val bearerToken: String,
) {
  private val httpClient by lazy {
    HttpClient {
      install(ContentNegotiation) {
        json(Json {
//          @OptIn(ExperimentalSerializationApi::class)
//          explicitNulls = false
          ignoreUnknownKeys = true
        })
      }
    }
  }

  @Throws(MastodonClientException::class)
  suspend fun getPosts(
    listId: Long,
  ): List<Post> {
    return try {
      LOGGER.debug("Checking for new posts in list $listId")
      val statusList: List<MastodonStatus> = httpClient.get("https://$server/api/v1/timelines/list/$listId") {
        bearerAuth(bearerToken)
        accept(ContentType.Application.Json)
      }.body()

      if (statusList.isEmpty()) {
        LOGGER.debug("No posts")
      }
      statusList.map {
        Post(
          id = it.id,
          url = it.uri,
          createdAt = CREATED_AT_DATE_FORMAT.parse(it.created_at.dropLast(1) + "UTC"),
          isReblog = it.reblog != null,
        )
      }
    } catch (e: Exception) {
      LOGGER.warn("Could not retrieve posts", e)
      throw MastodonClientException(e)
    }
  }
}

class MastodonClientException(cause: Exception) : Throwable(cause.message, cause)

private val CREATED_AT_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

@Serializable
@Suppress("PropertyName")
private data class MastodonStatus(
  val id: String,
  val created_at: String,
  val uri: String,
  val reblog: JsonObject?,
)

data class Post(
  val id: String,
  val url: String,
  val createdAt: Date,
  val isReblog: Boolean,
)
