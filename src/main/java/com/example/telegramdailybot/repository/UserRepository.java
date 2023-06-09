package com.example.telegramdailybot.repository;

import com.example.telegramdailybot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findByChatid(Long chatid);

}
