package cn.download.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Job {

	private int fileSize;

	private String fileName;

	private int connectTimeout = 10000;

	private int readTimeout = 20000;

	private String url;

	private String storeDir;

	private int taskNum;

	private String jobId;

	private int[] startIndexes;

	private int[] endIndexes;

	private int[] progress;

	private Task[] tasks;

	private File storeDirFile;

	private File dtDirFile;

	private File localFile;

	private ThreadLocal<RandomAccessFile> rafLocalTl;

	private ThreadLocal<RandomAccessFile> rafOffsetTl;

	private CountDownLatch latch;

	private ProgressThread pt;

	public Job(String url, String storeDir, int taskNum) throws IOException {

		this.url = url;

		this.storeDir = storeDir;

		this.taskNum = taskNum;

		this.startIndexes = new int[taskNum];

		this.endIndexes = new int[taskNum];

		this.progress = new int[taskNum];

		this.tasks = new Task[taskNum];

		this.latch = new CountDownLatch(taskNum);

		this.jobId = Math.abs(url.hashCode()) + "_" + taskNum;

		this.rafLocalTl = new ThreadLocal<RandomAccessFile>();

		this.rafOffsetTl = new ThreadLocal<RandomAccessFile>();

		this.pt = new ProgressThread();

	}

	public void startJob() throws Exception {

		long start = System.currentTimeMillis();

		System.out.println("开始下载文件...");

		boolean j = fetchFileMetaInfo();

		if (j) {

			assignTasks();

			createFiles();

			startTasks();

			openProgressThread();

			waitForCompeletion();

			long end = System.currentTimeMillis();

			System.out.println("下载完毕,全程耗时" + (end - start) + "ms");

		} else {

			System.out.println("获取文件长度或文件名失败，请重试");

		}

	}

	private void openProgressThread() {

		this.pt.start();

	}

	private void waitForCompeletion() throws Exception {

		latch.await();

		deleteFiles();

		pt.join();

	}

	private void deleteFiles() {

		if (dtDirFile != null) {

			File[] subFiles = dtDirFile.listFiles();

			for (File subFile : subFiles) {

				subFile.delete();

			}

			dtDirFile.delete();

		}

	}

// 1.fetch file size and file name

	private boolean fetchFileMetaInfo() throws IOException {

		HttpURLConnection connection = createConnection();

		connection.setRequestMethod("GET");

		if (connection.getResponseCode() == 200) {

			this.fileSize = connection.getContentLength();

			String disposition = connection.getHeaderField("Content-Disposition");

			if (disposition == null) {

				parseFileNameFromUrl(url);

			} else {

				parseFileNameFromDisposition(disposition);

			}

			if (this.fileName == null || this.fileSize < 0) {

				return false;

			}

			System.out.println("找到文件资源，长度为" + fileSize + ",资源名称为" + fileName);

			return true;

		}

		return false;

	}

	private void parseFileNameFromUrl(String url) throws UnsupportedEncodingException {

		this.fileName = url.substring(url.lastIndexOf("/") + 1, url.length());

		if (this.fileName.contains("%")) {

			this.fileName = URLDecoder.decode(this.fileName, "UTF-8");

		}

	}

	private void parseFileNameFromDisposition(String disposition) throws UnsupportedEncodingException {

		Pattern pattern = Pattern.compile(".+filename=\"(.+?)\".*");

		Matcher matcher = pattern.matcher(disposition);

		if (matcher.matches()) {

			this.fileName = new String(matcher.group(1).getBytes("ISO-8859-1"), "UTF-8");

		} else {

			parseFileNameFromUrl(url);

		}

	}

	public HttpURLConnection createConnection() throws IOException {

		URL urlObj = new URL(url);

		HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

		connection.setConnectTimeout(connectTimeout);

		connection.setReadTimeout(readTimeout);

		connection.setRequestProperty("Accept-Charset", "UTF-8");

		connection.setRequestProperty("contentType", "UTF-8");

		return connection;

	}

// 2.assign every task start index and end index out of the file

	private void assignTasks() throws IOException {

		for (int i = 0; i < taskNum; i++) {

			int size = fileSize / taskNum;

			int startIndex = i * size;

			int endIndex = i == taskNum - 1 ? fileSize - 1 : i * size + size - 1;

			this.startIndexes[i] = startIndex;

			this.endIndexes[i] = endIndex;

		}

	}

// 3.create the local file and temp directory

	private void createFiles() throws IOException {

		storeDirFile = new File(storeDir);

		storeDirFile.mkdirs();

		localFile = new File(storeDirFile, fileName);

		dtDirFile = new File(storeDirFile, "." + jobId);

		dtDirFile.mkdirs();

		if (!localFile.exists()) {

			RandomAccessFile raf = new RandomAccessFile(localFile, "rw");

			raf.setLength(fileSize);

			raf.close();

		}

	}

// 4.let the task start to do their work

	private void startTasks() throws IOException {

		for (int i = 0; i < taskNum; i++) {

			Task task = new Task(this, i);

			tasks[i] = task;

			task.start();

		}

	}

	private int totalReadBytes() {

		int totalReadBytes = 0;

		for (int i = 0; i < progress.length; i++) {

			totalReadBytes += progress[i];

		}

		return totalReadBytes;

	}

	public int[] getStartIndexes() {

		return startIndexes;

	}

	public int[] getEndIndexes() {

		return endIndexes;

	}

	public void writeLocalFile(int startIndex, byte[] buf, int off, int len) throws IOException {

		if (rafLocalTl.get() == null) {

			RandomAccessFile raf = new RandomAccessFile(localFile, "rw");

			rafLocalTl.set(raf);

		}

		RandomAccessFile raf = rafLocalTl.get();

		raf.seek(startIndex);

		raf.write(buf, off, len);

	}

// 5.let task to report their progress

	public void reportProgress(int index, int readBytes) {

		progress[index] = readBytes;

	}

	public void closeTaskResource(int index) throws IOException {

		RandomAccessFile raf = rafLocalTl.get();

		if (raf != null) {

			raf.close();

		}

		raf = rafOffsetTl.get();

		if (raf != null) {

			raf.close();

		}

	}

	public void commitOffset(int index, int offset) throws IOException {

		File offsetFile = new File(dtDirFile, String.valueOf(index));

		if (rafOffsetTl.get() == null) {

			RandomAccessFile raf = new RandomAccessFile(offsetFile, "rw");

			rafOffsetTl.set(raf);

		}

		RandomAccessFile raf = rafOffsetTl.get();

		raf.seek(0);

		raf.writeInt(offset);

	}

	public int readOffset(int index) throws IOException {

		File offsetFile = new File(dtDirFile, String.valueOf(index));

		if (offsetFile.exists()) {

			RandomAccessFile raf = new RandomAccessFile(offsetFile, "rw");

			raf.seek(0);

			int offset = raf.readInt();

			raf.close();

			return offset;

		}

		return 0;

	}

	public void reStartTask(int index) throws IOException {

		Task task = new Task(this, index);

		tasks[index] = task;

		task.start();

		System.out.println("任务" + index + "发生错误，重新调度该任务");

	}

	public void taskFinished() {

		latch.countDown();

	}

	private class ProgressThread extends Thread {

		private DecimalFormat decimalFormat = new DecimalFormat();

		public void run() {

			decimalFormat.applyPattern("0.0");

			while (true) {

				try {

					int endPointX = totalReadBytes();

					TimeUnit.SECONDS.sleep(1);

					int endPointY = totalReadBytes();

					int waitSeconds = 1;

					while (endPointY - endPointX == 0) {

						TimeUnit.SECONDS.sleep(1);

						waitSeconds++;

						endPointY = totalReadBytes();

					}

					int speed = (endPointY - endPointX) / waitSeconds;

					String speedStr = speed > 1024 ? speed / 1024 + "kb/s" : speed + "b/s";

					String percent = decimalFormat.format(endPointY * 100.0 / fileSize);

					int remainSeconds = (fileSize - endPointY) / speed;

					System.out.println("下载完成" + percent + "%,速度" + speedStr + ",估计还需要" + remainSeconds + "秒");

					if ("100.0".equals(percent)) {

						break;

					}

				} catch (InterruptedException e) {

					e.printStackTrace();

				}

			}

		}

	}

}
