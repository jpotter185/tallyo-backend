package com.tallyo.tallyo_backend.model;

import com.tallyo.tallyo_backend.entity.Game;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GameResponse {
    List<Game> games;
    int gameCount;
}
