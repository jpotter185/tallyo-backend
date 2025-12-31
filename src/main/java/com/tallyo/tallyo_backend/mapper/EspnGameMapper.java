package com.tallyo.tallyo_backend.mapper;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.Team;
import com.tallyo.tallyo_backend.entity.TeamKey;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.model.espn.*;
import com.tallyo.tallyo_backend.model.espn.Record;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EspnGameMapper {

    public Game toGame(Event event, League league) {
        Competition competition = first(event.getCompetitions());
        if (competition == null) {
            return null;
        }

        Team homeTeam = null;
        Team awayTeam = null;

        for (Competitor competitor : competition.getCompetitors()) {
            if (competitor == null || competitor.getTeam() == null) continue;

            Team mappedTeam = toTeam(competitor, league);

            if ("home".equalsIgnoreCase(competitor.getHomeAway())) {
                homeTeam = mappedTeam;
            } else if ("away".equalsIgnoreCase(competitor.getHomeAway())) {
                awayTeam = mappedTeam;
            }
        }

        if (homeTeam == null || awayTeam == null) {
            return null;
        }

        Status status = competition.getStatus();
        return Game.builder()
                .id(Integer.parseInt(event.getId()))
                .league(league)
                .homeTeam(homeTeam)
                .homeRecordAtTimeOfGame(homeTeam.getRecord())
                .awayTeam(awayTeam)
                .awayRecordAtTimeOfGame(awayTeam.getRecord())
                .week(event.getWeek().getNumber())
                .seasonType(event.getSeason().getType())
                .year(event.getSeason().getYear())
                .stadiumName(
                        competition.getVenue() != null
                                ? competition.getVenue().getFullName()
                                : null
                )
                .location(
                        competition.getVenue() != null && competition.getVenue().getAddress() != null
                                ? competition.getVenue().getAddress().toString(): ""
                )
                .isoDate(competition.getStartDate())
                .homeScore(getScore("home", competition.getCompetitors()))
                .awayScore(getScore("away", competition.getCompetitors()))
                .period(status != null ? String.valueOf(status.getPeriod()) : null)
                .shortPeriod(
                        status != null && status.getType() != null
                                ? status.getType().getShortDetail()
                                : null
                )
                .channel(competition.getBroadcast())
                .gameStatus(
                        status != null && status.getType() != null
                                ? status.getType().getName()
                                : null
                )
                .finalGame(
                        status != null && status.getType() != null
                                ? status.getType().isCompleted()
                                : null
                )
                .espnLink(event.getLinks().get(0).getHref())
                .winner(getWinningTeamId(competition))
                .headline(competition.getNotes() != null && !competition.getNotes().isEmpty()
                        ? competition.getNotes().get(0).getHeadline()
                        : null)
                .possessionTeamId(competition.getSituation() != null ? competition.getSituation().getPossession(): "")
                .homeTimeouts(competition.getSituation() != null ? competition.getSituation().getHomeTimeouts(): 0)
                .awayTimeouts(competition.getSituation() != null ? competition.getSituation().getAwayTimeouts(): 0)
                .lastPlay(competition.getSituation() != null && competition.getSituation().getLastPlay() != null ?
                    competition.getSituation().getLastPlay().getText(): "")
                .down(competition.getSituation() != null && competition.getSituation().getShortDownDistanceText() != null ?
                        competition.getSituation().getShortDownDistanceText(): "")
                .currentDownAndDistance(competition.getSituation() != null && competition.getSituation().getDownDistanceText() != null ?
                        competition.getSituation().getDownDistanceText(): "")
                .ballLocation(competition.getSituation() != null && competition.getSituation().getPossessionText() != null ?
                        competition.getSituation().getPossessionText(): "")
                .build();

    }

    private int getWinningTeamId(Competition competition) {
        if (competition == null || competition.getCompetitors() == null) {
            return 0;
        }

        return competition.getCompetitors().stream()
                .filter(c -> Boolean.TRUE.equals(c.getWinner()))
                .map(c -> c.getTeam() != null ? c.getTeam().getId() : 0)
                .findFirst()
                .orElse(0);
    }

    private Team toTeam(Competitor competitor, League league) {
        com.tallyo.tallyo_backend.model.espn.Team espnTeam = competitor.getTeam();
        TeamKey teamId = new TeamKey(espnTeam.getId(), league);
        return Team.builder()
                .teamKey(teamId)
                .name(espnTeam.getDisplayName())
                .abbreviation(espnTeam.getAbbreviation())
                .logo(espnTeam.getLogo())
                .primaryColor(espnTeam.getColor())
                .alternateColor(espnTeam.getAlternateColor())
                .location(espnTeam.getLocation())
                .record(getOverallRecord(competitor))
                .homeRecord(getRecord(competitor, "home"))
                .roadRecord(getRecord(competitor, "road"))
                .score(competitor.getScore())
                .build();
    }

    private String getScore(String homeAway, List<Competitor> competitors) {
        if (competitors == null) return null;

        return competitors.stream()
                .filter(c -> homeAway.equalsIgnoreCase(c.getHomeAway()))
                .map(Competitor::getScore)
                .findFirst()
                .orElse(null);
    }

    private String getOverallRecord(Competitor competitor) {
        return getRecordByType(competitor, "total");
    }

    private String getRecord(Competitor competitor, String type) {
        return getRecordByType(competitor, type);
    }

    private String getRecordByType(Competitor competitor, String type) {
        if (competitor.getRecords() == null) return null;

        return competitor.getRecords().stream()
                .filter(r -> type.equalsIgnoreCase(r.getType()))
                .map(Record::getSummary)
                .findFirst()
                .orElse(null);
    }

    private <T> T first(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}

