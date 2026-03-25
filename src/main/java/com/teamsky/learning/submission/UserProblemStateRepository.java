package com.teamsky.learning.submission;

import com.teamsky.learning.submission.entity.UserProblemState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProblemStateRepository extends JpaRepository<UserProblemState, Long> {

    Optional<UserProblemState> findByUser_IdAndProblem_Id(Long userId, Long problemId);
}
