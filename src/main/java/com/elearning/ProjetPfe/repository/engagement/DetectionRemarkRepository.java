package com.elearning.ProjetPfe.repository.engagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.engagement.DetectionRemark;

@Repository
public interface DetectionRemarkRepository extends JpaRepository<DetectionRemark, Long> {

    List<DetectionRemark> findByStudentIdAndCourseIdOrderByDetectedAtDesc(Long studentId, Long courseId);

    List<DetectionRemark> findByCourseIdOrderByDetectedAtDesc(Long courseId);

    List<DetectionRemark> findByStudentIdOrderByDetectedAtDesc(Long studentId);

    long countByStudentIdAndCourseId(Long studentId, Long courseId);
}
