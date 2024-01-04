import { getIronSession } from "iron-session";
import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

import { SessionData, sessionOptions } from "./app/api/sessionConfig";
import { AUTH_PATHS, NO_AUTH_PATHS } from "./constants";

export default async function middleware(req: NextRequest) {
  const session = await getIronSession<SessionData>(cookies(), sessionOptions);
  const pathname = req.nextUrl.pathname;

  const hasSessionButUnAuthPath =
    NO_AUTH_PATHS.includes(pathname) && session.sessionId;
  const noSessionButAuthPath =
    AUTH_PATHS.includes(pathname) && !session.sessionId;

  if (hasSessionButUnAuthPath || noSessionButAuthPath) {
    const url = req.nextUrl.clone();
    url.pathname = "/";
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/", ...NO_AUTH_PATHS, ...AUTH_PATHS],
};
