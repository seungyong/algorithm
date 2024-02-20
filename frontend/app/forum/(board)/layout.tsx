import React from "react";

import BoardNavigation, { NavItem } from "@/components/common/boardNavigation";

import { getUser } from "@/api/user";

const Forum = async ({ children }: { children: React.ReactNode }) => {
  let user;
  try {
    user = (await getUser()).data;
  } catch (error) {
    user = undefined;
  }

  const navItems: NavItem[] = [
    {
      title: "전체",
      link: `/forum/all`,
    },
    {
      title: "질문",
      link: `/forum/question`,
    },
    {
      title: "자유",
      link: `/forum/free`,
    },
  ];

  return (
    <>
      <BoardNavigation isVisiblePost={user !== undefined} items={navItems} />
      {children}
    </>
  );
};

export default Forum;
