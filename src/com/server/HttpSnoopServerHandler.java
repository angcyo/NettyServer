package com.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

public class HttpSnoopServerHandler extends SimpleChannelInboundHandler<Object> {

	private HttpRequest request;
	/** Buffer that stores the response content */
	private final StringBuilder buf = new StringBuilder();

	public static Map<String, PlayerInfo> playerMap = new HashMap<>();// 保存所有广告机
	private static Map<String, String> paramsMap;// 保存所有参数

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
		initRequestInfo(msg, false);

		if (msg instanceof HttpRequest) {
			HttpRequest request = this.request = (HttpRequest) msg;
			HttpMethod method = request.getMethod();

			if (method == HttpMethod.GET) {
				try {
					QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
							request.getUri());
					String cmd = queryStringDecoder.path();// 请求的命令 类似:
															// /getName.xml

					// 处理请求...
					if (cmd.equalsIgnoreCase("/list")) {
						DefaultFullHttpResponse response = new DefaultFullHttpResponse(
								HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
								Unpooled.copiedBuffer(getList(),
										CharsetUtil.UTF_8));
						ctx.write(response);
					} else {
						DefaultFullHttpResponse response = new DefaultFullHttpResponse(
								HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
								Unpooled.copiedBuffer("欢迎访问!",
										CharsetUtil.UTF_8));
						ctx.write(response);
					}
					// 快速写入,并且flush
					ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
							ChannelFutureListener.CLOSE);
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else if (method == HttpMethod.POST) {
				try {
					HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(
							new DefaultHttpDataFactory(false),
							(HttpRequest) msg);

					InterfaceHttpData data = decoder.getBodyHttpData("info");
					if (data != null) {
						updatePlayerMap(((Attribute) data).getValue());// 保存终端信息

						DefaultFullHttpResponse response = new DefaultFullHttpResponse(
								HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
								Unpooled.copiedBuffer("已收到数据...".toString(),
										CharsetUtil.UTF_8));

						ctx.write(response);

						// 快速写入,并且flush
						ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
								ChannelFutureListener.CLOSE);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 显示请求的方法,参数,等信息
	public String initRequestInfo(Object msg, boolean isShow) {
		StringBuilder requestInfo = new StringBuilder();
		if (msg instanceof HttpRequest) {
			paramsMap = new HashMap<String, String>();

			HttpRequest request = (HttpRequest) msg;
			requestInfo.append("请求的URL-->" + request.getUri() + "\n");

			HttpMethod method = request.getMethod();

			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
					request.getUri());

			String cmd = queryStringDecoder.path();// 请求的命令 类似: /getName.xml

			requestInfo.append("请求函数-->" + cmd + "\n");

			if (method == HttpMethod.GET) {
				requestInfo.append("请求方式-->GET\n参数:\n");

				Map<String, List<String>> params = queryStringDecoder
						.parameters();
				if (!params.isEmpty()) {
					for (java.util.Map.Entry<String, List<String>> p : params
							.entrySet()) {
						String key = p.getKey();
						List<String> vals = p.getValue();

						paramsMap.put(key, vals.get(0));
						requestInfo.append(key + ":" + vals.get(0) + "\n");
					}
				}

			} else if (method == HttpMethod.POST) {
				requestInfo.append("请求方式-->POST\n参数:\n");

				HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(
						new DefaultHttpDataFactory(false), (HttpRequest) msg);

				try {
					String key;
					String value;
					for (InterfaceHttpData iterable_element : decoder
							.getBodyHttpDatas()) {
						if (iterable_element != null) {
							key = iterable_element.getName();
							value = ((Attribute) iterable_element).getValue();

							requestInfo.append(key + ":" + value);

							paramsMap.put(key, value);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			if (isShow) {
				System.out.println(requestInfo.toString());
			}

			return requestInfo.toString();

		} else {

			if (isShow) {
				System.out.println("未知的HttpRequest...");
			}

			return "未知的HttpRequest...";
		}

	}

	private void updatePlayerMap(String value) {
		if (value == null || value.length() < 1) {
			return;
		}
		PlayerInfo info = new PlayerInfo();

		try {
			JSONObject jsonObject = new JSONObject(value);
			info.mac = jsonObject.getString("mac");
			info.address = jsonObject.getString("addr");
			info.state = jsonObject.getString("status");
			info.id = String.valueOf(jsonObject.getInt("machineID"));
			info.ip = jsonObject.getString("ip");
			info.name = jsonObject.getString("name");

			playerMap.put(info.ip, info);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// 获取所有终端
	public String getList() {
		String list = "";
		StringBuilder listInfo = new StringBuilder();
		try {
			String listHtml = convertStreamToString(new FileInputStream(
					"./raw/list.html"));
			String listLine = convertStreamToString(new FileInputStream(
					"./raw/info.html"));

			PlayerInfo info;
			for (Map.Entry<String, PlayerInfo> iterable_element : playerMap
					.entrySet()) {
				info = iterable_element.getValue();
				String temp;

				temp = listLine.replaceAll("%ID%", info.id);
				temp = temp.replaceAll("%IP%", info.ip);
				temp = temp.replaceAll("%MAC%", info.mac);
				temp = temp.replaceAll("%NAME%", info.name);
				temp = temp.replaceAll("%ADDRESS%", info.address);
				temp = temp.replaceAll("%STATE%", info.state);
				temp = temp.replaceAll("%SHAKE%", "http://" + info.ip
						+ ":8080/shake");

				listInfo.append(temp);
			}

			list = listHtml.replaceAll("%LIST%", listInfo.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return list;
	}

	public static String convertStreamToString(InputStream is) {
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is,
						"UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return writer.toString();
		} else {
			return "";
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}