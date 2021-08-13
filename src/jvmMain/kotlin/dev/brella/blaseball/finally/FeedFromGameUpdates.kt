package dev.brella.blaseball.finally

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.putJsonArray
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

class FeedFromGameUpdates {
    companion object {
        val TEAM_SETUP = "(.+) vs\\. (.+)".toRegex()

        val INNING_CHANGE = "(Top|Bottom) of (\\d+), (.+) batting.".toRegex()
        val PLAYER_BATTING_FOR_TEAM = "(.+?) (?:batting|skipped up to bat) for the (.+?)(?:\\.$|, wielding (.+?)\\.$)".toRegex()

        val STRIKE = "Strike, (flinching|looking|swinging). (-?\\d+(?:\\.\\d+)?)-(-?\\d+(?:\\.\\d+)?)".toRegex()
        val STRIKES_OUT = "(.+?) strikes out (looking|swinging|thinking).".toRegex()
        val STRIKES_OUT_WILLINGLY = "(.+?) swings (\\d+) times to strike out willingly!".toRegex()

        val HOME_FIELD_ADVANTAGE = "The (.+) apply Home Field [Aa]dvantage!".toRegex()

        val FOUL_BALL = "(Offworld\\s+)?(Very\\s+)?Foul Ball.\\s+(-?\\d+(?:\\.\\d+)?)-(-?\\d+(?:\\.\\d+)?)".toRegex()
        val BALL = "Ball.\\s+(-?\\d+(?:\\.\\d+)?)-(-?\\d+(?:\\.\\d+)?)".toRegex()

        val FLYOUT = "(.+) hit a flyout to (.+)(?:'s? Shell)?.".toRegex()
        val GROUND_OUT = "(.+) hit a ground out to (.+)(?:'s? Shell)?.".toRegex()
        val HIT_INTO_PLAY = "(.+) hit into a ([Ss]ingle|[Dd]ouble|[Tt]riple|[Qq]uadruple) play!".toRegex()
        val HIT_BALL = "(.+) hits a ([Ss]ingle|[Dd]ouble|[Tt]riple|[Qq]uadruple)!".toRegex()

        //Child Events
        val FLYOUT_SACRIFICE = "(.+) tags up and scores!".toRegex()
        val COOLED_OFF = "(.+) cooled off.".toRegex()
        val BECAME_UNSTABLE = "(.+) became Unstable!".toRegex()
        val BEING_OBSERVED = "(.+) is now being Observed.".toRegex()
        val FREE_REFILL = "(.+) used their Free Refill.".toRegex()
        val ITEM_BROKE = "(.+)'s? (.+) broke!".toRegex()
        val ITEM_DAMAGED = "(.+)'s? (.+) (?:was|were) damaged.".toRegex()
        val HYPE_BUILT = "Hype built in (.+)!".toRegex()
        val PLAYER_PARTYING = "(.+) is Partying!".toRegex()
        val POWER_CHARGED = "(.+) Power Chaaarged!".toRegex()
        val RED_HOT = "(.+) is Red Hot!".toRegex()
        val SCORED = "(.+) scores!".toRegex()
    }

    fun buildFromUpdate(input: JsonObject): JsonElement? {
        input.getStringOrNull("lastUpdate")?.let { lastUpdate ->
            if (lastUpdate.equals("Let's Go!", true)) return buildLetsGo(input, null)
            if (lastUpdate.equals("Play ball!", true)) return buildPlayBall(input, null)

            PLAYER_BATTING_FOR_TEAM.find(lastUpdate)?.let { return buildPlayerBatting(lastUpdate, input, it) }
            INNING_CHANGE.find(lastUpdate)?.let { return buildInningChange(input, it) }

            STRIKE.find(lastUpdate)?.let { return buildStrike(lastUpdate, input, it) }
            STRIKES_OUT.find(lastUpdate)?.let { return buildStrikesOut(lastUpdate, input, it) }
            STRIKES_OUT_WILLINGLY.find(lastUpdate)?.let { return buildStrikesOutWillingly(lastUpdate, input, it) }

            FOUL_BALL.find(lastUpdate)?.let { return buildFoulBall(lastUpdate, input, it) }
            BALL.find(lastUpdate)?.let { return buildBall(lastUpdate, input, it) }

            FLYOUT.find(lastUpdate)?.let { return buildFlyout(lastUpdate, input, it) }
            GROUND_OUT.find(lastUpdate)?.let { return buildGroundOut(lastUpdate, input, it) }
            HIT_INTO_PLAY.find(lastUpdate)?.let { return buildHitIntoPlay(lastUpdate, input, it) }

            HIT_BALL.find(lastUpdate)?.let { return buildHitBall(lastUpdate, input, it) }

            HOME_FIELD_ADVANTAGE.find(lastUpdate)?.let { return buildHomeFieldAdvantage(input, it) }
        }
        return null
    }

