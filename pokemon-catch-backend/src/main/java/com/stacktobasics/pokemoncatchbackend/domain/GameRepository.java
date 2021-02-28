package com.stacktobasics.pokemoncatchbackend.domain;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface GameRepository extends CrudRepository<Game, String> {
    @Override
    List<Game> findAll();
}
