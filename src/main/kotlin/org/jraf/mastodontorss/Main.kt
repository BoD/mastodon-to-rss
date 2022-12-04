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

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.host
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private const val DEFAULT_PORT = 8080

private const val ENV_PORT = "PORT"

private const val PATH_LIST_ID = "listId"

private const val PARAM_SERVER = "server"
private const val PARAM_BEARER_TOKEN = "bearerToken"

private val PUB_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss 'Z'", Locale.US)

fun main() {
  val listenPort = System.getenv(ENV_PORT)?.toInt() ?: DEFAULT_PORT
  embeddedServer(Netty, listenPort, module = Application::mastodonToRssModule).start(wait = true)
}

private fun Application.mastodonToRssModule() {
  install(DefaultHeaders)

  install(StatusPages) {
    status(HttpStatusCode.NotFound) { call, status ->
      call.respondText(
        text = "Usage: ${call.request.local.scheme}://${call.request.local.host}:${call.request.local.port}//<$PATH_LIST_ID>",
        status = status
      )
    }

    exception<IllegalArgumentException> { call, exception ->
      call.respond(HttpStatusCode.BadRequest, exception.message ?: "Bad request")
    }
    exception<MastodonClientException> { call, exception ->
      call.respond(
        HttpStatusCode.BadRequest, exception.message ?: "Could not retrieve the list's tweets"
      )
    }
  }

  routing {
    get("{$PATH_LIST_ID}") {
      val listId =
        call.parameters[PATH_LIST_ID]?.toLongOrNull() ?: throw IllegalArgumentException("Invalid list ID")

      val server = call.request.queryParameters[PARAM_SERVER] ?: throw IllegalArgumentException("Missing server")
      val bearerToken = call.request.queryParameters[PARAM_BEARER_TOKEN] ?: throw IllegalArgumentException("Missing bearerToken")

      val selfLink =
        URLBuilder("${call.request.origin.scheme}://${call.request.host()}${call.request.uri}").apply {
          parameters.append(PARAM_BEARER_TOKEN, bearerToken)
        }.buildString()
      call.respondText(
        getRss(
          selfLink = selfLink,
          server = server,
          bearerToken = bearerToken,
          listId = listId,
        ),
        ContentType.Application.Rss.withCharset(Charsets.UTF_8)
      )
    }
  }
}

private suspend fun getRss(
  selfLink: String,
  server: String,
  bearerToken: String,
  listId: Long,
): String {
  val mastodonClient = MastodonClient(
    server = server,
    bearerToken = bearerToken,
  )
  val posts = mastodonClient.getPosts(listId)

  return xml("rss") {
    includeXmlProlog = true
    attribute("version", "2.0")
    "channel" {
      "title" { -"Posts for list $listId" }
      "description" { -"Posts for list $listId" }
      "link" { -selfLink }
      "ttl" { -"60" }
      for (post in posts) {
        "item" {
          "link" { -post.url }
          "guid" {
            attribute("isPermaLink", "true")
            -post.url
          }
          "pubDate" { -formatPubDate(post.createdAt) }
          // Slack RSS bot already fetches the text from the link, so it's not necessary to include it here
//                    "description" { -post.text }
        }
      }
    }
  }.toString(PrintOptions(singleLineTextElements = true, indent = "  "))
}

private fun formatPubDate(date: Date): String =
  PUB_DATE_FORMAT.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("GMT")))