    fun buildChildEvents(parentID: String, input: JsonObject): List<JsonObject> {
        val list: MutableList<JsonObject> = ArrayList()
        var last: JsonElement? = null
        input.getStringOrNull("lastUpdate")?.let { lastUpdate ->
            var lastUpdate = lastUpdate
            do {
                when (val last = last) {
                    is JsonObject -> list.add(last)
                    is JsonArray -> last.forEach { if (it is JsonObject) list.add(it) }
                    else -> {
                    }
                }
                last = null

                FLYOUT_SACRIFICE.find(lastUpdate)?.let {
                    last = buildFlyoutSacrifice(input, it, parentID, list.size)
                    lastUpdate = lastUpdate.removeRange(it.range)
                }

                val scores: MutableList<MatchResult> = ArrayList()
                while (true) SCORED.find(lastUpdate)?.let {
                    scores.add(it)
                    lastUpdate = lastUpdate.removeRange(it.range)
                } ?: break

                if (scores.isNotEmpty()) last = buildPlayerScored(input, scores, parentID, list.size)

            } while (last != null)
        }

        return list
    }

    private inline fun buildFrom(input: JsonObject, builder: GameFeedBuilder.() -> Any?): JsonObject =
        buildGameFeedEvent(
            game = input.getStringOrNull("id") ?: input.getString("_id"),
            homeTeam = input.getString("homeTeam"),
            awayTeam = input.getString("awayTeam"),
            homeTeamName = input.getString("homeTeamName"),
            awayTeamName = input.getString("awayTeamName"),
            season = input.getInt("season"),
            day = input.getInt("day"),
            //Game Event doesn't have phase unfortunately
            phase = input.getIntOrNull("_phase") ?: -1,
            tournament = input.getIntOrNull("tournament") ?: -1,
            builder
        )

    private inline fun buildEventsFrom(input: JsonObject, builder: GameFeedBuilder.() -> Unit): JsonArray =
        JsonArray(
            buildGameFeed(
                game = input.getStringOrNull("id") ?: input.getString("_id"),
                homeTeam = input.getString("homeTeam"),
                awayTeam = input.getString("awayTeam"),
                homeTeamName = input.getString("homeTeamName"),
                awayTeamName = input.getString("awayTeamName"),
                season = input.getInt("season"),
                day = input.getInt("day"),
                //Game Event doesn't have phase unfortunately
                phase = input.getIntOrNull("_phase") ?: -1,
                tournament = input.getIntOrNull("tournament") ?: -1,
                builder
            )
        )

    fun buildLetsGo(input: JsonObject, matchResult: MatchResult?): JsonObject =
        buildFrom(input) {
            gameStart(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getStringOrNull("stadiumId"),
                input.getInt("weather"),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1
            )
        }

    fun buildPlayBall(input: JsonObject, matchResult: MatchResult?): JsonObject? =
        buildFrom(input) {
            playBall(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1
            )
        }

    fun buildHomeFieldAdvantage(input: JsonObject, matchResult: MatchResult?): JsonObject? =
        buildFrom(input) {
            applyHomeFieldAdvantage(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                matchResult?.groupValues?.getOrNull(1) ?: "{NICKNAME}",
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1
            )
        }

    fun buildInningChange(input: JsonObject, matchResult: MatchResult?): JsonObject? =
        buildFrom(input) {
            halfInning(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                matchResult?.groupValues?.getOrNull(2)?.toIntOrNull() ?: -1,
                matchResult?.groupValues?.getOrNull(1) == "Top",
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1
            )
        }

