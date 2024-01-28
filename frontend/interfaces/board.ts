import { User } from "./user";

type OriginalUser = User;
export type RequiredUser = Pick<
  OriginalUser,
  "userId" | "profile" | "nickname"
>;

export default interface Board {
  boardId: number;
  boardType: number;
  user: RequiredUser;
  title: string;
  content: string;
  views: number;
  tags?: string[];
  isSolved?: boolean;
  likeTotal: number;
  isLike: boolean;
  isView: boolean;
  createdTime: string;
}
