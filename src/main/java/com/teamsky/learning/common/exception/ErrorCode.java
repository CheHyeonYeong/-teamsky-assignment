package com.teamsky.learning.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid input value"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "Internal server error"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),

    // Chapter
    CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "Chapter not found"),

    // Problem
    PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "Problem not found"),
    NO_MORE_PROBLEMS(HttpStatus.NOT_FOUND, "P002", "No more problems available in this chapter"),

    // Submission
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Submission not found"),
    ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "S002", "Already submitted this problem"),

    // Bookmark
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "Bookmark not found"),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "B002", "Bookmark already exists");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
