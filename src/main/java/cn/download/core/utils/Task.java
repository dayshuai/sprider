package cn.download.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class Task extends Thread {

	private Job owner;

	private int index;

	private int readBytes;

	private int startIndex;

	private int endIndex;

	public Task(Job owner, int index) throws IOException {

		this.owner = owner;

		this.index = index;

		if (owner.readOffset(index) != 0) {

			this.readBytes = owner.readOffset(index) - owner.getStartIndexes()[index];

			owner.reportProgress(index, readBytes);

		}

		this.startIndex = owner.getStartIndexes()[index] + readBytes;

		this.endIndex = owner.getEndIndexes()[index];

	}

	public void run() {

		InputStream inputStream = null;

		HttpURLConnection connection = null;

		try {

			if (startIndex > endIndex) {

				owner.taskFinished();

				return;

			}

			connection = owner.createConnection();

			connection.setRequestMethod("GET");

			String range = "bytes=" + startIndex + "-" + endIndex;

			connection.setRequestProperty("Range", range);

			if (connection.getResponseCode() == 206) {

				inputStream = connection.getInputStream();

				int len = -1;

				byte buf[] = new byte[1024];

				int offset = startIndex;

				while ((len = inputStream.read(buf)) != -1) {

					owner.writeLocalFile(offset, buf, 0, len);

					readBytes += len;

					offset += len;

					owner.commitOffset(index, offset);

					owner.reportProgress(index, readBytes);

				}

				owner.taskFinished();

			}

		} catch (IOException e) {

			e.printStackTrace();

			try {

				owner.reStartTask(index);

			} catch (IOException e1) {

				e1.printStackTrace();

			}

		} finally {

			if (inputStream != null) {

				try {

					inputStream.close();

				} catch (IOException e) {

					e.printStackTrace();

				}

			}

			if (connection != null) {

				connection.disconnect();

			}

			try {

				owner.closeTaskResource(index);

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

	}

}