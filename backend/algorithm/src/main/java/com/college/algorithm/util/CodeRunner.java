package com.college.algorithm.util;

import com.college.algorithm.dto.RequestCodeDto;
import com.college.algorithm.dto.ResponseCodeDto;
import com.college.algorithm.exception.CustomException;
import com.college.algorithm.exception.ErrorCode;
import com.college.algorithm.entity.AlgorithmTestcase;
import com.college.algorithm.repository.AlgorithmTestcaseRepository;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CodeRunner {

    public final AlgorithmTestcaseRepository testcaseRepository;

    public CodeRunner(AlgorithmTestcaseRepository testcaseRepository) {
        this.testcaseRepository = testcaseRepository;
    }

    public ResponseCodeDto runCpp(RequestCodeDto requestCodeDto, Long algorithmId) throws IOException, InterruptedException {

        List<AlgorithmTestcase> testcaseEntities = testcaseRepository.findAlgorithmTestcasesByAlgorithm_AlgorithmId(algorithmId);

// Java에서 C++ 코드 실행
        try {
            Boolean flag = true;
            double excuteTime = 0.0;
            for(AlgorithmTestcase testcase : testcaseEntities) {
                String[] inputs = testcase.getInput().split(",");
                String[] expectedOutput = testcase.getOutput().split(",");

                // 외부 프로세스로 코드를 컴파일하고 실행
                Process compileProcess = Runtime.getRuntime().exec("g++ -o program -x c++ -");
                if(!compileProcess.isAlive()){
                    throw new CustomException(ErrorCode.ERROR_PREV_PROCESS_ALIVE); // 50041
                }
                compileProcess.getOutputStream().write(requestCodeDto.getCode().getBytes());
                compileProcess.getOutputStream().close();

                // 컴파일 결과 확인
                int compileResult = compileProcess.waitFor();
                if (compileResult != 0) {
                    throw new CustomException(ErrorCode.ERROR_EXTERN_COMPILE); // 컴파일이 오류
                }

                // 생성된 실행 파일 실행하여 결과값 확인
                double startTime = System.currentTimeMillis();
                Process executeProcess = Runtime.getRuntime().exec("./program");

                // 입력값 제공
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(executeProcess.getOutputStream()));
                for (String input : inputs) {
                    writer.write(input.trim() + "\n");
                }
                writer.close();
                double endTime = System.currentTimeMillis();

                excuteTime = ( endTime - startTime )/ 1000.0;

                // 결과값 수집
                BufferedReader reader = new BufferedReader(new InputStreamReader(executeProcess.getInputStream()));
                String line;
                List<String> results = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    results.add(line);
                }

                // 결과값 비교
                if (!results.equals(Arrays.asList(expectedOutput)))
                    flag = false;
            }
                return new ResponseCodeDto(flag, excuteTime);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.ERROR_CODE_RUNNER);
        }
    }

    public ResponseCodeDto runPython(RequestCodeDto requestCodeDto, Long algorithmId){
        List<AlgorithmTestcase> testcaseEntities = testcaseRepository.findTestcaseEntitiesByAlgorithmAlgorithmId(algorithmId);
        // Java에서 python 코드 실행
        try {
            Boolean flag = true;
            double excuteTime = 0.0;
            for(AlgorithmTestcase testcase : testcaseEntities) {
                String[] inputs = testcase.getInput().split(",");
                String[] expectedOutput = testcase.getOutput().split(",");

                List<String> command = new ArrayList<>();
                command.add("python3");
                command.add("-c");
                command.add(requestCodeDto.getCode());
                command.addAll(List.of(inputs));  // 모든 추가 인자들을 명령 목록에 추가

                ProcessBuilder pb = new ProcessBuilder(command);

                double startTime = System.currentTimeMillis();
                Process process = pb.start();
                double endTime = System.currentTimeMillis();

                excuteTime = ( endTime - startTime ) / 1000.0;
                // 파이썬 프로세스의 출력을 읽음
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                List<String> results = new ArrayList<>();
                while ((line = br.readLine()) != null) {
                    results.add(line);
                }

                if(results.size() <= 0){
                    throw new CustomException(ErrorCode.ERROR_EMPTY_RESULT);
                }

                // 결과값 비교
                if (!results.equals(Arrays.asList(expectedOutput)))
                    flag = false;
                // 프로세스가 종료될 때까지 대기
                int exitCode = process.waitFor();
            }
                return new ResponseCodeDto(flag, excuteTime);
        }catch(Exception e) {
            throw new CustomException(ErrorCode.ERROR_CODE_RUNNER);
        }
    }

    public ResponseCodeDto runJava(RequestCodeDto requestCodeDto, Long algorithmId){
        List<AlgorithmTestcase> testcaseEntities = testcaseRepository.findAlgorithmTestcasesByAlgorithm_AlgorithmId(algorithmId);
        // Java에서 python 코드 실행
        try {
            Boolean flag = true;
            double excuteTime = 0.0;
            for(AlgorithmTestcase testcase : testcaseEntities) {
                String[] inputs = testcase.getInput().split(",");
                String[] expectedOutput = testcase.getOutput().split(",");

                // 소스 파일 작성
                new File("Main.class").delete();
                File sourceFile = new File("Main.java");
                FileWriter writer = new FileWriter(sourceFile);
                writer.write(requestCodeDto.getCode());
                writer.close();

                // javac로 컴파일
                ProcessBuilder pbCompile = new ProcessBuilder("javac", "Main.java");
                Process processCompile = pbCompile.start();
                processCompile.waitFor();

                // java로 실행
                ProcessBuilder pbRun = new ProcessBuilder("java", "Main");
                pbRun.redirectErrorStream(true); // 표준 에러를 표준 출력으로 리디렉션

                double startTime = System.currentTimeMillis();
                Process processRun = pbRun.start();


                // 입력값 제공
                BufferedWriter inputWriter = new BufferedWriter(new OutputStreamWriter(processRun.getOutputStream()));
                for (String input : inputs) {
                    inputWriter.write(input.trim() + "\n");
                }
                inputWriter.close(); // [ 3  5 ]   ->   [ 8 ]
                double endTime = System.currentTimeMillis();

                excuteTime = ( endTime - startTime )/ 1000.0;

                // 실행 결과 출력
                BufferedReader reader = new BufferedReader(new InputStreamReader(processRun.getInputStream()));
                String line;
                List<String> results = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    results.add(line);
                }

                // 임시 파일 삭제
                sourceFile.delete();
                new File("Main.class").delete();


                // 결과값 비교
                if (!results.equals(Arrays.asList(expectedOutput)))
                    flag = false;

            }
            return new ResponseCodeDto(flag, excuteTime);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.ERROR_CODE_RUNNER);
        }
    }

}
