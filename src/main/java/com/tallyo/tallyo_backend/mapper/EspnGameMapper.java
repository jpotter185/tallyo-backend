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
import java.util.Objects;
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

        List<Competitor> competitors = Optional.ofNullable(competition.getCompetitors()).orElse(List.of());
        for (Competitor competitor : competitors) {
            if (competitor == null || competitor.getTeam() == null) {
                continue;
            }

            Team mappedTeam = toTeam(competitor, league);
            if (mappedTeam == null) {
                continue;
            }

            if ("home".equalsIgnoreCase(competitor.getHomeAway())) {
                homeTeam = mappedTeam;
            } else if ("away".equalsIgnoreCase(competitor.getHomeAway())) {
                awayTeam = mappedTeam;
            }
        }

        if (homeTeam == null || awayTeam == null) {
            return null;
        }

        GameOdd odds = buildGameOdd(event, competition);
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
                .seasonType(Optional.ofNullable(event.getSeason())
                        .map(Season::getType)
                        .orElse(0))
                .year(Optional.ofNullable(event.getSeason())
                        .map(Season::getYear)
                        .orElse(0))
                .stadiumName(Optional.ofNullable(competition.getVenue())
                        .map(Venue::getFullName)
                        .orElse(null))
                .location(Optional.ofNullable(competition.getVenue())
                        .map(Venue::getAddress)
                        .map(Address::toString)
                        .orElse(""))
                .isoDate(ZonedDateTime.parse(competition.getStartDate(),
                        DateTimeFormatter.ISO_DATE_TIME).toInstant())
                .homeScore(getScore("home", competitors))
                .awayScore(getScore("away", competitors))
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
                        .map(StatusType::getCompleted)
                        .orElse(null))
                .espnLink(firstLinkHref(event))
                .winner(getWinningTeamId(competition))
                .headline(Optional.ofNullable(competition.getNotes())
                        .orElse(List.of())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(Note::getHeadline)
                        .filter(Objects::nonNull)
                        .findFirst()
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

    private GameOdd buildGameOdd(Event event, Competition competition) {
        String spreadText = Optional.ofNullable(competition.getOdds())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(Odds::getDetails)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (spreadText == null) {
            return null;
        }

        GameOdd odds = new GameOdd();
        odds.setId(Integer.parseInt(event.getId()));
        odds.setSpreadText(spreadText);
        return odds;
    }

    private String firstLinkHref(Event event) {
        return Optional.ofNullable(event.getLinks())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(Link::getHref)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private int getWinningTeamId(Competition competition) {
        if (competition == null || competition.getCompetitors() == null) {
            return 0;
        }

        return competition.getCompetitors().stream()
                .filter(Objects::nonNull)
                .filter(c -> Boolean.TRUE.equals(c.getWinner()))
                .map(Competitor::getTeam)
                .filter(Objects::nonNull)
                .map(com.tallyo.tallyo_backend.model.espn.scoreboard.Team::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0);
    }

    private Team toTeam(Competitor competitor, League league) {
        com.tallyo.tallyo_backend.model.espn.scoreboard.Team espnTeam = competitor.getTeam();
        if (espnTeam == null || espnTeam.getId() == null) {
            return null;
        }

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
        if (competitors == null) {
            return null;
        }

        return competitors.stream()
                .filter(Objects::nonNull)
                .filter(c -> homeAway.equalsIgnoreCase(c.getHomeAway()))
                .map(Competitor::getScore)
                .findFirst()
                .orElse(null);
    }

    private String getOverallRecord(Competitor competitor) {
        return getRecord(competitor);
    }

    private String getRecord(Competitor competitor, String type) {
        return getRecordByType(competitor, type);
    }

    private String getRecord(Competitor competitor) {
        if (competitor.getRecords() == null) {
            return null;
        }
        return competitor.getRecords().stream()
                .filter(Objects::nonNull)
                .map(Record::getSummary)
                .findFirst()
                .orElse(null);
    }

    private String getRecordByType(Competitor competitor, String type) {
        if (competitor.getRecords() == null) {
            return null;
        }

        return competitor.getRecords().stream()
                .filter(Objects::nonNull)
                .filter(r -> type.equalsIgnoreCase(r.getType()))
                .map(Record::getSummary)
                .findFirst()
                .orElse(null);
    }

    private <T> T first(List<T> list) {
        if (list == null) {
            return null;
        }

        return list.stream().filter(Objects::nonNull).findFirst().orElse(null);
    }
}
