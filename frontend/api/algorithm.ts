import { AlgorithmOptions } from "@/types/algorithm";
import { API_INSTANCE, Body } from ".";
import { PageOptions } from "@/types";

const API_URL = "/algorithm";

export const AlgorithmAPI = {
  getRecommendAlgorithms: async () => {
    return await API_INSTANCE.GET(`${API_URL}/recommend`);
  },
  getAlgorithms: async (options: AlgorithmOptions) => {
    const searchParams = new URLSearchParams();

    Object.entries(options).forEach(([key, value]) => {
      value && searchParams.append(key, String(value));
    });

    return await API_INSTANCE.GET(`${API_URL}?${searchParams.toString()}`);
  },
  getAlgorithmKinds: async () => {
    return await API_INSTANCE.GET(`${API_URL}/kind`);
  },
  getAlgorithm: async (algorithmId: number) => {
    return await API_INSTANCE.GET(`${API_URL}/${algorithmId}`);
  },
  getAlgorithmCorrects: async (
    algorithmId: number,
    options: { language: string } & PageOptions,
  ) => {
    const searchParams = new URLSearchParams();

    Object.entries(options).forEach(([key, value]) => {
      value && searchParams.append(key, String(value));
    });

    return await API_INSTANCE.GET(
      `${API_URL}/${algorithmId}/correct?${searchParams.toString()}`,
    );
  },
  getAlgorithmExplanation: async (algorithmId: number) => {
    return await API_INSTANCE.GET(`${API_URL}/${algorithmId}/explanation`);
  },
  submitAlgorithm: async (algorithmId: number, body: Body) => {
    return await API_INSTANCE.POST(`${API_URL}/${algorithmId}`, body);
  },
  addBookmark: async (algorithmId: number) => {
    return await API_INSTANCE.POST(`${API_URL}/${algorithmId}/recommend`);
  },
  addAlgorithmBoard: async (algorithmId: number, body: Body) => {
    return await API_INSTANCE.POST(`${API_URL}/${algorithmId}/board`, body);
  },
  deleteBookmark: async (algorithmId: number) => {
    return await API_INSTANCE.DELETE(`${API_URL}/${algorithmId}/recommend`);
  },
};
