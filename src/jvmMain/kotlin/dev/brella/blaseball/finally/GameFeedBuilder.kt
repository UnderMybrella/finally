package dev.brella.blaseball.finally

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.*
import kotlin.collections.ArrayList

inline fun buildGameFeed(
    game: String,
    homeTeam: String,
    awayTeam: String,
    homeTeamName: String,
    awayTeamName: String,
    season: Int,
    day: Int,
    phase: Int,
    tournament: Int,
    builder: GameFeedBuilder.() -> Unit
): List<JsonObject> =
    GameFeedBuilder(game, homeTeam, awayTeam, homeTeamName, awayTeamName, season, day, phase, tournament)
        .apply(builder)
        .feed

inline fun buildGameFeedEvent(
    game: String,
    homeTeam: String,
    awayTeam: String,
    homeTeamName: String,
    awayTeamName: String,
    season: Int,
    day: Int,
    phase: Int,
    tournament: Int,
    builder: GameFeedBuilder.() -> Any?
): JsonObject {
    val feedBuilder = GameFeedBuilder(game, homeTeam, awayTeam, homeTeamName, awayTeamName, season, day, phase, tournament)
    val response = feedBuilder.let(builder)

    if (response is JsonObject) return response
    return feedBuilder.feed.first()
}


class GameFeedBuilder(
    val game: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val season: Int,
    val day: Int,
    val phase: Int,
    val tournament: Int
) {
    val feed: MutableList<JsonObject> = ArrayList()

    inline fun build(
        category: Int,
        description: String,
        created: String,
        id: String = UUID.randomUUID().toString(),
        gameTags: JsonArray = JsonArray(listOf(JsonPrimitive(game))),
        teamTags: JsonArray = JsonArray(listOf(JsonPrimitive(homeTeam), JsonPrimitive(awayTeam))),
        playerTags: JsonArray = JsonArray(emptyList()),
        type: Int,
        builder: JsonObjectBuilder.() -> Unit
    ) =
        build(category, description, created, id, gameTags, teamTags, playerTags, type, buildJsonObject(builder))

    inline fun build(
        category: Int,
        description: String,
        created: String,
        id: String = UUID.randomUUID().toString(),
        gameTags: JsonArray = JsonArray(listOf(JsonPrimitive(game))),
        teamTags: JsonArray = JsonArray(listOf(JsonPrimitive(homeTeam), JsonPrimitive(awayTeam))),
        playerTags: JsonArray = JsonArray(emptyList()),
        type: Int,
        metadata: JsonElement
    ) = buildJsonObject {
        this["category"] = category
        this["created"] = created
        this["day"] = day
        this["description"] = description
        this["gameTags"] = gameTags
        this["id"] = id
        this["metadata"] = metadata
        this["nuts"] = 0L
        this["phase"] = phase
        this["playerTags"] = playerTags
        this["season"] = season
        this["teamTags"] = teamTags
        this["tournament"] = tournament
        this["type"] = type
    }

    inline fun playBall(created: String, play: Int, subPlay: Int) =
        feed.add(build(category = 0, type = 1, description = "Play ball!", created = created) {
            this["play"] = play
            this["subPlay"] = subPlay
        })

    inline fun gameStart(created: String, stadium: String?, weather: Int, play: Int, subPlay: Int) =
        feed.add(build(category = 0, type = 0, description = "$homeTeamName vs. $awayTeamName", created = created) {
            this["play"] = play
            this["subPlay"] = subPlay
            this["weather"] = weather
            this["home"] = homeTeam
            this["away"] = awayTeam

            if (stadium != null) this["stadium"] = stadium
        })

    inline fun halfInning(created: String, halfInnings: Int, topOfInning: Boolean, play: Int, subPlay: Int) =
        feed.add(build(
            category = 0,
            type = 2,
            description = if (topOfInning) "Top of $halfInnings, $awayTeamName batting." else "Bottom of $halfInnings, $homeTeamName batting.",
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
        })

    inline fun applyHomeFieldAdvantage(created: String, teamNickname: String, play: Int, subPlay: Int) =
        feed.add(build(
            category = 2,
            type = 21,
            description = "The $teamNickname apply Home Field advantage!",
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
        })

    inline fun playerBattingForTeam(created: String, teamNickname: String, playerName: String, play: Int, subPlay: Int) =
        playerBattingForTeam(created, play, subPlay, "$playerName batting for the $teamNickname.")
    inline fun playerBattingForTeam(created: String, play: Int, subPlay: Int, description: String) =
        feed.add(build(
            category = 0,
            type = 12,
            description = description,
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
        })

    inline fun strike(created: String, play: Int, subPlay: Int, description: String) =
        feed.add(build(
            category = 0,
            type = 13,
            description = description,
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
        })

    inline fun strikesOut(created: String, play: Int, subPlay: Int, description: String) =
        feed.add(build(
            category = 0,
            type = 6,
            description = description,
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
        })

    inline fun strikesOutWillingly(created: String, play: Int, subPlay: Int, description: String) =
        feed.add(build(
            category = 0,
            type = 6,
            description = description,
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
        })

    inline fun foulBall(created: String, play: Int, subPlay: Int, description: String) =
        feed.add(build(
            category = 0,
            type = 15,
            description = description,
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
        })

    inline fun ball(created: String, play: Int, subPlay: Int, description: String) =
        feed.add(build(
            category = 0,
            type = 14,
            description = description,
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
        })

    inline fun flyout(id: String = UUID.randomUUID().toString(), created: String, play: Int, subPlay: Int, description: String, metadata: JsonObjectBuilder.() -> Unit) =
        feed.add(build(
            id = id,
            category = 0,
            type = 7,
            description = description,
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay

            metadata()
        })
    inline fun groundOut(id: String = UUID.randomUUID().toString(), created: String, play: Int, subPlay: Int, description: String, metadata: JsonObjectBuilder.() -> Unit) =
        feed.add(build(
            id = id,
            category = 0,
            type = 7,
            description = description,
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay

            metadata()
        })
    inline fun hitBall(id: String = UUID.randomUUID().toString(), created: String, play: Int, subPlay: Int, description: String, metadata: JsonObjectBuilder.() -> Unit) =
        feed.add(build(
            id = id,
            category = 0,
            type = 10,
            description = description,
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay

            metadata()
        })

    inline fun flyoutSacrifice(
        created: String,
        teamNickname: String,
        play: Int,
        subPlay: Int,

        parentID: String? = null,
        awayEmoji: String? = null,
        awayScore: Number? = null,
        homeEmoji: String? = null,
        homeScore: Number? = null,
        ledger: String? = null,
        update: String? = null
    ) =
        feed.add(build(
            category = 0,
            type = 209,
            description = "The $teamNickname scored!",
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
            parentID?.let { this["parent"] = it }
            awayEmoji?.let { this["awayEmoji"] = it }
            awayScore?.let { this["awayScore"] = it }
            homeEmoji?.let { this["homeEmoji"] = it }
            homeScore?.let { this["homeScore"] = it }
            ledger?.let { this["ledger"] = it }
            update?.let { this["update"] = it }
        })

    inline fun playerScored(
        created: String,
        teamNickname: String,
        play: Int,
        subPlay: Int,

        parentID: String? = null,
        awayEmoji: String? = null,
        awayScore: Number? = null,
        homeEmoji: String? = null,
        homeScore: Number? = null,
        ledger: String? = null,
        update: String? = null
    ) =
        feed.add(build(
            category = 0,
            type = 209,
            description = "The $teamNickname scored!",
            created = created
        ) {
            this["play"] = play
            this["subPlay"] = subPlay
            parentID?.let { this["parent"] = it }
            awayEmoji?.let { this["awayEmoji"] = it }
            awayScore?.let { this["awayScore"] = it }
            homeEmoji?.let { this["homeEmoji"] = it }
            homeScore?.let { this["homeScore"] = it }
            ledger?.let { this["ledger"] = it }
            update?.let { this["update"] = it }
        })
}