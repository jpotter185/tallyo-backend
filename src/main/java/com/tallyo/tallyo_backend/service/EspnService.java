package com.tallyo.tallyo_backend.service;

import com.tallyo.tallyo_backend.entity.Game;
import com.tallyo.tallyo_backend.entity.Team;
import com.tallyo.tallyo_backend.enums.League;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class EspnService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");


    public EspnService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public List<Game> fetchGames(String league){
        String url =   "https://site.api.espn.com/apis/site/v2/sports/football/"
                + league + "/scoreboard";
        String jsonResponse = restTemplate.getForObject(url, String.class);
        System.out.println(jsonResponse);
        JsonNode data = objectMapper.readTree(jsonResponse);
        List<Game> games = new ArrayList<>();


        JsonNode events = data.path("events");
        if (events.isArray() && events.size() > 0) {
            for (JsonNode event : events) {
                JsonNode competitions = event.path("competitions");
                for (JsonNode competition : competitions) {

                    JsonNode competitors = competition.path("competitors");

                    JsonNode homeNode = findCompetitor(competitors, "home");
                    JsonNode awayNode = findCompetitor(competitors, "away");
                    JsonNode winnerNode = findWinner(competitors);

                    Team homeTeam = buildTeam(homeNode);
                    Team awayTeam = buildTeam(awayNode);

                    String gameStatus = competition.path("status").path("type").path("name").asString();
                    String homeScore = determineScore(homeNode.path("score").asString(""), gameStatus);
                    String awayScore = determineScore(awayNode.path("score").asString(""), gameStatus);

                    String possessionTeamId = competition.path("situation").path("possession").asString(null);
                    String gameLocation = buildGameLocationString(competition.path("venue").path("address"));
                    String channel = competition.path("broadcast").asString(null);

                    String headline = "";
                    JsonNode notes = competition.path("notes");
                    if (notes.isArray() && notes.size() > 0) {
                        headline = notes.get(0).path("headline").asString("");
                    }

                    String currentDownAndDistance = competition.path("situation").path("downDistanceText").asString(null);
                    Integer homeTimeouts = competition.path("situation").path("homeTimeouts").asInt(0);
                    Integer awayTimeouts = competition.path("situation").path("awayTimeouts").asInt(0);



                    String homeWinPercentage = competition.path("situation").path("lastPlay").path("probability")
                            .path("homeWinPercentage").asString(null);
                    String awayWinPercentage = competition.path("situation").path("lastPlay").path("probability")
                            .path("awayWinPercentage").asString(null);

                    Game game = new Game();
                    if(competition.path("startDate").asString()  != null){
                        game.setIsoDate(competition.path("startDate").asString());
                    }
                    game.setId(competition.path("id").asInt());
                    game.setLeague(League.valueOf(league.toUpperCase()));
                    game.setHomeTeam(homeTeam);
                    game.setAwayTeam(awayTeam);
                    game.setStadiumName(competition.path("venue").path("fullName").asString(null));
                    game.setLocation(gameLocation);
                    game.setHomeScore(homeScore);
                    game.setAwayScore(awayScore);
                    game.setGameStatus(gameStatus);
                    game.setPeriod(competition.path("status").path("type").path("detail").asString(null));
                    game.setShortPeriod(competition.path("status").path("type").path("shortDetail").asString(null));
                    game.setChannel(channel);
                    game.setEspnLink(event.path("links").get(0).path("href").asString(null));
                    game.setLastPlay(competition.path("situation").path("lastPlay").path("text").asString(null));
                    game.setPossessionTeamId(possessionTeamId);
                    game.setCurrentDownAndDistance(currentDownAndDistance);
                    game.setDown(competition.path("situation").path("shortDownDistanceText").asString(null));
                    game.setBallLocation(competition.path("situation").path("possessionText").asString(null));
                    game.setWinner(winnerNode != null ? winnerNode.path("id").asString(null) : null);
                    game.setFinalGame(winnerNode != null);
                    game.setHeadline(headline);
                    game.setHomeTimeouts(homeTimeouts);
                    game.setAwayTimeouts(awayTimeouts);
                    game.setHomeWinPercentage(homeWinPercentage);
                    game.setAwayWinPercentage(awayWinPercentage);

                    games.add(game);
                }
            }
        }

        return games;
    }

    private JsonNode findCompetitor(JsonNode competitors, String homeAway) {
        for (JsonNode c : competitors) {
            if (c.path("homeAway").asString("").equalsIgnoreCase(homeAway)) {
                return c;
            }
        }
        return null;
    }

    private JsonNode findWinner(JsonNode competitors) {
        for (JsonNode c : competitors) {
            if (c.path("winner").asBoolean(false)) {
                return c;
            }
        }
        return null;
    }

    private Team buildTeam(JsonNode teamJson) {
        if (teamJson == null) return null;

        Team team = new Team();
        int rank = teamJson.path("curatedRank").path("current").asInt(99);
        team.setRanking(rank == 99 ? "" : "#" + rank + " ");
        team.setId(teamJson.path("id").asInt());
        team.setName(teamJson.path("team").path("displayName").asString());
        team.setAbbreviation(teamJson.path("team").path("abbreviation").asString());
        team.setLogo(teamJson.path("team").path("logo").asString(null));
        team.setPrimaryColor(teamJson.path("team").path("color").asString(null));
        team.setAlternateColor(teamJson.path("team").path("alternateColor").asString(null));
        team.setLocation(teamJson.path("team").path("location").asString(null));
        team.setRecord(getTeamRecord(teamJson.path("records")));

        return team;
    }

    private String getTeamRecord(JsonNode records) {
        if (records != null && records.isArray() && records.size() > 0) {
            for (JsonNode r : records) {
                if (r.path("name").asString("").equalsIgnoreCase("overall")) {
                    return r.path("summary").asString("0-0");
                }
            }
        }
        return "0-0";
    }

    private String determineScore(String score, String gameStatus) {
        return "STATUS_SCHEDULED".equalsIgnoreCase(gameStatus) ? "" : score;
    }

    private String buildGameLocationString(JsonNode address) {
        if (address != null && !address.isMissingNode()) {
            String city = address.path("city").asString(null);
            String state = address.path("state").asString(null);
            String country = address.path("country").asString(null);

            return (city != null ? city + ", " : "") +
                    (state != null ? state + ", " : "") +
                    (country != null ? country : "");
        }
        return "";
    }
    }



