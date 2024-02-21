import { Testcase } from "./testcase";
import { RequiredUser } from "./user";

export type Algorithm = {
  algorithmId: number;
  user: RequiredUser;
  title: string;
  level: string;
  algorithmKind: number;
  algorithmCompe: number;
  limitTime: string;
  limitMem: string;
  createdTime: string;
  content: string;
  thumbnail?: string;
  kinds: string[];
  testcase: Testcase[];
  isFavorite?: boolean;
  solved?: boolean;
  correctRate: number;
};

export type AlgorithmResponse = {
  algorithms: Algorithm[];
  total: number;
};

export type SolvedOptions = "a" | "s" | "ns";
export type SortOptions = "r" | "or" | "t";
export type KindOptions = "a" | "c" | "p" | "j";
export type RateOptions = "h" | "l";

export type AlgorithmOptions = {
  count: number;
  page: number;
  solved: SolvedOptions;
  sort: SortOptions;
  level: number;
  kind: KindOptions;
  rate?: RateOptions;
  tag?: number;
  keyword?: string;
};

export type AlgorithmKind = {
  kindId: string;
  kindName: string;
};
