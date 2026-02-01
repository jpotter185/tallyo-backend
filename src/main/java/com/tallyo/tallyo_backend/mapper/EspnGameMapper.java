package com.tallyo.tallyo_backend.mapper;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.GameOdd;
import com.tallyo.tallyo_backend.entity.Team;
import com.tallyo.tallyo_backend.entity.TeamKey;
import com.tallyo.tallyo_backend.enums.League;
import com.tallyo.tallyo_backend.model.espn.scoreboard.*;
import com.tallyo.tallyo_backend.model.espn.scoreboard.Record;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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

        GameOdd odds = null;
        if (competition.getOdds() != null && !competition.getOdds().isEmpty()) {
            odds = new GameOdd();
            odds.setId(Integer.parseInt(event.getId()));
            odds.setSpreadText((competition.getOdds().get(0).getDetails()));
        }

        Status status = competition.getStatus();
        Game game = Game.builder()
                .id(Integer.parseInt(event.getId()))
                .league(league)
                .homeTeam(homeTeam)
                .gameOdd(odds)
                .homeRecordAtTimeOfGame(homeTeam.getRecord())
                .awayTeam(awayTeam)
                .awayRecordAtTimeOfGame(awayTeam.getRecord())
                .week(Optional.ofNullable(event.getWeek())
                        .map(Week::getNumber)
                        .orElse(0))
                .seasonType(event.getSeason().getType())
                .year(event.getSeason().getYear())
                .stadiumName(Optional.ofNullable(competition.getVenue())
                        .map(Venue::getFullName)
                        .orElse(null))
                .location(Optional.ofNullable(competition.getVenue())
                        .map(Venue::getAddress)
                        .map(Address::toString)
                        .orElse(""))
                .isoDate(ZonedDateTime.parse(competition.getStartDate(),
                        DateTimeFormatter.ISO_DATE_TIME).toInstant())
                .homeScore(getScore("home", competition.getCompetitors()))
                .awayScore(getScore("away", competition.getCompetitors()))
                .period(Optional.ofNullable(status)
                        .map(Status::getPeriod)
                        .map(String::valueOf)
                        .orElse(null))
                .shortPeriod(Optional.ofNullable(status)
                        .map(Status::getType)
                        .map(StatusType::getShortDetail)
                        .orElse(null))
                .channel(competition.getBroadcast())
                .gameStatus(Optional.ofNullable(status)
                        .map(Status::getType)
                        .map(StatusType::getName)
                        .orElse(null))
                .finalGame(Optional.ofNullable(status)
                        .map(Status::getType)
                        .map(StatusType::isCompleted)
                        .orElse(null))
                .espnLink(event.getLinks().get(0).getHref())
                .winner(getWinningTeamId(competition))
                .headline(Optional.ofNullable(competition.getNotes())
                        .filter(notes -> !notes.isEmpty())
                        .map(notes -> notes.get(0).getHeadline())
                        .orElse(null))
                .possessionTeamId(Optional.ofNullable(competition.getSituation())
                        .map(Situation::getPossession)
                        .orElse(""))
                .homeTimeouts(Optional.ofNullable(competition.getSituation())
                        .map(Situation::getHomeTimeouts)
                        .orElse(0))
                .awayTimeouts(Optional.ofNullable(competition.getSituation())
                        .map(Situation::getAwayTimeouts)
                        .orElse(0))
                .lastPlay(Optional.ofNullable(competition.getSituation())
                        .map(Situation::getLastPlay)
                        .map(LastPlay::getText)
                        .orElse(""))
                .down(Optional.ofNullable(competition.getSituation())
                        .map(Situation::getShortDownDistanceText)
                        .orElse(""))
                .currentDownAndDistance(Optional.ofNullable(competition.getSituation())
                        .map(Situation::getDownDistanceText)
                        .orElse(""))
                .ballLocation(Optional.ofNullable(competition.getSituation())
                        .map(Situation::getPossessionText)
                        .orElse(""))
                .build();

        if (odds != null) {
            odds.setGame(game);
        }
        return game;
    }

    private int getWinningTeamId(Competition competition) {
        if (competition == null || competition.getCompetitors() == null) {
            return 0;
        }

        return competition.getCompetitors().stream().filter(c -> Boolean.TRUE.equals(c.getWinner())).map(c -> c.getTeam() != null ? c.getTeam().getId() : 0).findFirst().orElse(0);
    }

    private Team toTeam(Competitor competitor, League league) {
        com.tallyo.tallyo_backend.model.espn.scoreboard.Team espnTeam = competitor.getTeam();
        TeamKey teamId = new TeamKey(espnTeam.getId(), league);
        return Team.builder().teamKey(teamId).name(espnTeam.getDisplayName()).abbreviation(espnTeam.getAbbreviation()).logo(espnTeam.getLogo()).primaryColor(espnTeam.getColor()).alternateColor(espnTeam.getAlternateColor()).location(espnTeam.getLocation()).record(getOverallRecord(competitor)).homeRecord(getRecord(competitor, "home")).roadRecord(getRecord(competitor, "road")).score(competitor.getScore()).build();
    }

    private String getScore(String homeAway, List<Competitor> competitors) {
        if (competitors == null) return null;

        return competitors.stream().filter(c -> homeAway.equalsIgnoreCase(c.getHomeAway())).map(Competitor::getScore).findFirst().orElse(null);
    }

    private String getOverallRecord(Competitor competitor) {
        return getRecord(competitor);
//        return getRecordByType(competitor, "total");
    }

    private String getRecord(Competitor competitor, String type) {
        return getRecordByType(competitor, type);
    }

    private String getRecord(Competitor competitor) {
        if (competitor.getRecords() == null) {
            return null;
        }
        return competitor.getRecords().stream().map(Record::getSummary).findFirst().orElse(null);
    }

    private String getRecordByType(Competitor competitor, String type) {
        if (competitor.getRecords() == null) return null;
        return competitor.getRecords().stream().filter(r -> type.equalsIgnoreCase(r.getType())).map(Record::getSummary).findFirst().orElse("JACK");
    }

    private <T> T first(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}

