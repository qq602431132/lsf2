package net.zhongfu.entry;

/*
 * #%L
 * NanoHttpd-Webserver
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.util.ServerRunner;
import net.zhongfu.util.IPUtil1;
import net.zhongfu.util.InternalRewrite;
import net.zhongfu.util.NanoFileUpload;
import net.zhongfu.util.ProcessUtils;
import net.zhongfu.util.ProcessUtils.Result;
import net.zhongfu.util.RandomUtils;
import net.zhongfu.util.WebServerPlugin;
import net.zhongfu.util.WebServerPluginInfo;

public class SimpleWebServer extends NanoHTTPD {
	private static final String buibui = "202003121105";
	private static final Logger LOG = Logger.getLogger(SimpleWebServer.class.getName());

	NanoFileUpload nanoFileUploader;
	private final static String sepa = File.separator;
//	boolean isDelFlag;// false※关闭 true※开启
	private final static String DELETE = "delete";
	private final static String IFDEL = "ifDel";
	private final static String GETMD5 = "getMD5";
	private final static String MKDIR = "mkDir";
	private final static String DOCMD = "docmd";
	private final static String CDDIR = "cddir";
	private static List<String> logSkr = new ArrayList<String>();
	static {
		logSkr.add(".! ❀ ♀ ♂ ― ￣ _ @ &");
		logSkr.add("# * ■ § № ○ ● → ※ ▲ △");
		logSkr.add("← ◎ ↑ ◇ ↓ ◆ 〓 □ ¤ ℃");
		logSkr.add("°‰ € ∑ の ≌ つ Θ 阝");
		logSkr.add("丿 § 、 ℃ ☆ ★ 丶 _ 灬");
		logSkr.add("↓ * ____ i 卩 巛 艹 彡");
		logSkr.add("丨 廾 宀 ≮ ≯ ° ╮ ˊ");
		logSkr.add("￠ ⊙ メ ︶ ㄣ ╭");
		logSkr.add("ァ ↗ ↘ ㄟ 乁 ~ ■");
	}

	private static String randomASkr() {
		return RandomUtils.getRandomElement(logSkr);
	}

	public static String getIP() throws UnknownHostException {
		return IPUtil1.getLocalIP();
	}

	/**
	 * Default Index file names.
	 */
	@SuppressWarnings("serial")
	public static final List<String> INDEX_FILE_NAMES = new ArrayList<String>() {
		{
			add("index.html");
			add("index.htm");
		}
	};

	/**
	 * The distribution licence
	 */
	private static final String LICENCE;
	static {
		mimeTypes();
		String text;
		try {
			InputStream stream = SimpleWebServer.class.getResourceAsStream("/LICENSE.txt");
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int count;
			while ((count = stream.read(buffer)) >= 0) {
				bytes.write(buffer, 0, count);
			}
			text = bytes.toString("UTF-8");
		} catch (Exception e) {
			text = "unknown";
		}
		LICENCE = text;
	}

	private static Map<String, WebServerPlugin> mimeTypeHandlers = new HashMap<String, WebServerPlugin>();

	/**
	 * Starts as a standalone file server and waits for Enter.
	 */
	public static void main(String[] args) {
		// Defaults
		int port = 2003;
		String host = null; // bind to all interfaces by default
		List<File> rootDirs = new ArrayList<File>();
		boolean quiet = false;
		String cors = null;
		Map<String, String> options = new HashMap<String, String>();
		// Parse command-line, with short and long versions of the options.
		File patha = new File(new File("").toURI());
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
				patha = (args.length > 1 && args.length < 3) ? new File(args[1]) : patha;
				rootDirs.add(patha.getAbsoluteFile());
			} catch (NumberFormatException e) {
				patha = new File(args[0]);
				rootDirs.add(patha.getAbsoluteFile());
				port = (args.length > 1 && args.length < 3) ? Integer.parseInt(args[1]) : port;
			}
		}

