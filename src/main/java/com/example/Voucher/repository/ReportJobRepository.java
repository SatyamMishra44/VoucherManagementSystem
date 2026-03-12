package com.example.Voucher.repository;

import com.example.Voucher.report.ReportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {
}

