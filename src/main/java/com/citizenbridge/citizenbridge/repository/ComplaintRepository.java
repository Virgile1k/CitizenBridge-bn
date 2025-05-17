package com.citizenbridge.citizenbridge.repository;

import com.citizenbridge.citizenbridge.model.Complaint;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ComplaintRepository extends MongoRepository<Complaint, String> {
}