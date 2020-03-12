package net.zhongfu.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * 命令处理
 * @author 拿来主义
 *
 */
public class ProcessUtils {

	/**
	 * 默认字符集
	 */
	private static String DEFAULT_CHARSET_NAME = "UTF-8";
	static {
		String osName = System.getProperty("os.name");
		if (osName.toLowerCase().indexOf("windows") > -1) {
			DEFAULT_CHARSET_NAME="GBK";
		}
	}

	/**
	 * 创建进程并执行指令返回结果
	 *
	 * @param commend 子进程执行的命令
	 * @return
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static Result run(File bsdir,String commend) throws IOException, InterruptedException {

		return run(bsdir,commend, DEFAULT_CHARSET_NAME);
	}

	/**
	 * 创建进程并执行指令返回结果
	 *
	 * @param commend     子进程执行的命令
	 * @param charsetName 字符集
	 * @return
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static Result run(File bsdir,String commend, String charsetName) throws IOException, InterruptedException {
		StringTokenizer st = new StringTokenizer(commend);
		String[] commendArray = new String[st.countTokens()];
		for (int i = 0; st.hasMoreTokens(); i++) {
			commendArray[i] = st.nextToken();
		}
		return run(bsdir,Arrays.asList(commendArray), charsetName);
	}

	/**
	 * 创建进程并执行指令返回结果
	 *
	 * @param commend 子进程执行的命令
	 * @return
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static Result run(File bsdir,List<String> commend) throws IOException, InterruptedException {
		return run(bsdir,commend, DEFAULT_CHARSET_NAME);
	}

	/**
	 * 创建进程并执行指令返回结果
	 *
	 * @param commend     子进程执行的命令
	 * @param charsetName 字符集
	 * @return
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static Result run(File bsdir,List<String> commend, String charsetName) throws IOException, InterruptedException {
		Result result = new Result();
		List<String> ls = new ArrayList<String>();
		InputStream is = null;
			// 重定向异常输出流
			ProcessBuilder processBuilder = new ProcessBuilder(commend);
			processBuilder.directory(bsdir);
			Process process = processBuilder.redirectErrorStream(true).start();
			// 读取输入流中的数据
			is = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, charsetName));
//			StringBuilder data = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				ls.add(line+System.lineSeparator());
				System.out.println("" + line + "");
			}
			// 获取返回码
			result.setCode(process.waitFor());
			result.setData(ls);
			// 获取执行结果
			closeStreamQuietly(is);
		
		return result;
	}

	/**
	 * 关闭数据流
	 *
	 * @param stream
	 */
	private static void closeStreamQuietly(Closeable stream) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 进程处理结果实体类
	 */
	public static class Result {
		public int getCode() {
			return code;
		}
		public void setCode(int code) {
			this.code = code;
		}
		public List<String> getData() {
			return data;
		}
		public void setData(List<String> data) {
			this.data = data;
		}
		/**
		 * 返回码，0：正常，其他：异常
		 */
		public int code;
		/**
		 * 返回结果
		 */
		public List<String> data;
	}

	/**
	 * 测试
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		//单行字符串命令
//		Result r = ProcessUtils.run("cmd /C ipconfig /all", "GBK");
//		System.out.println("code:" + r.code + "\ndata:" + r.data);
		//字符串列表命令
		List<String> commend = new ArrayList<>();
		commend.add("ls");
//		commend.add("/C ipconfig /all");
		try {
			Result r = ProcessUtils.run(new File("/home/"),commend, DEFAULT_CHARSET_NAME);
			System.out.println("code:" + r.code + "\ndata:" + r.data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}