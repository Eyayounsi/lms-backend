package com.elearning.ProjetPfe.repository.recruiter;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.recruiter.JobOffer;
import com.elearning.ProjetPfe.entity.recruiter.JobOfferStatus;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    List<JobOffer> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);

    List<JobOffer> findByStatusOrderByCreatedAtDesc(JobOfferStatus status);

    long countByRecruiterId(Long recruiterId);
}
