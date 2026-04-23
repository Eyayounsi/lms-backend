package com.elearning.ProjetPfe.repository.payment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.payment.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    long countByStudentIdAndUsedFalse(Long studentId);

    Optional<Coupon> findByCouponCode(String couponCode);
}
