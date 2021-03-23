package cn.download.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JavaXunlei {

	private static final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

	private static String storeDir = null;

	public static void main(String args[]) throws Exception {

		storeDir = "D://movie";

		int taskNum = 2;

		while (true) {

			String url = "thunder://QUFodHRwOi8vYWlrYW5keS5vcmcv56ul5bm05b6A5LqLLlRoZS5UaW1lLnRvLkxpdmUuYW5kLnRoZS5UaW1lLnRvLkRpZS4xOTg1LkRWRDkuTWluaVNELVRMRi5ta3Y/ZmlkPURMTHkwdzFGMVAwZEY0bFVxQ1dtbVRuOEg2TUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUEmbWlkPTY2NiZ0aHJlc2hvbGQ9MTUwJnRpZD1FNTFERDdCQzAxQTkyODJCQTc3RUMxQzNCRUIzQ0JGNyZzcmNpZD0xMjAmdmVybm89MVpa";

			Job job = new Job(url, storeDir, taskNum);

			job.startJob();

		}

	}

	private static int getIntInput(String message) throws IOException {

		String number = getInput(message);

		while (!number.matches("\\d+")) {

			System.out.println("线程数必须是1个整数");

			number = getInput(message);

		}

		return Integer.parseInt(number);

	}

	private static String getInput(String message) throws IOException {

		System.out.print(message);

		String line = in.readLine();

		while (line == null || line.trim().length() < 1) {

			System.out.print(message);

			line = in.readLine();

		}

		return line.trim();

	}

}