//		for (int i = 0; i < args.length; ++i) {
//			if ("-h".equalsIgnoreCase(args[i]) || "--host".equalsIgnoreCase(args[i])) {
//				host = args[i + 1];
//			} else if ("-p".equalsIgnoreCase(args[i]) || "--port".equalsIgnoreCase(args[i])) {
//				port = Integer.parseInt(args[i + 1]);
//			} else if ("-q".equalsIgnoreCase(args[i]) || "--quiet".equalsIgnoreCase(args[i])) {
//				quiet = true;
//			} else if ("-d".equalsIgnoreCase(args[i]) || "--dir".equalsIgnoreCase(args[i])) {
//				rootDirs.add(new File(args[i + 1]).getAbsoluteFile());
//			} else if (args[i].startsWith("--cors")) {
//				cors = "*";
//				int equalIdx = args[i].indexOf('=');
//				if (equalIdx > 0) {
//					cors = args[i].substring(equalIdx + 1);
//				}
//			} else if ("--licence".equalsIgnoreCase(args[i])) {
//				LOG.info(randomASkr()+SimpleWebServer.LICENCE + "\n");
//			} else if (args[i].startsWith("-X:")) {
//				int dot = args[i].indexOf('=');
//				if (dot > 0) {
//					String name = args[i].substring(0, dot);
//					String value = args[i].substring(dot + 1, args[i].length());
//					options.put(name, value);
//				}
//			}
//		}

		if (rootDirs.isEmpty()) {
			rootDirs.add(new File(".").getAbsoluteFile());
		}
		options.put("host", host);
		options.put("port", "" + port);
		options.put("quiet", String.valueOf(quiet));
		StringBuilder sb = new StringBuilder();
		for (File dir : rootDirs) {
			if (sb.length() > 0) {
				sb.append(":");
			}
			try {
				sb.append(dir.getCanonicalPath());
			} catch (IOException ignored) {
			}
		}
		options.put("home", sb.toString());
		try {
			LOG.info(randomASkr() + "开始运行※访问地址※http://" + getIP() + ":" + port + "/");
		} catch (Exception e) {
			LOG.info(randomASkr() + "启动失败※" + e.getMessage());
		}
		ServiceLoader<WebServerPluginInfo> serviceLoader = ServiceLoader.load(WebServerPluginInfo.class);
		for (WebServerPluginInfo info : serviceLoader) {
			String[] mimeTypes = info.getMimeTypes();
			for (String mime : mimeTypes) {
				String[] indexFiles = info.getIndexFilesForMimeType(mime);
				if (!quiet) {
					LOG.info(randomASkr() + "# Found plugin for Mime type: \"" + mime + "\"");
					if (indexFiles != null) {
						LOG.info(randomASkr() + " (serving index files: ");
						for (String indexFile : indexFiles) {
							LOG.info(randomASkr() + indexFile + " ");
						}
					}
					LOG.info(randomASkr() + ").");
				}
				registerPluginForMimeType(indexFiles, mime, info.getWebServerPlugin(mime), options);
			}
		}
		ServerRunner.executeInstance(new SimpleWebServer(host, port, rootDirs, quiet, cors));
	}

	protected static void registerPluginForMimeType(String[] indexFiles, String mimeType, WebServerPlugin plugin,
			Map<String, String> commandLineOptions) {
		if (mimeType == null || plugin == null) {
			return;
		}
		if (indexFiles != null) {
			for (String filename : indexFiles) {
				int dot = filename.lastIndexOf('.');
				if (dot >= 0) {
					String extension = filename.substring(dot + 1).toLowerCase();
					mimeTypes().put(extension, mimeType);
				}
			}
			SimpleWebServer.INDEX_FILE_NAMES.addAll(Arrays.asList(indexFiles));
		}
		SimpleWebServer.mimeTypeHandlers.put(mimeType, plugin);
		plugin.initialize(commandLineOptions);
	}

	private final boolean quiet;
	private final String cors;
	protected List<File> rootDirs;

	public SimpleWebServer(String host, int port, File wwwroot, boolean quiet, String cors) {
		this(host, port, Collections.singletonList(wwwroot), quiet, cors);
	}

	public SimpleWebServer(String host, int port, File wwwroot, boolean quiet) {
		this(host, port, Collections.singletonList(wwwroot), quiet, null);
	}

	public SimpleWebServer(String host, int port, List<File> wwwroots, boolean quiet) {
		this(host, port, wwwroots, quiet, null);
	}

	public SimpleWebServer(String host, int port, List<File> wwwroots, boolean quiet, String cors) {
		super(host, port);
		this.quiet = quiet;
		this.cors = cors;
		this.rootDirs = new ArrayList<File>(wwwroots);
		this.nanoFileUploader = new NanoFileUpload(new DiskFileItemFactory());
		init();
	}

	private boolean canServeUri(String uri, File homeDir) {
		boolean canServeUri;
		File f = new File(homeDir, uri);
		canServeUri = f.exists();
		if (!canServeUri) {
			WebServerPlugin plugin = SimpleWebServer.mimeTypeHandlers.get(getMimeTypeForFile(uri));
			if (plugin != null) {
				canServeUri = plugin.canServeUri(uri, homeDir);
			}
		}
		return canServeUri;
	}

	/**
	 * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
	 * instead of '+'.
	 */
	private String encodeUri(String uri) {
		String newUri = "";
		StringTokenizer st = new StringTokenizer(uri, "/ ", true);
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			if ("/".equals(tok)) {
				newUri += "/";
			} else if (" ".equals(tok)) {
				newUri += "%20";
			} else {
				try {
					newUri += URLEncoder.encode(tok, "UTF-8");
				} catch (UnsupportedEncodingException ignored) {
					ignored.printStackTrace();
				}
			}
		}
		return newUri;
	}

	private String findIndexFileInDirectory(File directory) {
		for (String fileName : SimpleWebServer.INDEX_FILE_NAMES) {
			File indexFile = new File(directory, fileName);
			if (indexFile.isFile()) {
				return fileName;
			}
		}
		return null;
	}

	protected Response render403(String s) {
		return NanoHTTPD.newFixedLengthResponse(Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
	}

	protected Response render500(String s) {
		return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
				"INTERNAL ERROR: " + s);
	}

	protected Response render404() {
		return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
				"Error 404, file not found.");
	}

	private Response render200(String s) {
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, s);
	}

	/**
	 * Used to initialize and customize the server.
	 */
	public void init() {
	}

	public void do_buildJS(StringBuilder msg) {
		msg.append("<script type=\"text/javascript\">");
		msg.append("function callBackMD5(murl,domId){\r\n"
				+ "http.get({url:murl,timeout:100000},function(err,result){document.getElementById(domId).innerHTML=result;});\r\n"
				+ "}");
		msg.append("var http = {};\n");
		msg.append("http.quest = function (option, callback) {\n");
		msg.append("    var url = option.url;\n");
		msg.append("    var method = option.method;\n");
		msg.append("    var data = option.data;\n");
		msg.append("    var timeout = option.timeout || 0;\n");
		msg.append("    var xhr = new XMLHttpRequest();\n");
		msg.append("    (timeout > 0) && (xhr.timeout = timeout);\n");
		msg.append("    xhr.onreadystatechange = function () {\n");
		msg.append("        if (xhr.readyState == 4) {\n");
		msg.append("            if (xhr.status >= 200 && xhr.status < 400) {\n");
		msg.append("            var result = xhr.responseText;\n");
		msg.append("            try {result = JSON.parse(xhr.responseText);} catch (e) {}\n");
		msg.append("                callback && callback(null, result);\n");
		msg.append("            } else {\n");
		msg.append("                callback && callback('status: ' + xhr.status);\n");
		msg.append("            }\n");
		msg.append("        }\n");
		msg.append("    }.bind(this);\n");
		msg.append("    xhr.open(method, url, true);\n");
		msg.append("    if(typeof data === 'object'){\n");
		msg.append("        try{\n");
		msg.append("            data = JSON.stringify(data);\n");
		msg.append("        }catch(e){}\n");
		msg.append("    }\n");
		msg.append("    xhr.send(data);\n");
		msg.append("    xhr.ontimeout = function () {\n");
		msg.append("        callback && callback('timeout');\n");
		msg.append(
				"        console.log('%c连%c接%c超%c时', 'color:red', 'color:orange', 'color:purple', 'color:green');\n");
		msg.append("    };\n");
		msg.append("};\n");
		msg.append("http.get = function (url, callback) {\n");
		msg.append("    var option = url.url ? url : { url: url };\n");
		msg.append("    option.method = 'get';\n");
		msg.append("    this.quest(option, callback);\n");
		msg.append("};\n");
		msg.append("http.post = function (option, callback) {\n");
		msg.append("    option.method = 'post';\n");
		msg.append("    this.quest(option, callback);\n");
		msg.append("};\n");
		msg.append("</script>");
	}

	protected String listDirectory(IHTTPSession session,String uri, File f) throws IOException {
		String heading = "Directory " + uri;
		StringBuilder msg = new StringBuilder(
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
						+ "<html xmlns=\"http://www.w3.org/1999/xhtml\">" + "<head>"
						+ "<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />" + "<title>"
						+ heading + "</title>" + "<style type=\"text/css\">"
						+ "<!--H1 {font-family: Tahoma, Arial, sans-serif;color: white;background-color: #525d76;font-size: 22px;}H2 {font-family: Tahoma, Arial, sans-serif;color: white;background-color: #525d76;font-size: 16px;}H3 {font-family: Tahoma, Arial, sans-serif;color: white;background-color: #525d76;font-size: 14px;}BODY {font-family: Tahoma, Arial, sans-serif;color: black;background-color: white;}B {font-family: Tahoma, Arial, sans-serif;color: white;background-color: #525d76;}P {font-family: Tahoma, Arial, sans-serif;background: white;color: black;font-size: 12px;}A {color: black;}A.name {color: black;}HR {color: #525d76;}-->"
						+ "</style>");
		do_buildJS(msg);
		msg.append("</head>");
		msg.append("<body><h1>" + heading + "</h1>");
		msg.append("<hr>");
		msg.append("<table><tr>");
		msg.append(
				"<td><input name=\"bt_fh\" type=\"button\" value=\"返回\" onclick=\"javascript:history.back();\" /></td>");
		msg.append("<td><form enctype=\"multipart/form-data\" name=\"fileul\" method=\"post\">");
		msg.append("<input name=\"file\" type=\"file\"/>");
		msg.append("<input name=\"btn_sc\" type=\"submit\" value=\"上传\"/>");
		msg.append("</form></td>");
		msg.append("<td><form name=\"xjwjj\" method=\"get\">");
		msg.append("<input type=\"text\" name=\"" + MKDIR + "\" />");
		msg.append("<input type=\"submit\" name=\"btn_xjwjj\" value=\"新建文件夹\" />  ");
		msg.append("</form></td>");
		msg.append("<td><form name=\"szifdel\" method=\"get\">");
		msg.append("<input type=\"hidden\" name=\"" + IFDEL + "\" value=\"" + randomASkr() + "\" ></input>");
		boolean isDelFlag =Boolean.parseBoolean(session.getHeaders().get(IFDEL));
		String isDelstr = (isDelFlag) ? "关闭删除" + "</button><font color=\"red\">删除功能已开启，请谨慎操作！！！</font>"
				: "开启删除" + "</button>";
		msg.append("<button name=\"btn_kgdel\" type=\"submit\" >" + isDelstr + "</button>");
		msg.append("</form></td>");
		msg.append("<td><input name=\"fhsy\" type=\"button\" value=\"返回首页\" onclick=\"location='/'\" /></td>");
		msg.append("</tr></table>");
		msg.append("<hr>");
		String up = null;
		if (uri.length() > 1) {
			String u = uri.substring(0, uri.length() - 1);
			int slash = u.lastIndexOf('/');
			if (slash >= 0 && slash < u.length()) {
				up = uri.substring(0, slash + 1);
			}
		}
		List<String> files = Arrays.asList(f.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isFile();
			}
		}));
		Collections.sort(files);
		List<String> directories = Arrays.asList(f.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isDirectory();
			}
		}));
		Collections.sort(directories);
		if (up != null || directories.size() + files.size() > 0) {
			msg.append(
					"<table border=0 width=\"100%\" cellspacing=\"0\" cellpadding=\"5\" align=\"center\" style=\"overflow: scroll;word-break: keep-all\">"
							+ "<tr bgcolor=\"#00db00\">" + "<td width=\"5%\">序号</td>" + "<td width=\"30%\">文件名</td>"
							+ "<td width=\"15%\">文件大小</td>" + "<td  width=\"25%\">文件修改时间</td>"
							+ "<td align=\"center\">MD5</td>" + "<td width=\"5%\">操作</td>" + "</tr>");
			int xxxidn = 1;
			String xxxName = "";
			String xxxSize = "";
			String xxxModifyTime = "";
			String xxxGetMD5 = "";
			String xxxIsDel = "";
			if (up != null || directories.size() > 0) {
				// 所有的目录呗
				if (up != null) {
					// 单独一行
					xxxName = "..";
					xxxIsDel = "";
					msg.append("<tr>");
					msg.append("<td>" + xxxidn++ + "</td>");
					msg.append("<td><a rel=\"directory\" href=\"").append(encodeUri(up))
							.append("\"><span>" + xxxName + "</span></a></td>" + "<td>" + xxxSize + "</td><td>"
									+ xxxModifyTime + "</td><td>" + xxxGetMD5 + "</td><td>" + xxxIsDel + "</td></tr>");
				}
				for (String directory : directories) {
					// 每条一行
					if (xxxidn % 2 != 0) {
						msg.append("<tr>");
						msg.append("<td>" + xxxidn++ + "</td>");
					} else {
						msg.append("<tr bgcolor=\"#eeeeee\">");
						msg.append("<td>" + xxxidn++ + "</td>");
					}
					xxxName = directory + "/";
					if (isDelFlag) {
						xxxIsDel = "<a style=\"background-color: #ceffce;\"  href=\"?delete=" + encodeUri(xxxName)
								+ "\" onclick=\"return confirm('请确认是否删除:" + xxxName + "?');\">删除</a>";
					} else {
						xxxIsDel = "<a>删除</a>";
					}
					msg.append("<td><a rel=\"directory\" href=\"").append(encodeUri(xxxName))
							.append("\"><span style=\"background-color: #ceffce;\">").append(xxxName)
							.append("</span></a></td>" + "<td>" + xxxSize + "</td><td>" + xxxModifyTime + "</td><td>"
									+ xxxGetMD5 + "</td><td>" + xxxIsDel + "</td></tr>");
				}

			}
			if (files.size() > 0) {

				for (String file : files) {
					// 每条一行
					xxxName = file;
					if (xxxidn % 2 != 0) {
						msg.append("<tr>");
						msg.append("<td>" + xxxidn + "</td>");
					} else {
						msg.append("<tr bgcolor=\"#eeeeee\">");
						msg.append("<td>" + xxxidn + "</td>");
					}
					msg.append("<td><a href=\"").append(encodeUri(xxxName)).append("\"><span>").append(xxxName)
							.append("</span></a>");
					File curFile = new File(f, xxxName);
					long len = curFile.length();
//					msg.append("");
//					if (len < 1024) {
//						msg.append(len).append(" bytes");
//					} else if (len < 1024 * 1024) {
//						msg.append(len / 1024).append(".").append(len % 1024 / 10 % 100).append(" KB");
//					} else {
//						msg.append(len / (1024 * 1024)).append(".").append(len % (1024 * 1024) / 10000 % 100)
//								.append(" MB");
//					}
					xxxSize = FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(curFile)) + "";
					DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE HH时mm分ss秒");
					BasicFileAttributes attributes = Files.readAttributes(curFile.toPath(), BasicFileAttributes.class);
					LocalDateTime fileCreationTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(),
							ZoneId.systemDefault());
					LocalDateTime fileLastModifiedTime = LocalDateTime
							.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());
					xxxModifyTime = "" + dateTimeFormatter.format(fileCreationTime);
					xxxGetMD5 = "<a id='a_" + xxxidn + "' onclick=\"callBackMD5('?" + GETMD5 + "=" + encodeUri(xxxName)
							+ "','a_" + xxxidn + "')\">MD5</a>";
					if (isDelFlag) {
						xxxIsDel = "<a style=\"background-color: #ceffce;\"  href=\"?delete=" + encodeUri(xxxName)
								+ "\" onclick=\"return confirm('请确认是否删除:" + xxxName + "?');\">删除</a>";
					} else {
						xxxIsDel = "<a>删除</a>";
					}
					msg.append("</td>" + "<td>" + xxxSize + "</td><td>" + xxxModifyTime + "</td><td align=\"center\">"
							+ xxxGetMD5 + "</td><td>" + xxxIsDel + "</td></tr>");
					xxxidn++;
				}

			}
		}
		msg.append("</table>\n<hr>\n<span onClick=\"location='/?" + DOCMD + "=dir&&" + CDDIR
				+ "='\" ><h2>Powered By kanbuxiaqu@outlook.com</h2></span>\n</body>\n</html>\n");
		return msg.toString();
	}

	public static Response addRangesHeaders(IStatus status, String mimeType, String message) {
		Response response = NanoHTTPD.newFixedLengthResponse(status, mimeType, message);
		response.addHeader("Accept-Ranges", "bytes");
		return response;
	}

	private Response respond(Map<String, String> headers, IHTTPSession session, String uri) {
		// First let's handle CORS OPTION query
		Response r = null;
		if (cors != null && Method.OPTIONS.equals(session.getMethod())) {
			r = NanoHTTPD.newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, null, 0);
		} else {
			try {
				r = defaultRespond(headers, session, uri);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (cors != null) {
			r = addCORSHeaders(headers, r, cors);
		}
		session.getCookies().unloadQueue(r);
		return r;
	}

	private Response defaultRespond(Map<String, String> headers, IHTTPSession session, String uri) throws IOException {
		// Remove URL arguments
		uri = uri.trim().replace(File.separatorChar, '/');
		if (uri.indexOf('?') >= 0) {
			uri = uri.substring(0, uri.indexOf('?'));
		}

		// Prohibit getting out of current directory
		if (uri.contains("../")) {
			return render403("Won't serve ../ for security reasons.");
		}

		boolean canServeUri = false;
		File homeDir = null;
		for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
			homeDir = this.rootDirs.get(i);
			canServeUri = canServeUri(uri, homeDir);
		}
		if (!canServeUri) {
			return render404();
		}

		// Browsers get confused without '/' after the directory, send a
		// redirect.
		File f = new File(homeDir, uri);
		if (f.isDirectory() && !uri.endsWith("/")) {
			uri += "/";
			Response res = addRangesHeaders(Status.REDIRECT, NanoHTTPD.MIME_HTML,
					"<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
			res.addHeader("Location", uri);
			return res;
		}

		if (f.isDirectory()) {
			// First look for index files (index.html, index.htm, etc) and if
			// none found, list the directory if readable.
			String indexFile = findIndexFileInDirectory(f);
			if (indexFile == null) {
				if (f.canRead()) {
					// No index file, list the directory if it is readable
					return addRangesHeaders(Status.OK, NanoHTTPD.MIME_HTML, listDirectory(session,uri, f));
				} else {
					return render403("No directory listing.");
				}
			} else {
				return respond(headers, session, uri + indexFile);
			}
		}
		String mimeTypeForFile = getMimeTypeForFile(uri);
		WebServerPlugin plugin = SimpleWebServer.mimeTypeHandlers.get(mimeTypeForFile);
		Response response = null;
		if (plugin != null && plugin.canServeUri(uri, homeDir)) {
			response = plugin.serveFile(uri, headers, session, f, mimeTypeForFile);
			if (response != null && response instanceof InternalRewrite) {
				InternalRewrite rewrite = (InternalRewrite) response;
				return respond(rewrite.getHeaders(), session, rewrite.getUri());
			}
		} else {
			response = serveFile(uri, headers, f, mimeTypeForFile);
		}
		return response != null ? response : render404();
	}

	// 入口
	@Override
	public Response serve(IHTTPSession session) {
		Map<String, String> header = session.getHeaders();
		Map<String, String> parms = session.getParms();
		CookieHandler cookies = session.getCookies();
		String read = cookies.read(IFDEL);
		System.out.println("IFDEL="+read);
		if("".equals(read)) {
			//第一次请求
			cookies.set(IFDEL, "false", 1);
			header.put(IFDEL, false+"");
		}else {
			header.put(IFDEL, read);
		}
		String uri = session.getUri();
		LOG.info(randomASkr() + "请求uri:" + uri + ",请求参数:" + parms.keySet());
		try {
			boolean canServeUri = false;
			File homeDir = null;
			for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
				homeDir = this.rootDirs.get(i);
				canServeUri = canServeUri(session.getUri(), homeDir);
			}
			if (!canServeUri) {
				return render404();
			}
			File reqFile = new File(FilenameUtils.normalize(homeDir.getCanonicalPath() + session.getUri()));
			LOG.info(randomASkr() + "reqFile" + reqFile.toPath());
			if (session.getMethod() == Method.POST && NanoFileUpload.isMultipartContent(session)) { // 处理POST请求
				List<FileItem> parseRequest = nanoFileUploader.parseRequest(session);
				FileItem fileItem = parseRequest.get(0);
				String fname = FilenameUtils.getName(fileItem.getName());
				File tgFile = new File(FilenameUtils.normalize(reqFile.getAbsolutePath() + sepa + fname));
				LOG.info(randomASkr() + "tgFile:" + tgFile.toPath());
				if ("".equals(fname)) {
					return doshowFile(session);
				} else {
					InputStream is = fileItem.getInputStream();
					while (tgFile.exists()) {
						tgFile = new File(FilenameUtils
								.normalize(reqFile.getAbsolutePath() + "" + sepa + "_" + tgFile.getName()));
						LOG.info(randomASkr() + "tgFile:" + tgFile.toPath());
					}
					FileOutputStream os = FileUtils.openOutputStream(tgFile, false);
					Streams.copy(is, os, true);
					is.close();
					os.flush();
					os.close();
					return doshowFile(session);
				}
			} else if (session.getMethod() == Method.GET && parms.keySet().size() > 0) {// 处理GET
				LOG.info(randomASkr() + parms);
				if (parms.containsKey(DELETE)) {
					String delFileName = parms.get(DELETE);
					File tgFile = new File(FilenameUtils.normalize(reqFile.getAbsolutePath() + sepa + delFileName));
					LOG.info(randomASkr() + "tgFile:" + tgFile.toPath());
					return deal_delFile(session, parms);
				} else if (parms.containsKey(GETMD5)) {
					String getMD5FileName = parms.get(GETMD5);
					File tgFile = new File(FilenameUtils.normalize(reqFile.getAbsolutePath() + sepa + getMD5FileName));
					LOG.info(randomASkr() + "tgFile:" + tgFile.toPath());
					return deal_getMD5(session, parms);
				} else if (parms.containsKey(DOCMD)) {
					String doCMDFileName = parms.get(DOCMD);
					File tgFile = new File(FilenameUtils.normalize(reqFile.getAbsolutePath() + sepa + doCMDFileName));
					LOG.info(randomASkr() + "tgFile:" + tgFile.toPath());
					return deal_docmd(session, parms);
				} else if (parms.containsKey(MKDIR)) {
					String mkDIRName = parms.get(MKDIR);
					File tgFile = new File(FilenameUtils.normalize(reqFile.getAbsolutePath() + sepa + mkDIRName));
					LOG.info(randomASkr() + "tgFile:" + tgFile.toPath());
					return deal_makeDir(session, parms);
				} else if (parms.containsKey(IFDEL)) {
					return deal_ifdel(session, parms);
				} else {
					File tgFile = new File(FilenameUtils.normalize(reqFile.getAbsolutePath()));
					LOG.info(randomASkr() + "tgFile:" + tgFile.toPath());
					return doshowFile(session);
				}
			} else {
				File tgFile = new File(FilenameUtils.normalize(reqFile.getAbsolutePath()));
				LOG.info(randomASkr() + "tgFile:" + tgFile.toPath());
				return doshowFile(session);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return doshowFile(session);
		} catch (FileUploadException e) {
			e.printStackTrace();
			return doshowFile(session);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return doshowFile(session);
		}
	}

	private Response deal_ifdel(IHTTPSession session, Map<String, String> parms) {
		CookieHandler cookies = session.getCookies();
		boolean isDelFlag =Boolean.parseBoolean(cookies.read(IFDEL));
		isDelFlag = !isDelFlag;
		System.out.println("替换为:"+isDelFlag);
		cookies.set(IFDEL, ""+isDelFlag, 1);
		session.getHeaders().put(IFDEL, ""+isDelFlag+"");
		return doshowFile(session);
	}

	private Response deal_makeDir(IHTTPSession session, Map<String, String> parms) {
		String dirName = parms.get(MKDIR);
		boolean canServeUri = false;
		File homeDir = null;
		for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
			homeDir = this.rootDirs.get(i);
			canServeUri = canServeUri(session.getUri(), homeDir);
		}
		if (!canServeUri) {
			return render404();
		}
		try {
			File reqFile = new File(FilenameUtils.normalize(homeDir.getCanonicalPath() + session.getUri()));
			File tgDir = new File(FilenameUtils.normalize(reqFile.getAbsolutePath() + sepa + dirName));
			if (dirName == null || "".equals(dirName)) {
				return doshowFile(session);
			} else if (tgDir.exists() || tgDir.mkdirs()) {
				return doshowFile(session);
			} else {
				return doshowFile(session);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return doshowFile(session);
		}

	}

	private Response deal_docmd(IHTTPSession session, Map<String, String> parms) {
		StringBuilder msg = new StringBuilder("<html><head><style><!--\n" + "span.dirname { font-weight: bold; }\n"
				+ "span.filesize { font-size: 75%; }\n" + "// -->\n" + "</style>" + "</head><body>");
		boolean canServeUri = false;
		File homeDir = null;
		for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
			homeDir = this.rootDirs.get(i);
			canServeUri = canServeUri(session.getUri(), homeDir);
		}
		if (!canServeUri) {
			return render404();
		}
		List<String> cmds = new ArrayList<String>();
		String cmdstr = parms.get(DOCMD);
		if (IPUtil1.isWindowsOS()) {
			cmds.add("cmd.exe");
			cmds.add("/c");
		} else {
			cmds.add("sh");
			cmds.add("-c");
		}
		cmds.add(cmdstr);
		String cddir1 = parms.get(CDDIR);
		if (!"".equals(cmdstr)) {
			try {
				String cddir = "".equals(cddir1)
						? FilenameUtils.normalize(homeDir.getCanonicalPath() + session.getUri())
						: cddir1;
				File reqFile = new File(FilenameUtils.normalize(homeDir.getCanonicalPath() + session.getUri()));
				Result r = ProcessUtils.run(new File(cddir), cmds);
				StringBuilder rlb = new StringBuilder();
				if (r.code == 0) {
					msg.append("<form name=\"zxcmdform\" method=\"get\">");
					msg.append("<input type=\"text\" name=\"" + CDDIR + "\" value=\"" + cddir + "\" />");
					msg.append("<input type=\"text\" name=\"" + DOCMD + "\" value=\"" + cmdstr + "\" />");
					msg.append("<input type=\"submit\" name=\"btn_zxcmd\" value=\"执行CMD\" />  ");
					msg.append("</form>");
					for (String rl : r.data) {
						rlb.append("<p><script type=\"text/html\" style='display:block'>").append(rl.trim())
								.append("</script>");
					}
					return render200(msg.toString() + "执行返回code※" + r.code + "<p>" + rlb.toString() + "</body></html>");
				} else {
					msg.append("<form name=\"zxcmdform\" method=\"get\">");
					msg.append("<input type=\"text\" name=\"" + CDDIR + "\" value=\"" + cddir + "\" />");
					msg.append("<input type=\"text\" name=\"" + DOCMD + "\" value=\"" + cmdstr + "\" />");
					msg.append("<input type=\"submit\" name=\"btn_zxcmd\" value=\"执行CMD\" />  ");
					msg.append("</form>");
					for (String rl : r.data) {
						rlb.append("<p><script type=\"text/html\" style='display:block'>").append(rl.trim())
								.append("</script>");
					}
					return render200(
							msg.toString() + "执行命令失败code※" + r.code + "<p>" + rlb.toString() + "</body></html>");
				}
			} catch (Exception e) {
				e.printStackTrace();
				return doshowFile(session);
			}
		} else {
			return doshowFile(session);
		}
	}

	private Response deal_getMD5(IHTTPSession session, Map<String, String> parms) {
		String delFileName = parms.get(GETMD5);
		boolean canServeUri = false;
		File homeDir = null;
		for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
			homeDir = this.rootDirs.get(i);
			canServeUri = canServeUri(session.getUri(), homeDir);
		}
		if (!canServeUri) {
			return render404();
		}
		try {
			File reqFile = new File(FilenameUtils.normalize(homeDir.getCanonicalPath() + session.getUri()));
			File tgFile = new File(FilenameUtils.normalize(reqFile.getAbsolutePath() + sepa + delFileName));
			FileInputStream is = FileUtils.openInputStream(tgFile);
			String md5Hex = DigestUtils.md5Hex(is);
			is.close();
			return render200("" + md5Hex);
		} catch (IOException e) {
			e.printStackTrace();
			return render500(e.getMessage());
		}
	}

	private Response deal_delFile(IHTTPSession session, Map<String, String> parms)
			throws URISyntaxException, IOException {
		boolean canServeUri = false;
		File homeDir = null;
		for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
			homeDir = this.rootDirs.get(i);
			canServeUri = canServeUri(session.getUri(), homeDir);
		}
		if (!canServeUri) {
			return render404();
		}
		String delFileName = parms.get(DELETE);
		File reqFile = new File(FilenameUtils.normalize(homeDir.getCanonicalPath() + session.getUri()));
		File tgFile = new File(FilenameUtils.normalize(reqFile.getAbsolutePath() + sepa + delFileName));
		boolean isDelFlag =Boolean.parseBoolean(session.getHeaders().get(IFDEL));
		if (isDelFlag) {
			FileUtils.deleteQuietly(tgFile);
			return doshowFile(session);
		} else {
			return doshowFile(session);
		}
	}

	public Response doshowFile(IHTTPSession session) {
		Map<String, String> header = session.getHeaders();
		Map<String, String> parms = session.getParms();
		String uri = session.getUri();
		if (!this.quiet) {
			LOG.info(randomASkr() + session.getMethod() + " '" + uri + "' ");
			Iterator<String> e = header.keySet().iterator();
			while (e.hasNext()) {
				String value = e.next();
				LOG.info(randomASkr() + "  HDR: '" + value + "' = '" + header.get(value) + "'");
			}
			e = parms.keySet().iterator();
			while (e.hasNext()) {
				String value = e.next();
				LOG.info(randomASkr() + "  PRM: '" + value + "' = '" + parms.get(value) + "'");
			}
		}
		for (File homeDir : this.rootDirs) {
			if (!homeDir.isDirectory()) {
				return render500("given path is not a directory (" + homeDir + ").");
			}
		}
		return respond(Collections.unmodifiableMap(header), session, uri);
	}

	/**
	 * Serves file from homeDir and its' subdirectories (only). Uses only URI,
	 * ignores all headers and HTTP parameters.
	 */
	Response serveFile(String uri, Map<String, String> header, File file, String mime) {
		Response res;
		try {
			// Calculate etag
			String etag = Integer
					.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

			// Support (simple) skipping:
			long startFrom = 0;
			long endAt = -1;
			String range = header.get("range");
			if (range != null) {
				if (range.startsWith("bytes=")) {
					range = range.substring("bytes=".length());
					int minus = range.indexOf('-');
					try {
						if (minus > 0) {
							startFrom = Long.parseLong(range.substring(0, minus));
							endAt = Long.parseLong(range.substring(minus + 1));
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}

			// get if-range header. If present, it must match etag or else we
			// should ignore the range request
			String ifRange = header.get("if-range");
			boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

			String ifNoneMatch = header.get("if-none-match");
			boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null
					&& ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));

			// Change return code and add Content-Range header when skipping is
			// requested
			long fileLen = file.length();

			if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
				// range request that matches current etag
				// and the startFrom of the range is satisfiable
				if (headerIfNoneMatchPresentAndMatching) {
					// range request that matches current etag
					// and the startFrom of the range is satisfiable
					// would return range from file
					// respond with not-modified
					res = addRangesHeaders(Status.NOT_MODIFIED, mime, "");
					res.addHeader("ETag", etag);
				} else {
					if (endAt < 0) {
						endAt = fileLen - 1;
					}
					long newLen = endAt - startFrom + 1;
					if (newLen < 0) {
						newLen = 0;
					}

					FileInputStream fis = new FileInputStream(file);
					fis.skip(startFrom);

					res = NanoHTTPD.newFixedLengthResponse(Status.PARTIAL_CONTENT, mime, fis, newLen);

					res.addHeader("Accept-Ranges", "bytes");
					res.addHeader("Content-Length", "" + newLen);
					res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
					res.addHeader("ETag", etag);
				}
			} else {

				if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
					// return the size of the file
					// 4xx responses are not trumped by if-none-match
					res = addRangesHeaders(Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
					res.addHeader("Content-Range", "bytes */" + fileLen);
					res.addHeader("ETag", etag);
				} else if (range == null && headerIfNoneMatchPresentAndMatching) {
					// full-file-fetch request
					// would return entire file
					// respond with not-modified
					res = addRangesHeaders(Status.NOT_MODIFIED, mime, "");
					res.addHeader("ETag", etag);
				} else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
					// range request that doesn't match current etag
					// would return entire (different) file
					// respond with not-modified

					res = addRangesHeaders(Status.NOT_MODIFIED, mime, "");
					res.addHeader("ETag", etag);
				} else {
					// supply the file
					res = addRangesHeaders(file, mime);
					res.addHeader("Content-Length", "" + fileLen);
					res.addHeader("ETag", etag);
				}
			}
		} catch (IOException ioe) {
			res = render403("Reading file failed.");
		}
		return res;
	}

	private Response addRangesHeaders(File file, String mime) throws FileNotFoundException {
		Response res;
		res = NanoHTTPD.newFixedLengthResponse(Status.OK, mime, new FileInputStream(file), (int) file.length());
		res.addHeader("Accept-Ranges", "bytes");
		return res;
	}

	protected Response addCORSHeaders(Map<String, String> queryHeaders, Response resp, String cors) {
		resp.addHeader("Access-Control-Allow-Origin", cors);
		resp.addHeader("Access-Control-Allow-Headers", calculateAllowHeaders(queryHeaders));
		resp.addHeader("Access-Control-Allow-Credentials", "true");
		resp.addHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
		resp.addHeader("Access-Control-Max-Age", "" + MAX_AGE);

		return resp;
	}

	public HTTPSession createSession(TempFileManager tempFileManager, InputStream inputStream,
			OutputStream outputStream) {
		return new HTTPSession(tempFileManager, inputStream, outputStream);
	}

	public HTTPSession createSession(TempFileManager tempFileManager, InputStream inputStream,
			OutputStream outputStream, InetAddress inetAddress) {
		return new HTTPSession(tempFileManager, inputStream, outputStream, inetAddress);
	}

	private String calculateAllowHeaders(Map<String, String> queryHeaders) {
		// here we should use the given asked headers
		// but NanoHttpd uses a Map whereas it is possible for requester to send
		// several time the same header
		// let's just use default values for this version
		return System.getProperty(ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME, DEFAULT_ALLOWED_HEADERS);
	}

	private final static String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";
	private final static int MAX_AGE = 42 * 60 * 60;
	public final static String DEFAULT_ALLOWED_HEADERS = "origin,accept,content-type";
	public final static String ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME = "AccessControlAllowHeader";
}
