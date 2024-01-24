import React from "react";

import { notosansBold } from "@/styles/_font";
import SubNavigation, {
  NavItem,
} from "@/components/mypage/activity/subNavigation";
import Content from "@/components/mypage/content";

import { getAlgorithmCounts } from "@/api/algorithm/algorithm";

import styles from "./posts.module.scss";

const navItems: NavItem[] = [
  {
    title: "질문",
    link: "/activity/question",
  },
  {
    title: "피드백",
    link: "/activity/feedback",
  },
  {
    title: "자유",
    link: "/activity/free",
  },
  {
    title: "답변",
    link: "/activity/answer",
  },
  {
    title: "댓글",
    link: "/activity/comment",
  },
  {
    title: "좋아요한 글",
    link: "/activity/favorite",
  },
];

const layout = async ({ children }: { children: React.ReactNode }) => {
  const algorithmCounts = await getAlgorithmCounts();

  return (
    <div className={styles.activity}>
      <Content title="문제">
        <div className={styles.algorithmCount}>
          <div className={styles.item}>
            <div
              className={`${styles.correct} ${styles.number} ${notosansBold.className}`}
            >
              {algorithmCounts.correct}
            </div>
            <div>맞힌 문제</div>
          </div>
          <div className={styles.line}></div>
          <div className={styles.item}>
            <div
              className={`${styles.incorrect} ${styles.number} ${notosansBold.className}`}
            >
              {algorithmCounts.inCorrect}
            </div>
            <div>시도한 문제</div>
          </div>
          <div className={styles.line}></div>
          <div className={styles.item}>
            <div
              className={`${styles.bookmark} ${styles.number} ${notosansBold.className}`}
            >
              {algorithmCounts.bookmark}
            </div>
            <div>찜한 문제</div>
          </div>
        </div>
      </Content>
      <Content title="내 활동 내역">
        <SubNavigation items={navItems} />
        <div className={styles.content}>{children}</div>
      </Content>
    </div>
  );
};

export default layout;
