import { API_INSTANCE } from ".";

const API_URL = "/board";

export const BoardAPI = {
  getBoardTypes: async () => {
    return await API_INSTANCE.GET(`${API_URL}/kind`);
  },
  getRecommendBoards: async () => {
    return await API_INSTANCE.GET(`${API_URL}/recommend`);
  },
};
