
package com.citizenbridge.citizenbridge.repository;

import com.citizenbridge.citizenbridge.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByIsActiveTrue();
    List<Category> findByParentId(String parentId);
}