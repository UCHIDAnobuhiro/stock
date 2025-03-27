package com.example.stock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.stock.model.Favorites;
import com.example.stock.model.Users;

public interface FavoritesRepository extends JpaRepository<Favorites, Long> {
	List<Favorites> findByUser(Users user);
}