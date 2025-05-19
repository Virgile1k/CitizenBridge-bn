package com.citizenbridge.citizenbridge.repository;

import com.citizenbridge.citizenbridge.model.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ComplaintRepository extends MongoRepository<Complaint, String> {

    /**
     * Find complaints with various filtering options
     *
     * @param submittedBy The ID of the user who submitted the complaint (optional)
     * @param status Complaint status filter (optional)
     * @param priority Complaint priority filter (optional)
     * @param categoryId Category ID filter (optional)
     * @param agencyId Agency ID filter (optional)
     * @param searchTerm Search term for title/description (optional)
     * @param pageable Pagination parameters
     * @return Paginated list of complaints matching the criteria
     */
    @Query("{ " +
            "$and: [ " +
            "  { $or: [ " +
            "    { 'submittedBy': { $regex: ?0, $options: 'i' } }, " +
            "    { '_id': { $exists: true } } " +
            "  ] }, " +
            "  { $or: [ " +
            "    { 'status': { $regex: ?1, $options: 'i' } }, " +
            "    { 'status': { $exists: true } } " +
            "  ] }, " +
            "  { $or: [ " +
            "    { 'priority': { $regex: ?2, $options: 'i' } }, " +
            "    { 'priority': { $exists: true } } " +
            "  ] }, " +
            "  { $or: [ " +
            "    { 'categoryId': { $regex: ?3, $options: 'i' } }, " +
            "    { 'categoryId': { $exists: true } } " +
            "  ] }, " +
            "  { $or: [ " +
            "    { 'agencyId': ?4 }, " +
            "    { 'agencyId': { $exists: true } } " +
            "  ] }, " +
            "  { $or: [ " +
            "    { 'title': { $regex: ?5, $options: 'i' } }, " +
            "    { 'description': { $regex: ?5, $options: 'i' } }, " +
            "    { 'referenceNumber': { $regex: ?5, $options: 'i' } } " +
            "  ] } " +
            "] }")
    Page<Complaint> findByFilters(String submittedBy, String status, String priority,
                                  String categoryId, String agencyId, String searchTerm,
                                  Pageable pageable);

    /**
     * Find complaints by agency ID and date range
     *
     * @param agencyId Agency ID
     * @param startDate Start date of the range (inclusive)
     * @param endDate End date of the range (inclusive)
     * @return List of complaints matching the criteria
     */
    @Query("{ 'agencyId': ?0, 'createdAt': { $gte: ?1, $lte: ?2 } }")
    List<Complaint> findByAgencyIdAndDateRange(String agencyId, LocalDate startDate, LocalDate endDate);

    /**
     * Find complaints by date range
     *
     * @param startDate Start date of the range (inclusive)
     * @param endDate End date of the range (inclusive)
     * @return List of complaints matching the criteria
     */
    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 } }")
    List<Complaint> findByDateRange(LocalDate startDate, LocalDate endDate);
}