"use client";

import React, { useCallback, useEffect, useState } from "react";
import Image from "next/image";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import toast from "react-hot-toast";

import { CommentListItem } from "@/app/api/model/comment";

import { useAuth } from "@/providers/authProvider";

import styles from "./comment.module.scss";

import EditorViewer from "../common/editorViewer";
import CommentUpdateEditor from "./commentUpdateEditor";
import RecommendPost from "./recommendButton";
import Modal from "../common/modal";

import { IMAGE_URL } from "@/api";
import { BoardTypeId } from "@/types/constants";
import { CommentAPI } from "@/api/comment";

type CommentProps = {
  comment: CommentListItem;
  userId: number;
  boardTypeId: string;
  solved?: number | null;
};

const Comment = ({ comment, userId, boardTypeId, solved }: CommentProps) => {
  const param = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const boardId = Array.isArray(param.boardId)
    ? param.boardId[0]
    : param.boardId;

  const [init, setInit] = useState<boolean>(false);
  const [isVisibleModal, setIsVisibleModal] = useState<boolean>(false);
  const [isVisibleEditor, setIsVisibleEditor] = useState<boolean>(false);

  const handleIsVisibleCommentEditor = useCallback(() => {
    setIsVisibleEditor((prev) => !prev);
  }, []);

  const handleISVisibleModal = useCallback(() => {
    setIsVisibleModal((prev) => !prev);
  }, []);

  const handleSolved = useCallback(async () => {
    try {
      await CommentAPI.addSolveComment(boardId, comment.commentId);
      router.refresh();
    } catch (error) {
      toast.error("댓글 채택에 실패하였습니다.\n나중에 다시 시도해주세요.");
    }
  }, [boardId, router, comment.commentId]);

  const handleCommentDelete = useCallback(async () => {
    try {
      await CommentAPI.deleteComment(comment.commentId);
      router.refresh();
      setIsVisibleModal(false);
    } catch (error) {
      toast.error("댓글 삭제에 실패하였습니다.\n나중에 다시 시도해주세요.");
    }
  }, [comment.commentId, router]);

  const handleCommentUpdate = useCallback(
    async (value: string) => {
      try {
        await CommentAPI.updateComment(comment.commentId, { content: value });
        setIsVisibleEditor(false);
        comment.content = value;
      } catch (error) {
        toast.error("댓글 수정에 실패하였습니다.\n나중에 다시 시도해주세요.");
      }
    },
    [comment],
  );

  const renderCheckMark = () => {
    if (
      boardTypeId !== String(BoardTypeId.PUBLIC_FREE) &&
      userId === user?.userId &&
      !solved
    ) {
      return (
        <button onClick={handleSolved}>
          <Image
            src="/svgs/non_check.svg"
            alt="미채택"
            width={32}
            height={32}
          />
        </button>
      );
    } else if (solved === comment.commentId) {
      return (
        <Image src="/svgs/valid_check.svg" alt="채택" width={32} height={32} />
      );
    }
  };

  useEffect(() => {
    if (init) {
      setInit(false);
    }
  }, [init]);

  useEffect(() => {
    if (!isVisibleEditor) {
      setInit(true);
    }
  }, [isVisibleEditor]);

  return (
    <div className={styles.comment}>
      <div className={styles.contentBox}>
        <div className={styles.top}>
          <div className={styles.user}>
            <Link href={`/user/${comment.user.userId}/question`}>
              <Image
                src={`${IMAGE_URL}${comment.user.profile}`}
                alt="프로필 사진"
                width={38}
                height={38}
                className={styles.profileImg}
              />
            </Link>
            <div className={styles.info}>
              <Link href={`/user/${comment.user.userId}/question`}>
                <div>{comment.user.nickname}</div>
              </Link>
              <div className={styles.fs14}>
                <Link href={`/user/${comment.user.userId}/question`}>
                  <span className={styles.createdTime}>
                    {comment.createdTime}
                  </span>
                </Link>
                {comment.user.userId === user?.userId &&
                  comment.commentId !== solved && (
                    <>
                      <button
                        className={styles.blue}
                        onClick={handleIsVisibleCommentEditor}
                      >
                        수정
                      </button>{" "}
                      ·{" "}
                      <button
                        className={styles.red}
                        onClick={handleISVisibleModal}
                      >
                        삭제
                      </button>
                    </>
                  )}
              </div>
            </div>
          </div>
          <div className={styles.recommend}>
            <div className={styles.solved}>{renderCheckMark()}</div>
            <div className={styles.recommendBtnBox}>
              <RecommendPost
                type="comment"
                requestId={comment.commentId}
                isRecommend={comment.isRecommend}
                recommendCount={Number(comment.recommendCount)}
              />
            </div>
          </div>
        </div>
        {isVisibleEditor ? (
          <CommentUpdateEditor
            init={init}
            initialValue={comment.content}
            onSubmit={handleCommentUpdate}
          />
        ) : (
          <EditorViewer content={comment.content} />
        )}
      </div>
      <Modal
        isVisible={isVisibleModal}
        title="정말 삭제하시겠습니까?"
        onOk={handleCommentDelete}
        onCancel={handleISVisibleModal}
        maxWidth={45}
      >
        <p>한 번 삭제하시면 되돌릴 수가 없습니다.</p>
      </Modal>
    </div>
  );
};

export default Comment;
