package org.yao.curator.x.discovery.server.rest;

import org.yao.curator.x.discovery.server.Util;

/** Response for HTTP API. */
public class Response {
  private int code;
  private String msg;
  private Object data;

  public Response() {
    code = 0;
    msg = "success";
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  /** Returns the JSON representation for the response. */
  @Override
  public String toString() {
    return Util.toJson(this);
  }
}
