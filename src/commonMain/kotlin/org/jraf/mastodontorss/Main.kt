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
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.host
import io.ktor.server.request.port
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.charsets.Charsets
import org.jraf.mastodontorss.atom.Atom
import org.jraf.mastodontorss.mastodon.MastodonClient
import org.jraf.mastodontorss.mastodon.MastodonClientException
import org.jraf.mastodontorss.util.exitProcess
import org.jraf.mastodontorss.util.gc

private const val PORT = 8080

private const val PATH_LIST_ID = "listId"

private const val PARAM_SERVER = "server"
private const val PARAM_BEARER_TOKEN = "bearerToken"

fun main() {
  val listenPort = PORT
  embeddedServer(CIO, listenPort, module = Application::mastodonToRssModule).start(wait = true)
}

private fun Application.mastodonToRssModule() {
  var counter: Int = 0

  install(StatusPages) {
    status(HttpStatusCode.NotFound) { call, status ->
      call.respondText(
        text = "Usage: ${call.request.origin.scheme}://${call.request.host()}${call.portStr()}//<$PATH_LIST_ID>",
        status = status
      )
    }

    exception<MastodonClientException> { call, exception ->
      call.respond(HttpStatusCode.BadRequest, "Could not retrieve the list's posts: " + (exception.message ?: "unknown error"))
    }
    exception<IllegalArgumentException> { call, exception ->
      call.respond(HttpStatusCode.BadRequest, "Bad request: " + (exception.message ?: "unknown error"))
    }
  }

  routing {
    get("{$PATH_LIST_ID}") {
      val listId = call.parameters[PATH_LIST_ID] ?: throw IllegalArgumentException("Missing $PATH_LIST_ID")

      val server = call.request.queryParameters[PARAM_SERVER] ?: throw IllegalArgumentException("Missing $PARAM_SERVER")
      val bearerToken = call.request.queryParameters[PARAM_BEARER_TOKEN] ?: throw IllegalArgumentException("Missing $PARAM_BEARER_TOKEN")

      val selfLink = URLBuilder("${call.request.origin.scheme}://${call.request.host()}${call.portStr()}${call.request.uri}").buildString()
      val mastodonClient = MastodonClient(
        server = server,
        bearerToken = bearerToken,
      )
      val mastodonPosts = mastodonClient.getPosts(listId).filterNot { it.isReblog }
      val atomPosts = mastodonPosts.map { post ->
        Atom.Post(
          url = post.url,
          updated = post.createdAt,
        )
      }
      val atomText = Atom(
        title = "Mastodon list $listId",
        selfLink = selfLink,
        posts = atomPosts,
      ).getText()
      call.respondText(
        atomText,
        ContentType.Application.Atom.withCharset(Charsets.UTF_8)
      )
      gc()

      // See https://youtrack.jetbrains.com/issue/KTOR-4288/Non-heap-memory-leak-when-making-a-request-and-closing-a-client
      counter++
      if (counter == 100) {
        exitProcess(-1)
      }
    }
  }
}

private fun ApplicationCall.portStr() = request.port().let { if (it == 80) "" else ":$it" }