    fun buildPlayerBatting(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonObject? =
        buildFrom(input) {
            playerBattingForTeam(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            )
        }

    fun buildStrike(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonObject? =
        buildFrom(input) {
            strike(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            )
        }

    fun buildStrikesOut(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonObject? =
        buildFrom(input) {
            strikesOut(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            )
        }

    fun buildStrikesOutWillingly(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonObject? =
        buildFrom(input) {
            strikesOutWillingly(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            )
        }

    fun buildFoulBall(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonObject =
        buildFrom(input) {
            foulBall(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            )
        }

    fun buildBall(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonObject =
        buildFrom(input) {
            ball(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            )
        }

    fun buildFlyout(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonArray =
        buildEventsFrom(input) {
            val flyoutID = UUID.randomUUID().toString()
            val childEvents = buildChildEvents(flyoutID, input)
            flyout(
                id = flyoutID,
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            ) {
                if (childEvents.isNotEmpty())
                    putJsonArray("children") {
                        childEvents.forEach { json -> add(json.getString("id")) }
                    }
            }

            feed.addAll(childEvents)
        }

    fun buildGroundOut(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonArray =
        buildEventsFrom(input) {
            val groundOutID = UUID.randomUUID().toString()
            val childEvents = buildChildEvents(groundOutID, input)
            groundOut(
                id = groundOutID,
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            ) {
                if (childEvents.isNotEmpty())
                    putJsonArray("children") {
                        childEvents.forEach { json -> add(json.getString("id")) }
                    }
            }

            feed.addAll(childEvents)
        }
    fun buildHitIntoPlay(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonArray =
        buildEventsFrom(input) {
            val groundOutID = UUID.randomUUID().toString()
            val childEvents = buildChildEvents(groundOutID, input)
            groundOut(
                id = groundOutID,
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            ) {
                if (childEvents.isNotEmpty())
                    putJsonArray("children") {
                        childEvents.forEach { json -> add(json.getString("id")) }
                    }
            }

            feed.addAll(childEvents)
        }
    fun buildHitBall(lastUpdate: String, input: JsonObject, matchResult: MatchResult?): JsonArray =
        buildEventsFrom(input) {
            val groundOutID = UUID.randomUUID().toString()
            val childEvents = buildChildEvents(groundOutID, input)
            hitBall(
                id = groundOutID,
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                input.getIntOrNull("playCount") ?: -1,
                input.getIntOrNull("subPlayCount") ?: -1,
                description = lastUpdate
            ) {
                if (childEvents.isNotEmpty())
                    putJsonArray("children") {
                        childEvents.forEach { json -> add(json.getString("id")) }
                    }
            }

            feed.addAll(childEvents)
        }

    fun buildFlyoutSacrifice(input: JsonObject, matchResult: MatchResult?, parentID: String, subPlay: Int = -1): JsonElement =
        buildFrom(input) {
            flyoutSacrifice(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                "{TEAM_NICKNAME}", //TODO
                input.getIntOrNull("playCount") ?: -1,
                subPlay = subPlay,

                parentID = parentID,
                awayEmoji = input.getStringOrNull("awayEmoji"),
                awayScore = input.getNumberOrNull("awayScore"),
                homeEmoji = input.getStringOrNull("homeEmoji"),
                homeScore = input.getNumberOrNull("homeScore"),
                ledger = input.getStringOrNull("scoreLedger"),// ?: "Sacrifice: 1 Run",
                update = input.getStringOrNull("scoreUpdate"),// ?: "1 Runs scored!"
            )
        }

    fun buildPlayerScored(input: JsonObject, matchResults: List<MatchResult>, parentID: String, subPlay: Int = -1): JsonElement =
        buildFrom(input) {
            playerScored(
                input.getStringOrNull("_created") ?: Instant.now().toString(),
                "{TEAM_NICKNAME}", //TODO
                input.getIntOrNull("playCount") ?: -1,
                subPlay = subPlay,

                parentID = parentID,
                awayEmoji = input.getStringOrNull("awayEmoji"),
                awayScore = input.getNumberOrNull("awayScore"),
                homeEmoji = input.getStringOrNull("homeEmoji"),
                homeScore = input.getNumberOrNull("homeScore"),
                ledger = input.getStringOrNull("scoreLedger"),// ?: "Base Hit: 1 Run",
                update = input.getStringOrNull("scoreUpdate"),// ?: "1 Runs scored!"
            )
        }
}