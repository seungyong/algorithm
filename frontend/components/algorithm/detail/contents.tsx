"use client";

import React, { useCallback, useEffect, useState } from "react";
import { Extension, ReactCodeMirrorProps } from "@uiw/react-codemirror";
import { cpp } from "@codemirror/lang-cpp";
import { python } from "@codemirror/lang-python";
import { java } from "@codemirror/lang-java";
import { useTheme } from "next-themes";
import Link from "next/link";
import { Resizable } from "re-resizable";
import dynamic from "next/dynamic";
import toast from "react-hot-toast";
import { useRouter } from "next/navigation";

import { CodeType, CodeValue } from "@/types/constants";
import { RequestAlgorithm } from "@/types/algorithm";

import {
  Algorithm,
  AlgorithmResult,
  AlgorithmResultItem,
} from "@/app/api/model/algorithm";

import { notosansBold, notosansMedium } from "@/styles/_font";
import styles from "./contents.module.scss";

import { useCodeType } from "@/providers/codeTypeProvider";

import Modal from "@/components/common/modal";
import EditorViewer from "@/components/common/editorViewer";
import { AlgorithmAPI } from "@/api/algorithm";

type DetailProps = {
  algorithm: Algorithm;
};

const contentDefaultSize = {
  width: "40%",
  height: "100%",
  minWidth: "30%",
  maxWidth: "70%",
};

const codeDefaultSize = {
  width: "100%",
  height: "75%",
  minHeight: "50%",
  maxHeight: "85%",
};

const DynamicCodeMirror = dynamic(() => import("@uiw/react-codemirror"), {
  ssr: false,
});

const defaultCode = (type?: CodeType) => {
  if (type === "cpp") {
    return `int main() {\n${"  "}\n${"  "}return 0;\n}`;
  } else if (type === "j") {
    return `public class Main {\n${"  "}public static void main(String[] args) {\n${"    "}\n${"  "}}\n}`;
  }

  return "";
};

