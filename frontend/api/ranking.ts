import Ranking from "@/types/ranking";

export interface ResponseRanking {
  rankings: Ranking[];
  total: number;
}

interface PageOptions {
  count: number;
  page: number;
  keyword?: string;
}

export const getRankings = async (
  options: PageOptions,
): Promise<ResponseRanking> => {
  return {
    rankings: [
      {
        rankingId: 1,
        user: {
          userId: 1,
          profile:
            "data:image/jpeg;base64,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",
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 1,
      },
      {
        rankingId: 2,
        user: {
          userId: 1,
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 2,
      },
      {
        rankingId: 3,
        user: {
          userId: 1,
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 3,
      },
      {
        rankingId: 4,
        user: {
          userId: 1,
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 4,
      },
      {
        rankingId: 5,
        user: {
          userId: 1,
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 5,
      },
      {
        rankingId: 6,
        user: {
          userId: 1,
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 6,
      },
      {
        rankingId: 7,
        user: {
          userId: 1,
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 7,
      },
      {
        rankingId: 8,
        user: {
          userId: 1,
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 8,
      },
      {
        rankingId: 9,
        user: {
          userId: 1,
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 9,
      },
      {
        rankingId: 10,
        user: {
          userId: 1,
          nickname: "황야의고라니",
        },
        tried: 11111,
        solved: 4423,
        rank: 10,
      },
    ],
    total: 78,
  };
};
