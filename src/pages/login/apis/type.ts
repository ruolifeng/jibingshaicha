export interface LoginRequestData {
  username: string
  password: string
}

export type LoginResponseData = ApiResponseData<string>
