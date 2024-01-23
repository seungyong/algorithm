"use client";

import React, {
  ReactNode,
  createContext,
  useState,
  useContext,
  useEffect,
} from "react";

import { KindOptions } from "@/api/algorithm";

export type CodeType = Exclude<KindOptions, "a">;
export const CODE_TYPE_OPTIONS_ARRAY: CodeType[] = ["c", "p", "j"];

type CodeTypeProviderContext = {
  type: CodeType;
  setType: (value: CodeType) => void;
};

const CodeTypeContext = createContext<CodeTypeProviderContext>({
  type: "p",
  setType: () => {},
});

export const checkMyType = (compareArray: CodeType[], value: CodeType) => {
  return compareArray.includes(value);
};

export const CodeTypeProvider = ({ children }: { children: ReactNode }) => {
  const [type, setStateType] = useState<CodeType>("p");

  useEffect(() => {
    const codeType = window.localStorage.getItem("codeType") || "p";

    setStateType(
      checkMyType(CODE_TYPE_OPTIONS_ARRAY, codeType as CodeType)
        ? (codeType as CodeType)
        : "p",
    );
  }, [type]);

  const setType = (value: CodeType) => {
    window.localStorage.setItem("codeType", value);
    setStateType(value);
  };

  return (
    <CodeTypeContext.Provider value={{ type, setType }}>
      {children}
    </CodeTypeContext.Provider>
  );
};

export const useCodeType = () => {
  const context = useContext(CodeTypeContext);
  return context;
};