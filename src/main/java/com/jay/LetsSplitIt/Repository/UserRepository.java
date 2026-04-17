package com.jay.LetsSplitIt.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.jay.LetsSplitIt.Entities.User;
import org.bson.types.ObjectId;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User,ObjectId> {
    Optional<User> findByEmail(String email);
}
