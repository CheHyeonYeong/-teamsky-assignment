package com.teamsky.learning.bookmark;

import com.teamsky.learning.bookmark.request.BookmarkRequest;
import com.teamsky.learning.bookmark.response.BookmarkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Bookmark", description = "북마크 관련 API")
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "북마크 추가", description = "문제를 북마크에 추가합니다.")
    @PostMapping
    public ResponseEntity<BookmarkResponse> addBookmark(@Valid @RequestBody BookmarkRequest request) {
        BookmarkResponse response = bookmarkService.addBookmark(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "북마크 목록", description = "북마크한 문제 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<BookmarkResponse>> getBookmarks(
            @RequestParam Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BookmarkResponse> response = bookmarkService.getBookmarks(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "북마크 삭제", description = "북마크를 삭제합니다.")
    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<Void> removeBookmark(@PathVariable Long bookmarkId) {
        bookmarkService.removeBookmark(bookmarkId);
        return ResponseEntity.ok().build();
    }
}
