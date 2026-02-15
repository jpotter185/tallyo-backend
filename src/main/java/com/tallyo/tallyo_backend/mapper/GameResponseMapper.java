package com.tallyo.tallyo_backend.mapper;

import com.tallyo.tallyo_backend.dto.*;
import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class GameResponseMapper {

    public GameResponse toResponse(Game game) {
        if (game == null) {
            return null;
        }
        return GameResponse.builder()
                .id(String.valueOf(game.getId()))
                .league(game.getLeague() != null ? game.getLeague().name().toLowerCase() : null)
                .homeTeam(toTeamResponse(game.getHomeTeam()))
                .awayTeam(toTeamResponse(game.getAwayTeam()))
                .stadiumName(game.getStadiumName())
                .location(game.getLocation())
                .isoDate(game.getIsoDate() != null ? game.getIsoDate().toString() : null)
                .date(game.getIsoDate() != null ? game.getIsoDate().toString() : null)
                .homeScore(game.getHomeScore())
                .awayScore(game.getAwayScore())
                .period(game.getPeriod())
                .shortPeriod(game.getShortPeriod())
                .channel(game.getChannel())
                .espnLink(game.getEspnLink())
                .lastPlay(game.getLastPlay())
                .currentDownAndDistance(game.getCurrentDownAndDistance())
                .down(game.getDown())
                .ballLocation(game.getBallLocation())
                .possessionTeamId(game.getPossessionTeamId())
                .homeTimeouts(game.getHomeTimeouts())
                .awayTimeouts(game.getAwayTimeouts())
                .winner(game.getWinner() == 0 ? null : String.valueOf(game.getWinner()))
                .headline(game.getHeadline())
                .gameOdd(game.getGameOdd() == null ? null : GameOddResponse.builder()
                        .spreadText(game.getGameOdd().getSpreadText())
                        .build())
                .gameStatus(game.getGameStatus())
                .stats(game.getStatsForJson())
                .homeWinPercentage(game.getHomeWinPercentage())
                .awayWinPercentage(game.getAwayWinPercentage())
                .finalGame(game.getFinalGame())
                .finalValue(game.getFinalGame())
                .homeRecordAtTimeOfGame(game.getHomeRecordAtTimeOfGame())
                .awayRecordAtTimeOfGame(game.getAwayRecordAtTimeOfGame())
                .build();
    }

    private TeamResponse toTeamResponse(Team team) {
        if (team == null) {
            return null;
        }

        TeamKeyResponse keyResponse = team.getTeamKey() == null ? null : TeamKeyResponse.builder()
                .teamId(team.getTeamKey().getTeamId())
                .league(team.getTeamKey().getLeague() != null ? team.getTeamKey().getLeague().name().toLowerCase() : null)
                .build();

        return TeamResponse.builder()
                .teamKey(keyResponse)
                .id(keyResponse != null ? String.valueOf(keyResponse.getTeamId()) : null)
                .name(team.getName())
                .abbreviation(team.getAbbreviation())
                .logo(team.getLogo())
                .primaryColor(team.getPrimaryColor())
                .alternateColor(team.getAlternateColor())
                .location(team.getLocation())
                .record(team.getRecord())
                .seed(team.getSeed())
                .ranking(team.getRanking())
                .wins(team.getWins())
                .losses(team.getLosses())
                .ties(team.getTies())
                .conference(team.getConference())
                .division(team.getDivision())
                .homeRecord(team.getHomeRecord())
                .roadRecord(team.getRoadRecord())
                .recordVsConference(team.getRecordVsConference())
                .recordVsDivision(team.getRecordVsDivision())
                .pointsFor(team.getPointsFor())
                .pointsAgainst(team.getPointsAgainst())
                .pointDifferential(team.getPointDifferential())
                .streak(team.getStreak())
                .winPercent(team.getWinPercent())
                .score(team.getScore())
                .build();
    }
}
