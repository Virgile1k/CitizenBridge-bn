package com.citizenbridge.citizenbridge.repository;

import com.citizenbridge.citizenbridge.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoryRepository extends MongoRepository<Category, String> {
}