const Contents = ({ algorithm }: DetailProps) => {
  const router = useRouter();
  const { type } = useCodeType();
  const { resolvedTheme } = useTheme();

  const [language, setLanguage] = useState<Extension>(python);
  const [code, setCode] = useState<string>(defaultCode(type));
  const [isVisibleContentResize, setIsVisibleContentResize] =
    useState<boolean>(true);
  const [isVisibleCodeResize, setIsVisibleCodeResize] = useState<boolean>(true);
  const [isVisibleTestcase, setIsVisibleTestcase] = useState<boolean>(false);
  const [isVisibleResetModal, setIsVisibleResetModal] =
    useState<boolean>(false);
  const [results, setResults] = useState<AlgorithmResult>();

  const handleChangeCode = useCallback((val: string) => {
    setCode(val);
  }, []);

  const handleResize = useCallback(() => {
    const windowWidth = window.innerWidth;

    if (windowWidth <= 767) {
      setIsVisibleContentResize(false);
      setIsVisibleCodeResize(false);
    } else {
      setIsVisibleContentResize(true);
      setIsVisibleCodeResize(true);
    }
  }, []);

  const handleResetOk = useCallback(() => {
    const initalCode = defaultCode(type);
    setCode(initalCode);
    setIsVisibleResetModal(false);
  }, [type]);

  const handleReset = useCallback(() => {
    setIsVisibleResetModal((prev) => !prev);
  }, []);

  const handleSubmit = useCallback(async () => {
    if (code.trim() === "") {
      toast.error("코드를 작성해주세요.");
      return;
    }

    const options: RequestAlgorithm = {
      code,
      type: CodeValue[type],
    };

    try {
      const response = await AlgorithmAPI.submitAlgorithm(
        algorithm.algorithmId,
        options,
      );
      // results 배열에 담기면 AlgorithmResult을 쓸 것.
      const result: AlgorithmResultItem = await response.json();

      if (result.isSuccess) {
        toast.success("정답입니다 !!");
      } else {
        toast.error("정답을 맞추지 못했습니다 :(");
      }

      setResults({
        results: [result],
      });
      router.refresh();
    } catch (error: any) {
      if (error.status === 401) {
        toast.error("로그인이 필요한 서비스입니다.");
        router.push(
          `/login?error=unauthorized&redirect_url=/algorithm/${algorithm.algorithmId}`,
        );
      } else {
        toast.error("정답을 맞추지 못했습니다 :(");
      }
    }
  }, [algorithm.algorithmId, code, router, type]);

  useEffect(() => {
    const initalCode = defaultCode(type);
    setCode(initalCode);

    if (type === "p") {
      setLanguage(python);
    } else if (type === "cpp") {
      setLanguage(cpp);
    } else if (type === "j") {
      setLanguage(java);
    }
  }, [type]);

  useEffect(() => {
    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, [handleResize]);

  const handleTestcaseModal = useCallback(() => {
    setIsVisibleTestcase((prev) => !prev);
  }, []);

  useEffect(() => {
    handleResize();
  }, [handleResize]);

  return (
    <>
      <div className={styles.content}>
        {isVisibleContentResize ? (
          <>
            <Resizable
              defaultSize={contentDefaultSize}
              minWidth={contentDefaultSize.minWidth}
              maxWidth={contentDefaultSize.maxWidth}
              enable={{
                top: false,
                right: true,
                bottom: false,
                left: false,
                topLeft: false,
                bottomLeft: false,
                bottomRight: false,
              }}
              handleClasses={{ right: styles.rightLine }}
            >
              <div className={styles.description}>
                <h4 className={`${styles.title} ${notosansBold.className}`}>
                  문제 설명
                </h4>
                <div className={styles.viewer}>
                  <EditorViewer
                    className={styles.qlEditor}
                    content={algorithm.content}
                  />
                </div>
              </div>
            </Resizable>
            <div className={styles.right}>
              <Resizable
                defaultSize={codeDefaultSize}
                minHeight={codeDefaultSize.minHeight}
                maxHeight={codeDefaultSize.maxHeight}
                enable={{
                  top: false,
                  right: false,
                  bottom: true,
                  left: false,
                  topLeft: false,
                  bottomLeft: false,
                  bottomRight: false,
                }}
                handleClasses={{ bottom: styles.bottomLine }}
              >
                <div className={styles.codeBox}>
                  <h4 className={styles.title}>코드창</h4>
                  <div className={styles.code}>
                    <DynamicCodeMirror
                      value={code}
                      placeholder="코드를 작성해주세요."
                      extensions={[language]}
                      onChange={handleChangeCode}
                      theme={resolvedTheme as ReactCodeMirrorProps["theme"]}
                      basicSetup={{
                        lineNumbers: true,
                        autocompletion: true,
                      }}
                    />
                  </div>
                </div>
              </Resizable>
              <div className={styles.run}>
                <div className={styles.runTitle}>
                  <h4 className={notosansBold.className}>실행결과</h4>
                  <div className={styles.limit}>
                    <span className={styles.mr15}>
                      제한시간 {algorithm.limitTime}초
                    </span>
                    <span>메모리 제한 {algorithm.limitMemory}MB</span>
                  </div>
                </div>
                <div className={styles.testcase}>
                  {results?.results.map((result, idx) => (
                    <div key={idx} className={styles.resultItem}>
                      <div
                        className={`${styles.text} ${notosansBold.className}
                              ${
                                result.isSuccess
                                  ? styles.success
                                  : styles.failed
                              }
                            `}
                      >
                        {result.isSuccess
                          ? idx + 1 + "차 성공"
                          : idx + 1 + "차 실패"}
                      </div>
                      <div className={styles.m5}>-</div>
                      <div> {result.useTime}ms</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </>
        ) : (
          <>
            <div className={styles.description}>
              <h4 className={`${styles.title} ${notosansBold.className}`}>
                문제 설명
              </h4>
              <div className={styles.viewer}>
                <EditorViewer
                  className={styles.qlEditor}
                  content={algorithm.content}
                />
              </div>
            </div>
            <div className={styles.right}>
              {isVisibleCodeResize ? (
                <>
                  <Resizable
                    defaultSize={codeDefaultSize}
                    minHeight={codeDefaultSize.minHeight}
                    maxHeight={codeDefaultSize.maxHeight}
                    enable={{
                      top: false,
                      right: false,
                      bottom: true,
                      left: false,
                      topLeft: false,
                      bottomLeft: false,
                      bottomRight: false,
                    }}
                    handleClasses={{ right: styles.bottomLine }}
                  >
                    <div className={styles.codeBox}>
                      <h4 className={styles.title}>코드창</h4>
                      <div className={styles.code}>
                        <DynamicCodeMirror
                          value={code}
                          placeholder="코드를 작성해주세요."
                          extensions={[language]}
                          onChange={handleChangeCode}
                          theme={resolvedTheme as ReactCodeMirrorProps["theme"]}
                          basicSetup={{
                            lineNumbers: true,
                            autocompletion: true,
                          }}
                        />
                      </div>
                    </div>
                  </Resizable>
                  <div className={styles.run}>
                    <div className={styles.runTitle}>
                      <h4 className={notosansBold.className}>실행결과</h4>
                      <div className={styles.limit}>
                        <span className={styles.mr15}>
                          제한시간 {algorithm.limitTime}초
                        </span>
                        <span>메모리 제한 {algorithm.limitMemory}MB</span>
                      </div>
                    </div>
                    <div className={styles.testcase}>
                      {results?.results.map((result, idx) => (
                        <div key={idx} className={styles.resultItem}>
                          <div
                            className={`${styles.text} ${notosansBold.className}
                              ${
                                result.isSuccess
                                  ? styles.success
                                  : styles.failed
                              }
                            `}
                          >
                            {result.isSuccess
                              ? idx + 1 + "차 성공"
                              : idx + 1 + "차 실패"}
                          </div>
                          <div className={styles.m5}>-</div>
                          <div> {result.useTime}ms</div>
                        </div>
                      ))}
                    </div>
                  </div>
                </>
              ) : (
                <>
                  <div className={styles.codeBox}>
                    <h4 className={styles.title}>코드창</h4>
                    <div className={styles.code}>
                      <DynamicCodeMirror
                        value={code}
                        placeholder="코드를 작성해주세요."
                        extensions={[language]}
                        onChange={handleChangeCode}
                        theme={resolvedTheme as ReactCodeMirrorProps["theme"]}
                        basicSetup={{
                          lineNumbers: true,
                          autocompletion: true,
                        }}
                      />
                    </div>
                  </div>
                  <div className={styles.run}>
                    <div className={styles.runTitle}>
                      <h4 className={notosansBold.className}>실행결과</h4>
                      <div className={styles.limit}>
                        <span className={styles.mr15}>
                          제한시간 {algorithm.limitTime}초
                        </span>
                        <span>메모리 제한 {algorithm.limitMemory}MB</span>
                      </div>
                    </div>
                    <div className={styles.testcase}>
                      {results?.results.map((result, idx) => (
                        <div key={idx} className={styles.resultItem}>
                          <div
                            className={`${styles.text} ${notosansBold.className}
                              ${
                                result.isSuccess
                                  ? styles.success
                                  : styles.failed
                              }
                            `}
                          >
                            {result.isSuccess
                              ? idx + 1 + "차 성공"
                              : idx + 1 + "차 실패"}
                          </div>
                          <div className={styles.m5}>-</div>
                          <div> {result.useTime}ms</div>
                        </div>
                      ))}
                    </div>
                  </div>
                </>
              )}
            </div>
          </>
        )}
      </div>
      <footer className={styles.footer}>
        <button className={styles.btn} onClick={handleTestcaseModal}>
          테스트 케이스
        </button>
        <Link href={`/algorithm/${algorithm.algorithmId}/all?page=1&count=10`}>
          <button className={styles.btn}>질문 및 피드백</button>
        </Link>
        <div className={styles.blank}></div>
        <Link
          href={`/algorithm/${algorithm.algorithmId}/other-answers?language=${CodeValue[type]}`}
        >
          <button className={styles.btn}>다른 사람 풀이 보기</button>
        </Link>
        <Link href={`/algorithm/${algorithm.algorithmId}/explain`}>
          <button className={styles.btn}>해설보기</button>
        </Link>
        <button className={styles.btn} onClick={handleReset}>
          초기화
        </button>
        <button
          className={`${styles.btn} ${styles.submit} ${notosansMedium.className}`}
          onClick={handleSubmit}
        >
          제출
        </button>
      </footer>
      <Modal
        isVisible={isVisibleTestcase}
        title="테스트 케이스"
        onOk={handleTestcaseModal}
        onCancel={handleTestcaseModal}
      >
        <div className={styles.table}>
          <div className={`${styles.th} ${styles.padding}`}>입력</div>
          <div className={`${styles.th} ${styles.padding}`}>출력</div>
          {algorithm.testcases &&
            algorithm.testcases.map((value, idx) => (
              <React.Fragment key={idx}>
                <div className={styles.padding}>{value.input}</div>
                <div className={styles.padding}>{value.output}</div>
              </React.Fragment>
            ))}
        </div>
      </Modal>
      <Modal
        isVisible={isVisibleResetModal}
        title="정말 초기화 하시겠습니까?"
        onOk={handleResetOk}
        onCancel={handleReset}
        maxWidth={45}
      >
        <p>초기화 시 코드를 되돌릴 수 없습니다.</p>
      </Modal>
    </>
  );
};

export default Contents;
