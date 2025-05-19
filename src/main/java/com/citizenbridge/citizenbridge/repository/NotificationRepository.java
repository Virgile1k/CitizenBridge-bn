package com.citizenbridge.citizenbridge.repository;

import com.citizenbridge.citizenbridge.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserId(String userId);
    List<Notification> findByUserIdAndType(String userId, String type);
    List<Notification> findByComplaintId(String complaintId);
}