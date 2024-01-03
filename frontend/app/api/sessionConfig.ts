import { SessionOptions } from "iron-session";
import { TriggerWithArgs, TriggerWithoutArgs } from "swr/mutation";

export interface SessionData {
  sessionId: string | undefined;
}

export interface SessionResponse {
  session: SessionData;
  logout: TriggerWithoutArgs<SessionData, any, "/api/auth", never>;
  login: TriggerWithArgs<SessionData, any, "/api/auth", string>;
  isLoading: false;
}

export const sessionOptions: SessionOptions = {
  password: process.env.SESSION_PW!.toString(),
  cookieName: "sessionId",
  cookieOptions: {
    secure: process.env.ENVIROMENT === "product" ? true : false,
  },
};
