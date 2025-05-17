package com.citizenbridge.citizenbridge.repository;

import com.citizenbridge.citizenbridge.model.Response;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResponseRepository extends MongoRepository<Response, String> {
}