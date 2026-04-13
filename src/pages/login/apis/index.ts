import type * as Auth from "./type"
import { request } from "@/http/axios"

/** 登录并返回 Token */
export function loginApi(data: Auth.LoginRequestData) {
  return request<Auth.LoginResponseData>({
    url: "user/login",
    method: "post",
    params: data
  })
}
