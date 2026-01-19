package com.tallyo.tallyo_backend.model.espn.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TeamOdds {
    boolean favorite;
    boolean underdog;
    boolean favoriteAtOpen;
}
