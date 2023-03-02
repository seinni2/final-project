package com.project.sparta.communityComment.controller;

import com.project.sparta.alarm.controller.AlarmController;
import com.project.sparta.alarm.dto.AlarmMessageDto;
import com.project.sparta.communityBoard.dto.CommunityBoardOneResponseDto;
import com.project.sparta.communityBoard.service.CommunityBoardServiceImpl;
import com.project.sparta.communityComment.dto.CommunityRequestDto;
import com.project.sparta.communityComment.service.CommunityCommentService;
import com.project.sparta.security.UserDetailsImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.hibernate.mapping.Join;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = {"커뮤니티 보드 댓글 API"})
@RestController
@RequiredArgsConstructor
public class CommunityCommnetController {

    private final CommunityCommentService commentService;

    private final AlarmController alarmController;

    private final CommunityBoardServiceImpl communityBoardService;

    //커뮤니티 댓글 작성
    @ApiOperation(value = "커뮤니티 댓글 작성", response = Join.class)
    @PostMapping("/comments/communities/{boardId}")
    public ResponseEntity createCommunityComment(@PathVariable Long boardId
        , @RequestBody CommunityRequestDto communityRequestDto
        , @AuthenticationPrincipal UserDetailsImpl userDetail) {

        commentService.createCommunityComments(boardId, communityRequestDto, userDetail.getUser());
        CommunityBoardOneResponseDto board = communityBoardService.getBoard(boardId);

        LocalDateTime now = LocalDateTime.now();
        AlarmMessageDto messageDto = AlarmMessageDto.builder()
                                    .title(board.getTitle())
                                    .writerNickName(board.getNickName())
                                    .sendNickName(userDetail.getUser().getNickName())
                                    .boardType("댓글")
                                    .time(now)
                                    .build();

        alarmController.alarmSendMessage("1", messageDto);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    //커뮤니티 댓글 수정
    @ApiOperation(value = "커뮤니티 댓글 수정", response = Join.class)
    @PatchMapping("/communities/comments/{commentId}")
    public ResponseEntity updateCommunityComment(
        @PathVariable Long commentId,
        @RequestBody CommunityRequestDto communityRequestDto,
        @AuthenticationPrincipal UserDetailsImpl userDetail) {
        commentService.updateCommunityComments(commentId, communityRequestDto,
            userDetail.getUser());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //커뮤니티 댓글 삭제
    @ApiOperation(value = "커뮤니티 댓글 삭제", response = Join.class)
    @DeleteMapping("/communities/comments/{commentId}")
    public ResponseEntity deleteCommunityComment(@PathVariable Long commentId,
        @AuthenticationPrincipal UserDetailsImpl userDetail) {
        commentService.deleteCommunityComments(commentId, userDetail.getUser());
        return new ResponseEntity<>(HttpStatus.OK);
    }


}
