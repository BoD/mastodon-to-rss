/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2022-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.host
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.charsets.Charsets
import org.jraf.mastodontorss.util.urlEncoded

private const val PORT = 8080

private const val PATH_LIST_ID = "listId"

private const val PARAM_SERVER = "server"
private const val PARAM_BEARER_TOKEN = "bearerToken"

fun main() {
  val listenPort = PORT
  embeddedServer(CIO, listenPort, module = Application::mastodonToRssModule).start(wait = true)
}

private fun Application.mastodonToRssModule() {
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
      val listId = call.parameters[PATH_LIST_ID] ?: throw IllegalArgumentException("Missing $PATH_LIST_ID")

      val server = call.request.queryParameters[PARAM_SERVER] ?: throw IllegalArgumentException("Missing $PARAM_SERVER")
      val bearerToken = call.request.queryParameters[PARAM_BEARER_TOKEN] ?: throw IllegalArgumentException("Missing $PARAM_BEARER_TOKEN")

      val selfLink =
        URLBuilder("${call.request.origin.scheme}://${call.request.host()}${call.request.uri}").apply {
          call.request.queryParameters.forEach { key, values ->
            parameters.append(key, values[0])
          }
        }.buildString()
      call.respondText(
        getAtom(
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

private suspend fun getAtom(
  selfLink: String,
  server: String,
  bearerToken: String,
  listId: String,
): String {
  val mastodonClient = MastodonClient(
    server = server,
    bearerToken = bearerToken,
  )
  val posts = mastodonClient.getPosts(listId)
  return """<?xml version="1.0" encoding="utf-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom">
      <title>Mastodon list $listId</title>
      <link href="${selfLink.urlEncoded()}" rel="self"/>
      <updated>${posts.firstOrNull()?.createdAt}</updated>
      ${
    posts.joinToString(separator = "\n") { post ->
      """
        <entry>
          <link href="${post.url.urlEncoded()}" />
          <id>${post.url.urlEncoded()}</id>
          <updated>${post.createdAt}</updated>
        </entry>
      """.trimIndent()
    }
  }
    </feed>
  """.trimIndent()
}
