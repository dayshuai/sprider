package cn.download.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpMKDownloader {

	// 定义成员变量
	private String path;
	private String targetPath;
	private DownFileThread[] threads;
	private int threadNum;
	private long length;

	public FtpMKDownloader(String path, String targetPath, int threadNum) {
		super();
		this.path = path;
		this.targetPath = targetPath;
		this.threads = new DownFileThread[threadNum];
		this.threadNum = threadNum;
	}

	public void download() {
		URL url;
		try {
			if (path.indexOf("http://") == 0 || path.indexOf("https://") == 0) {
				url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(20 * 1000);
				conn.setRequestMethod("GET");
				conn.setRequestProperty("connection", "keep-alive");
				conn.setRequestProperty("accept", "*/*");

				// 获取远程文件的大小
				length = conn.getContentLengthLong();
				conn.disconnect();
			} else if (path.indexOf("ftp://") == 0) {
				// System.out.println("Comming Soon...2");
				// System.out.println("Comming Soon...");
				String User = "Anonymous";
				String PassWord = "";
				String SerUrl = null;
				ArrayList<String> DrvList = new ArrayList<String>();
				String tempDrvs = "/";
				String fileName = "";
				int port = 21;
				if (path.indexOf("@") != -1) {
					User = path.substring(path.indexOf("//", 0) + 2, path.indexOf(":", path.indexOf("//") + 2));
					PassWord = path.substring(path.indexOf(":", path.indexOf("//")) + 1, path.indexOf("@"));
					SerUrl = path.substring(path.indexOf("@") + 1, path.indexOf("/", path.indexOf("@")));
					if (path.indexOf("/", path.indexOf("@") + 1) < path.lastIndexOf("/")) {
						tempDrvs = path.substring(path.indexOf("/", path.indexOf("@") + 1) + 1, path.lastIndexOf("/"));
					}
				} else {
					SerUrl = path.substring(path.indexOf("//") + 2, path.indexOf("/", path.indexOf("//") + 2));
					if (path.indexOf("/", path.indexOf("//") + 2) < path.lastIndexOf("/")) {
						tempDrvs = path.substring(path.indexOf("/", path.indexOf("//") + 2) + 1, path.lastIndexOf("/"));
					}
				}
				String ftpCurDrv = tempDrvs.replace("/", "\\");

				if (SerUrl.indexOf(":") != -1) {
					port = Integer.parseInt(SerUrl.substring(SerUrl.indexOf(":") + 1));
					SerUrl = SerUrl.substring(0, SerUrl.indexOf(":"));

				}

				fileName = path.substring(path.lastIndexOf("/") + 1);
				FTPClient ftpClient = FTPUtil.ftpConn(SerUrl, port, User, PassWord);
				//
				try {
					ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
					ftpClient.changeWorkingDirectory(ftpCurDrv);
					FTPFile[] ftpFiles = ftpClient.listFiles("\\" + fileName);
					FTPFile ftpFile = ftpFiles[0];
					if (null != ftpFile && ftpFile.isFile()) {
						length = ftpFile.getSize();
					}

				} catch (IOException ex) {
					ex.printStackTrace();

				}
				FTPUtil.ftpClose(ftpClient);
			}

			RandomAccessFile targetFile = new RandomAccessFile(targetPath, "rw");
			targetFile.setLength(length);
			targetFile.close();

			long avgPart = length / threadNum + 1;

			for (int i = 0; i < threadNum; i++) {
				long startPos = avgPart * i;
				RandomAccessFile targetTmp = new RandomAccessFile(targetPath, "rw");
				targetTmp.seek(startPos);
				threads[i] = new DownFileThread(startPos, targetTmp, avgPart);
				threads[i].start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public double getDownRate() {
		long currentSize = 0;
		for (int i = 0; i < threadNum; i++) {
			currentSize += threads[i].length;
		}
		return currentSize * 1.0 / length;
	}

	class DownFileThread extends Thread {
		private long startPos;
		private RandomAccessFile raf;
		private long size;
		private long length;

		public DownFileThread(long startPos, RandomAccessFile raf, long size) {
			super();
			this.startPos = startPos;
			this.raf = raf;
			this.size = size;
		}

		public void run() {
			URL url;
			try {
				InputStream in = null;
				if (path.indexOf("http://") == 0 || path.indexOf("https://") == 0) {
					url = new URL(path);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(120 * 1000);
					conn.setRequestMethod("GET");
					conn.setRequestProperty("connection", "keep-alive");
					conn.setRequestProperty("accept", "*/*");

					in = conn.getInputStream();
				} else if (path.indexOf("ftp://") == 0) {
					// System.out.println("Comming Soon...");
					String User = "Anonymous";
					String PassWord = "";
					String SerUrl = null;
					ArrayList<String> DrvList = new ArrayList<String>();
					String tempDrvs = "/";
					String fileName = "";
					int port = 21;
					if (path.indexOf("@") != -1) {
						User = path.substring(path.indexOf("//", 0) + 2, path.indexOf(":", path.indexOf("//") + 2));
						PassWord = path.substring(path.indexOf(":", path.indexOf("//")) + 1, path.indexOf("@"));
						SerUrl = path.substring(path.indexOf("@") + 1, path.indexOf("/", path.indexOf("@")));
						if (path.indexOf("/", path.indexOf("@") + 1) < path.lastIndexOf("/")) {
							tempDrvs = path.substring(path.indexOf("/", path.indexOf("@") + 1) + 1,
									path.lastIndexOf("/"));
						}
					} else {
						SerUrl = path.substring(path.indexOf("//") + 2, path.indexOf("/", path.indexOf("//") + 2));
						if (path.indexOf("/", path.indexOf("//") + 2) < path.lastIndexOf("/")) {
							tempDrvs = path.substring(path.indexOf("/", path.indexOf("//") + 2) + 1,
									path.lastIndexOf("/"));
						}
					}
					String ftpCurDrv = tempDrvs.replace("/", "\\");

					if (SerUrl.indexOf(":") != -1) {
						port = Integer.parseInt(SerUrl.substring(SerUrl.indexOf(":") + 1));
						SerUrl = SerUrl.substring(0, SerUrl.indexOf(":"));

					}

					fileName = path.substring(path.lastIndexOf("/") + 1);
					// System.out.println(User);
					// System.out.println(PassWord);
					// System.out.println(SerUrl);
					// System.out.println(port);
					// System.out.println(tempDrvs);
					// System.out.println(fileName);
					FTPClient ftpClient = FTPUtil.ftpConn(SerUrl, port, User, PassWord);
					//
					try {
						ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
						ftpClient.changeWorkingDirectory(ftpCurDrv);
						// FTPFile[] ftpFiles=ftpClient.listFiles("\\"+fileName);
						// FTPFile ftpFile=ftpFiles[0];
						// File localFile=new File(targetPath+fileName);
						// OutputStream out=new FileOutputStream(localFile);
						// System.out.println(ftpFile.getName());
						// ftpClient.retrieveFile(ftpFile.getName(), out);
						in = ftpClient.retrieveFileStream(fileName);
						// out.flush();
						// out.close();
						// System.out.println("下载完毕");

					} catch (IOException ex) {
						ex.printStackTrace();

					}
					//

				}

				in.skip(this.startPos);
				byte[] buf = new byte[1024];
				int hasRead = 0;
				while (length < size && (hasRead = in.read(buf)) != -1) {
					raf.write(buf, 0, hasRead);

					length += hasRead;
				}
				raf.close();
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// 下载ftp文件
		// 连接ftp服务器
		/*
		 * public FTPClient ConnectFtpServer(String StrServerUrl,int intPort,String
		 * StrUserName,String StrPassWord,String controlEcoding) { FTPClient
		 * ftpClient=new FTPClient();
		 * 
		 * return ftpClient; }
		 */
	}

	public static void main(String[] args) {

		// thunder转成http
		String thunder;
		String Url = "";
		Scanner input = new Scanner(System.in);
		while (true) {
			System.out.print("输入迅雷下载地址：");
			thunder = input.nextLine();
			if (thunder.equals("exit")) {
				System.exit(0);
			} else if (thunder.indexOf("thunder://") != 0) {
				if (thunder.indexOf("http://") == 0 || thunder.indexOf("ftp://") == 0
						|| thunder.indexOf("https://") == 0) {

					Url = thunder;
					// System.out.println("https is ok"+thunder);
					break;
				} else {
					System.out.println("链接格式错误，请输入thunder://开头的迅雷链接格式");
					continue;
				}
			}

			System.out.println("迅雷地址：" + thunder);
			String base64 = thunder.substring(10);
			System.out.println("Base64:" + base64);
			String str = "";
			try {
				byte[] bytes = Base64.getDecoder().decode(base64);
				str = new String(bytes, "UTF-8");
			} catch (UnsupportedEncodingException | IllegalArgumentException e) {
				System.out.println("BASE64不能解码，请输入正确的格式");
				continue;
			}

			if (str.indexOf("AA") == 0 && str.indexOf("ZZ") == str.length() - 2) {
				Url = str.substring(2, str.length() - 2);
			} else {
				System.out.println("链接未能转化，请输入正确的迅雷链接地址");
				continue;
			}

			// setSysClipboardText(Url);
			System.out.println("URL:" + Url);
			// 目前还不能下载ftp
			/*
			 * if(Url.indexOf("ftp://")==0) { System.out.println("目前不能ftp下载"); continue; }
			 */
			break;
		}
		// end

		// InputStreamReader in = new InputStreamReader(System.in);
		// BufferedReader bufferedReader = new BufferedReader(in);
		// System.out.print("请输入下载地址：");
		// try {
		// input = bufferedReader.readLine(); //读取用户输入
		// } catch (IOException e) {
		// e.printStackTrace();
		// System.out.println(e.toString());
		// }
		// System.out.println(input);

		String path = Url;
		String downLoadPath = "D:\\downLoad\\";
		String targetPath = downLoadPath
				+ Url.substring(Url.lastIndexOf('/') + 1, Url.contains("?") ? Url.lastIndexOf('?') : Url.length());
		final FtpMKDownloader download = new FtpMKDownloader(path, targetPath, 4);
		download.download();

		new Thread(new Runnable() {

			@Override
			public void run() {

				System.out.print("      %");
				while (download.getDownRate() < 1) {

					String strRate = String.format("%.2f", download.getDownRate() * 100);
					System.out.print("\r");
					System.out.print(strRate);

					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.print("\r");
				System.out.println("100.00");
			}

		}).start();

	}

}

class FTPUtil {
	public static FTPClient ftpConn(String SerUrl, int Port, String User, String PassWord) {
		FTPClient ftpClient = new FTPClient();
		// ftpClient.setControlEncoding(controlEncoding);
		try {
			ftpClient.connect(SerUrl, Port);
			ftpClient.login(User, PassWord);
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			ftpClient.setControlEncoding("GBK");
			int reply = ftpClient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				System.out.println("服务器响应失败！正在断开连接。。。");
				ftpClient.abort();
				ftpClient.disconnect();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("Ftp Server连接失败");
		}

		return ftpClient;
	}

	public static FTPClient ftpClose(FTPClient ftpClient) {
		try {
			if (ftpClient != null && ftpClient.isConnected()) {
				ftpClient.abort();
				ftpClient.disconnect();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ftpClient;

	}
}