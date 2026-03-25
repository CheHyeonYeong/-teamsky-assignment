package com.teamsky.learning.bookmark.presentation;

import com.teamsky.learning.bookmark.application.BookmarkService;
import com.teamsky.learning.bookmark.presentation.request.BookmarkRequest;
import com.teamsky.learning.bookmark.presentation.response.BookmarkResponse;
import com.teamsky.learning.shared.presentation.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookmark", description = "Bookmark APIs")
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "Add bookmark", description = "Add a problem to bookmarks.")
    @PostMapping
    public ResponseEntity<BookmarkResponse> addBookmark(@Valid @RequestBody BookmarkRequest request) {
        BookmarkResponse response = bookmarkService.addBookmark(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List bookmarks", description = "Get a paged list of bookmarked problems.")
    @GetMapping
    public ResponseEntity<PageResponse<BookmarkResponse>> getBookmarks(
            @RequestParam Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(bookmarkService.getBookmarks(userId, pageable)));
    }

    @Operation(summary = "Remove bookmark", description = "Remove a bookmark.")
    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<Void> removeBookmark(@PathVariable Long bookmarkId) {
        bookmarkService.removeBookmark(bookmarkId);
        return ResponseEntity.ok().build();
    }
}