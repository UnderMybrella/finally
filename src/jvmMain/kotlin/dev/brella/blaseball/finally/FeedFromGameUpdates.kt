package dev.brella.blaseball.finally

import kotlinx.serialization.json.JsonObject
import java.time.Instant

class FeedFromGameUpdates {
    companion object {
        val TEAM_SETUP = "(.+) vs\\. (.+)".toRegex()

        val INNING_CHANGE = "(Top|Bottom) of (\\d+), (.+) batting.".toRegex()

        val HOME_FIELD_ADVANTAGE = "The (.+) apply Home Field [Aa]dvantage!".toRegex()
    }

    fun buildFromUpdate(input: JsonObject): JsonObject? {
        input.getStringOrNull("lastUpdate")?.let { lastUpdate ->
            if (lastUpdate.equals("Let's Go!", true)) return buildLetsGo(input, null)
            if (lastUpdate.equals("Play ball!", true)) return buildPlayBall(input, null)

            HOME_FIELD_ADVANTAGE.find(lastUpdate)?.let { return buildHomeFieldAdvantage(input, it) }
        }
        return null
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
            tournament = input.getInt("tournament"),
            builder
        )

    fun buildLetsGo(input: JsonObject, matchResult: MatchResult?): JsonObject? {
        return null
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
}