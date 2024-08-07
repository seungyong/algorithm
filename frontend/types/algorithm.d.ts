import { ALGORITHM_BOARD_TYPE, PageOptions } from ".";
import { RequestBoard } from "./board";
import { BoardTypeIdValue } from "./constants";

export const SolvedOptionItem = {
  ALL: "a",
  SOLVED: "s",
  NOT_SOLVED: "ns",
} as const;
export type SolvedOptions =
  (typeof SolvedOptionItem)[keyof typeof SolvedOptionItem];

export const SortOptionItem = {
  RECNET: "r",
  OLDER: "or",
  TRIED: "t",
} as const;
export type SortOptions = (typeof SortOptionItem)[keyof typeof SortOptionItem];

export const RateOptionItem = {
  HIGH: "h",
  LOW: "l",
} as const;
export type RateOptions = (typeof RateOptionItem)[keyof typeof RateOptionItem];

export type AlgorithmOptions = {
  count: number;
  page: number;
  solved: SolvedOptions;
  sort: SortOptions;
  level: number;
  rate?: RateOptions;
  tag?: number;
  keyword?: string;
};

export type AlgorithmKind = {
  kindId: string;
  kindName: string;
};

export type RequestAlgorithm = {
  code: string;
  type: string;
};

export type RequestAlgorithmBoard = {
  boardType: Pick<BoardTypeIdValue, 3, 4>;
} & RequestBoard;
