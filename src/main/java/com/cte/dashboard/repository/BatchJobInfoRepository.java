package com.cte.dashboard.repository;

import com.cte.dashboard.entity.BatchJobInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchJobInfoRepository extends JpaRepository<BatchJobInfo, Long> {

    List<BatchJobInfo> findByJobNameOrderByCreatedAtDesc(String jobName);

    List<BatchJobInfo> findByStatusOrderByCreatedAtDesc(String status);

    List<BatchJobInfo> findTop10ByOrderByCreatedAtDesc();
}
